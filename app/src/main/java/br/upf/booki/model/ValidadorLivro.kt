package br.upf.booki.model

import java.time.LocalDate

/**
 * Regras de negócio do cadastro de livros.
 * Fica fora da Activity para poder ser testado com testes unitários.
 */
object ValidadorLivro {

    enum class Campo { TITULO, AUTOR, ISBN, PAGINAS, NOTA, DATA_INICIO, DATA_CONCLUSAO }

    const val TITULO_MAXIMO = 150
    const val PAGINAS_MAXIMO = 20000
    const val NOTA_MINIMA = 0.5f
    const val NOTA_MAXIMA = 5f

    /** Tira hífens e espaços e deixa o X do ISBN-10 maiúsculo. */
    fun normalizarIsbn(isbn: String): String =
        isbn.filter { it.isDigit() || it == 'x' || it == 'X' }.uppercase()

    /** Confere o dígito verificador do ISBN-10 ou ISBN-13. */
    fun isbnValido(isbn: String): Boolean {
        val limpo = normalizarIsbn(isbn)
        // Não pode ter outros caracteres além de dígitos, hífen, espaço e X.
        if (isbn.any { !(it.isDigit() || it == '-' || it == ' ' || it == 'x' || it == 'X') }) return false
        return when (limpo.length) {
            10 -> isbn10Valido(limpo)
            13 -> isbn13Valido(limpo)
            else -> false
        }
    }

    private fun isbn10Valido(isbn: String): Boolean {
        if (!isbn.substring(0, 9).all { it.isDigit() }) return false
        val ultimo = isbn[9]
        if (!(ultimo.isDigit() || ultimo == 'X')) return false
        var soma = 0
        for (i in 0 until 10) {
            val valor = if (isbn[i] == 'X') 10 else isbn[i].digitToInt()
            soma += valor * (10 - i)
        }
        return soma % 11 == 0
    }

    private fun isbn13Valido(isbn: String): Boolean {
        if (!isbn.all { it.isDigit() }) return false
        val soma = isbn.mapIndexed { i, c -> c.digitToInt() * if (i % 2 == 0) 1 else 3 }.sum()
        return soma % 10 == 0
    }

    /**
     * Valida um livro e devolve os erros por campo (mapa vazio = livro válido).
     * [hoje] é parâmetro para facilitar os testes.
     */
    fun validar(livro: Livro, hoje: LocalDate = LocalDate.now()): Map<Campo, String> {
        val erros = mutableMapOf<Campo, String>()

        if (livro.titulo.isBlank()) erros[Campo.TITULO] = "O título é obrigatório"
        else if (livro.titulo.length > TITULO_MAXIMO) erros[Campo.TITULO] = "Máximo de $TITULO_MAXIMO caracteres"

        if (livro.autor.isBlank()) erros[Campo.AUTOR] = "Informe o autor"

        val isbn = livro.isbn
        if (!isbn.isNullOrBlank() && !isbnValido(isbn)) {
            erros[Campo.ISBN] = "ISBN inválido (use 10 ou 13 dígitos)"
        }

        val paginas = livro.paginas
        if (paginas != null && paginas !in 1..PAGINAS_MAXIMO) {
            erros[Campo.PAGINAS] = "Número de páginas deve ficar entre 1 e $PAGINAS_MAXIMO"
        }

        // Nota só faz sentido para livro lido.
        val nota = livro.nota
        if (nota != null) {
            if (livro.status != StatusLeitura.LIDO) erros[Campo.NOTA] = "Só é possível dar nota a um livro lido"
            else if (nota < NOTA_MINIMA || nota > NOTA_MAXIMA) erros[Campo.NOTA] = "A nota vai de 0,5 a 5 estrelas"
        }

        val inicio = livro.dataInicio
        val fim = livro.dataConclusao
        if (inicio != null) {
            if (inicio.isAfter(hoje)) erros[Campo.DATA_INICIO] = "A data de início não pode estar no futuro"
            else if (livro.status == StatusLeitura.QUERO_LER) {
                erros[Campo.DATA_INICIO] = "Um livro que você ainda quer ler não tem data de início"
            }
        }
        if (fim != null) {
            when {
                livro.status != StatusLeitura.LIDO ->
                    erros[Campo.DATA_CONCLUSAO] = "Só livros lidos têm data de conclusão"
                fim.isAfter(hoje) ->
                    erros[Campo.DATA_CONCLUSAO] = "A data de conclusão não pode estar no futuro"
                inicio != null && fim.isBefore(inicio) ->
                    erros[Campo.DATA_CONCLUSAO] = "A conclusão não pode ser antes do início"
            }
        }
        return erros
    }
}
