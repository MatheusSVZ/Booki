package br.upf.booki

import br.upf.booki.model.Livro
import br.upf.booki.model.StatusLeitura
import br.upf.booki.model.ValidadorLivro
import br.upf.booki.model.ValidadorLivro.Campo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidadorLivroTest {

    private val hoje = LocalDate.of(2026, 10, 29)

    private fun livro(
        status: StatusLeitura = StatusLeitura.LIDO,
        titulo: String = "Dom Casmurro",
        isbn: String? = null,
        nota: Float? = null,
        inicio: LocalDate? = null,
        fim: LocalDate? = null,
        paginas: Int? = null
    ) = Livro(
        usuarioId = 1, titulo = titulo, autor = "Machado de Assis", isbn = isbn,
        status = status, nota = nota, dataInicio = inicio, dataConclusao = fim, paginas = paginas
    )

    @Test
    fun livroCompletoEValido() {
        val erros = ValidadorLivro.validar(
            livro(isbn = "978-85-359-0277-8", nota = 4.5f, inicio = hoje.minusDays(10), fim = hoje, paginas = 256),
            hoje
        )
        assertTrue(erros.toString(), erros.isEmpty())
    }

    @Test
    fun tituloEObrigatorio() {
        assertEquals(setOf(Campo.TITULO), ValidadorLivro.validar(livro(titulo = "  "), hoje).keys)
    }

    @Test
    fun conferenciaDoIsbn() {
        assertTrue(ValidadorLivro.isbnValido("9788535902778"))
        assertTrue(ValidadorLivro.isbnValido("85-359-0277-5"))
        assertTrue(ValidadorLivro.isbnValido("080442957X"))
        assertFalse(ValidadorLivro.isbnValido("9788535902771"))
        assertFalse(ValidadorLivro.isbnValido("12345"))
        assertFalse(ValidadorLivro.isbnValido("97885359O2778"))
    }

    @Test
    fun notaSoParaLivroLido() {
        val erros = ValidadorLivro.validar(livro(status = StatusLeitura.LENDO, nota = 3f), hoje)
        assertEquals(setOf(Campo.NOTA), erros.keys)
    }

    @Test
    fun datasNaoPodemSerFuturas() {
        val erros = ValidadorLivro.validar(livro(inicio = hoje.plusDays(1), fim = hoje.plusDays(2)), hoje)
        assertEquals(setOf(Campo.DATA_INICIO, Campo.DATA_CONCLUSAO), erros.keys)
    }

    @Test
    fun conclusaoDepoisDoInicio() {
        val erros = ValidadorLivro.validar(livro(inicio = hoje, fim = hoje.minusDays(3)), hoje)
        assertEquals(setOf(Campo.DATA_CONCLUSAO), erros.keys)
    }

    @Test
    fun queroLerNaoTemDatas() {
        val erros = ValidadorLivro.validar(livro(status = StatusLeitura.QUERO_LER, inicio = hoje, fim = hoje), hoje)
        assertEquals(setOf(Campo.DATA_INICIO, Campo.DATA_CONCLUSAO), erros.keys)
    }

    @Test
    fun paginasPositivas() {
        assertEquals(setOf(Campo.PAGINAS), ValidadorLivro.validar(livro(paginas = 0), hoje).keys)
    }
}
