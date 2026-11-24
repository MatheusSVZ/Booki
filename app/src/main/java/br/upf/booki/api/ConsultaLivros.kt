package br.upf.booki.api

import br.upf.booki.BuildConfig
import br.upf.booki.model.Livro
import br.upf.booki.model.ValidadorLivro
import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Resultado de uma consulta à internet, sem deixar exceções chegarem às telas. */
sealed class Resultado<out T> {
    data class Sucesso<T>(val valor: T) : Resultado<T>()
    /** Sem internet ou o servidor não respondeu. */
    data object SemConexao : Resultado<Nothing>()
    /** A API recusou (limite de consultas, chave inválida, erro no servidor). */
    data class Indisponivel(val codigo: Int) : Resultado<Nothing>()
}

data class PrecoEncontrado(
    val valorFormatado: String,
    val tituloNaLoja: String,
    val linkCompra: String?
)

/** Dados para preencher o formulário a partir do ISBN. */
data class DadosLivro(
    val titulo: String,
    val autor: String?,
    val categoria: String?,
    val paginas: Int?,
    val capaUrl: String? = null
)

/** Item retornado pela busca geral de livros e valores (Google Books com fallback Open Library). */
data class LivroPesquisa(
    val titulo: String,
    val autor: String,
    val isbn: String?,
    val paginas: Int?,
    val precoTexto: String?,
    val linkLoja: String?,
    val categoria: String? = null,
    val origemFallback: Boolean = false,
    val capaUrl: String? = null
)

/** Concentra as chamadas às APIs REST usadas pelo app. */
object ConsultaLivros {

    /** Devolve null quando a chave estiver vazia, para o Retrofit omitir o parâmetro `key` da URL. */
    private fun chaveGoogleBooks(): String? = BuildConfig.GOOGLE_BOOKS_API_KEY.trim().ifBlank { null }

    private fun ehPortuguesGoogle(idioma: String?): Boolean =
        idioma?.trim()?.lowercase()?.startsWith("pt") == true

    private fun ehPortuguesOpenLibrary(idiomas: List<String>?): Boolean =
        idiomas.orEmpty().any { codigo ->
            val normalizado = codigo.trim().lowercase()
            normalizado == "por" || normalizado.startsWith("pt")
        }

    private suspend fun <T> chamar(bloco: suspend () -> T): Resultado<T> = try {
        Resultado.Sucesso(bloco())
    } catch (e: HttpException) {
        Resultado.Indisponivel(e.code())
    } catch (e: IOException) {
        Resultado.SemConexao
    } catch (e: JsonParseException) {
        // Resposta fora do formato esperado.
        Resultado.Indisponivel(-1)
    }

    /**
     * Procura o preço do livro no Google Books (pelo ISBN ou por título + autor),
     * priorizando resultados em português (Brasil) e fazendo fallback sem `langRestrict` se necessário.
     * Sucesso(null) significa que a loja não vende esse livro.
     */
    suspend fun consultarPreco(livro: Livro): Resultado<PrecoEncontrado?> = chamar {
        val consulta = livro.isbn?.takeIf { it.isNotBlank() }?.let { "isbn:$it" }
            ?: "intitle:${livro.titulo} inauthor:${livro.autor}"
        val chave = chaveGoogleBooks()

        val volumesPt = RetrofitClient.googleBooks
            .buscar(consulta = consulta, chave = chave, idioma = "pt", pais = "BR")
            .items
            .orEmpty()
        extrairPreco(volumesPt, livro.titulo) ?: run {
            val volumesGeral = RetrofitClient.googleBooks
                .buscar(consulta = consulta, chave = chave, idioma = null, pais = "BR")
                .items
                .orEmpty()
            extrairPreco(volumesGeral, livro.titulo)
        }
    }

    private fun extrairPreco(volumes: List<Volume>, tituloFallback: String): PrecoEncontrado? {
        val ordenados = volumes.sortedByDescending { ehPortuguesGoogle(it.volumeInfo?.language) }
        return ordenados.firstNotNullOfOrNull { volume ->
            val venda = volume.saleInfo ?: return@firstNotNullOfOrNull null
            val preco = venda.retailPrice ?: venda.listPrice
            val valor = preco?.amount
            if (venda.saleability != "FOR_SALE" || valor == null) return@firstNotNullOfOrNull null
            PrecoEncontrado(
                valorFormatado = formatarMoeda(valor, preco.currencyCode),
                tituloNaLoja = volume.volumeInfo?.title ?: tituloFallback,
                linkCompra = venda.buyLink ?: volume.volumeInfo?.infoLink
            )
        }
    }

    /**
     * Busca livros por título, autor ou ISBN na Google Books API priorizando português (Brasil).
     * Em caso de HTTP 429, outra falha do Google Books ou resposta vazia, usa a Open Library em português.
     */
    suspend fun pesquisarLivros(termo: String, limite: Int = 15): Resultado<List<LivroPesquisa>> {
        val texto = termo.trim()
        if (texto.isEmpty()) return Resultado.Sucesso(emptyList())

        val resultadoGoogle = pesquisarNoGoogleBooks(texto, limite)
        if (resultadoGoogle is Resultado.Sucesso && resultadoGoogle.valor.isNotEmpty()) {
            return resultadoGoogle
        }
        return pesquisarNaOpenLibrary(texto, limite)
    }

    private suspend fun pesquisarNoGoogleBooks(texto: String, limite: Int): Resultado<List<LivroPesquisa>> = chamar {
        val consulta = if (ValidadorLivro.isbnValido(texto)) {
            "isbn:${ValidadorLivro.normalizarIsbn(texto)}"
        } else {
            texto
        }
        val chave = chaveGoogleBooks()
        val volumesPt = RetrofitClient.googleBooks
            .buscar(consulta = consulta, chave = chave, idioma = "pt", pais = "BR", maximo = limite)
            .items
            .orEmpty()
        val volumes = volumesPt.ifEmpty {
            RetrofitClient.googleBooks
                .buscar(consulta = consulta, chave = chave, idioma = null, pais = "BR", maximo = limite)
                .items
                .orEmpty()
        }

        volumes
            .sortedByDescending { ehPortuguesGoogle(it.volumeInfo?.language) }
            .mapNotNull { volume ->
                val info = volume.volumeInfo ?: return@mapNotNull null
                val titulo = info.title?.trim().orEmpty()
                if (titulo.isEmpty()) return@mapNotNull null

                val autor = info.authors
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.joinToString(", ")
                    .orEmpty()

                val identificadores = info.industryIdentifiers.orEmpty()
                val isbnBruto = identificadores.firstOrNull { it.type == "ISBN_13" }?.identifier
                    ?: identificadores.firstOrNull { it.type == "ISBN_10" }?.identifier
                    ?: identificadores.firstOrNull()?.identifier
                val isbn = isbnBruto?.let { ValidadorLivro.normalizarIsbn(it) }?.ifBlank { null }

                val venda = volume.saleInfo
                val preco = venda?.retailPrice ?: venda?.listPrice
                val precoTexto = preco?.amount?.let { formatarMoeda(it, preco.currencyCode) }
                val linkLoja = venda?.buyLink?.ifBlank { null } ?: info.infoLink?.ifBlank { null }
                val capaUrl = (info.imageLinks?.thumbnail ?: info.imageLinks?.smallThumbnail)
                    ?.trim()
                    ?.replace("http://", "https://")
                    ?.ifBlank { null }

                LivroPesquisa(
                    titulo = titulo,
                    autor = autor,
                    isbn = isbn,
                    paginas = info.pageCount?.takeIf { it > 0 },
                    precoTexto = precoTexto,
                    linkLoja = linkLoja,
                    categoria = info.categories?.firstOrNull()?.trim()?.ifBlank { null },
                    origemFallback = false,
                    capaUrl = capaUrl
                )
            }
    }

    private suspend fun pesquisarNaOpenLibrary(texto: String, limite: Int): Resultado<List<LivroPesquisa>> = chamar {
        val docsPt = RetrofitClient.openLibrary
            .buscar(texto = "$texto language:por", idioma = "pt", limite = limite)
            .docs
            .orEmpty()
        val docs = docsPt.ifEmpty {
            RetrofitClient.openLibrary
                .buscar(texto = texto, idioma = "pt", limite = limite)
                .docs
                .orEmpty()
        }

        docs.mapNotNull { doc ->
            val edicoes = doc.editions?.docs.orEmpty()
            val edicaoPt = edicoes.firstOrNull {
                ehPortuguesOpenLibrary(it.language) && !it.title.isNullOrBlank()
            } ?: edicoes.firstOrNull { !it.title.isNullOrBlank() }

            val titulo = edicaoPt?.title?.trim()?.ifBlank { null }
                ?: doc.title?.trim().orEmpty()
            if (titulo.isEmpty()) return@mapNotNull null

            val autor = doc.autores
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.joinToString(", ")
                .orEmpty()

            val isbnsBrutos = edicaoPt?.isbn.orEmpty() + doc.isbn.orEmpty()
            val isbnsNormalizados = isbnsBrutos
                .map { ValidadorLivro.normalizarIsbn(it) }
                .filter { it.isNotEmpty() }
            val isbn = isbnsNormalizados.firstOrNull { it.length == 13 && ValidadorLivro.isbnValido(it) }
                ?: isbnsNormalizados.firstOrNull { it.length == 10 && ValidadorLivro.isbnValido(it) }
                ?: isbnsNormalizados.firstOrNull()

            val coverId = edicaoPt?.capaId?.takeIf { it > 0 } ?: doc.capaId?.takeIf { it > 0 }
            val capaUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }

            LivroPesquisa(
                titulo = titulo,
                autor = autor,
                isbn = isbn,
                paginas = doc.paginas?.takeIf { it > 0 },
                precoTexto = null,
                linkLoja = null,
                categoria = doc.assuntos?.firstOrNull()?.trim()?.ifBlank { null },
                origemFallback = true,
                capaUrl = capaUrl
            )
        }
    }

    /** Busca título, autor, categoria e páginas na Open Library. Sucesso(null) = ISBN não encontrado. */
    suspend fun buscarPorIsbn(isbn: String): Resultado<DadosLivro?> = chamar {
        val doc = RetrofitClient.openLibrary.buscar(isbn = isbn, idioma = "pt", limite = 1).docs?.firstOrNull()
        val edicaoPt = doc?.editions?.docs.orEmpty().firstOrNull { !it.title.isNullOrBlank() }
        val titulo = edicaoPt?.title?.trim()?.ifBlank { null } ?: doc?.title?.trim()?.ifBlank { null }
        val coverId = edicaoPt?.capaId?.takeIf { it > 0 } ?: doc?.capaId?.takeIf { it > 0 }
        val capaUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
        if (doc == null || titulo == null) {
            null
        } else {
            DadosLivro(
                titulo = titulo,
                autor = doc.autores?.firstOrNull(),
                categoria = doc.assuntos?.firstOrNull(),
                paginas = doc.paginas,
                capaUrl = capaUrl
            )
        }
    }

    fun formatarMoeda(valor: Double, codigoMoeda: String?): String {
        val formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
        if (codigoMoeda != null) {
            runCatching { formato.currency = Currency.getInstance(codigoMoeda) }
        }
        return formato.format(valor)
    }
}
