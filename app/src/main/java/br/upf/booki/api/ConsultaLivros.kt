package br.upf.booki.api

import br.upf.booki.BuildConfig
import br.upf.booki.model.Livro
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
    val paginas: Int?
)

/** Concentra as chamadas às APIs REST usadas pelo app. */
object ConsultaLivros {

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
     * Procura o preço do livro no Google Books (pelo ISBN ou por título + autor).
     * Sucesso(null) significa que a loja não vende esse livro.
     */
    suspend fun consultarPreco(livro: Livro): Resultado<PrecoEncontrado?> = chamar {
        val consulta = livro.isbn?.let { "isbn:$it" }
            ?: "intitle:${livro.titulo} inauthor:${livro.autor}"
        val chave = BuildConfig.GOOGLE_BOOKS_API_KEY.ifBlank { null }
        val volumes = RetrofitClient.googleBooks.buscar(consulta, chave).items.orEmpty()

        volumes.firstNotNullOfOrNull { volume ->
            val venda = volume.saleInfo ?: return@firstNotNullOfOrNull null
            val preco = venda.retailPrice ?: venda.listPrice
            val valor = preco?.amount
            if (venda.saleability != "FOR_SALE" || valor == null) return@firstNotNullOfOrNull null
            PrecoEncontrado(
                valorFormatado = formatarMoeda(valor, preco.currencyCode),
                tituloNaLoja = volume.volumeInfo?.title ?: livro.titulo,
                linkCompra = venda.buyLink ?: volume.volumeInfo?.infoLink
            )
        }
    }

    /** Busca título, autor, categoria e páginas na Open Library. Sucesso(null) = ISBN não encontrado. */
    suspend fun buscarPorIsbn(isbn: String): Resultado<DadosLivro?> = chamar {
        val doc = RetrofitClient.openLibrary.buscar(isbn = isbn, limite = 1).docs?.firstOrNull()
        val titulo = doc?.title
        if (doc == null || titulo == null) {
            null
        } else {
            DadosLivro(
                titulo = titulo,
                autor = doc.autores?.firstOrNull(),
                categoria = doc.assuntos?.firstOrNull(),
                paginas = doc.paginas
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
