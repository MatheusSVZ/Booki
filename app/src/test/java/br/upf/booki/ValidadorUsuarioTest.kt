package br.upf.booki

import br.upf.booki.model.ValidadorUsuario
import br.upf.booki.model.ValidadorUsuario.Campo
import br.upf.booki.util.Senha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorUsuarioTest {

    @Test
    fun cadastroValidoNaoTemErros() {
        val erros = ValidadorUsuario.validarCadastro("Millena", "millena@upf.br", "123456", "123456")
        assertTrue(erros.isEmpty())
    }

    @Test
    fun apontaCadaCampoInvalido() {
        val erros = ValidadorUsuario.validarCadastro("", "sem-arroba", "123", "321")
        assertEquals(setOf(Campo.NOME, Campo.EMAIL, Campo.SENHA, Campo.CONFIRMACAO), erros.keys)
    }

    @Test
    fun validaFormatoDeEmail() {
        assertTrue(ValidadorUsuario.emailValido("matheus@upf.br"))
        assertFalse(ValidadorUsuario.emailValido("matheus@upf"))
        assertFalse(ValidadorUsuario.emailValido("matheus upf.br"))
    }

    @Test
    fun hashDaSenhaNaoGuardaTextoPuro() {
        val hash = Senha.hash("segredo1", "a@b.com")
        assertNotEquals("segredo1", hash)
        assertEquals(64, hash.length)
        assertEquals(hash, Senha.hash("segredo1", "A@B.com"))
    }
}
