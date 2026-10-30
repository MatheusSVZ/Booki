package br.upf.booki

import br.upf.booki.api.ConsultaLivros
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsultaLivrosTest {

    @Test
    fun formataPrecoEmReais() {
        val texto = ConsultaLivros.formatarMoeda(39.9, "BRL")
        assertTrue(texto, texto.contains("R$") && texto.contains("39,90"))
    }

    @Test
    fun moedaDesconhecidaNaoQuebra() {
        val texto = ConsultaLivros.formatarMoeda(10.0, "???")
        assertTrue(texto, texto.contains("10,00"))
    }
}
