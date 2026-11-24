package br.upf.booki.model

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import br.upf.booki.R

/** Situação de leitura de um livro na estante. */
enum class StatusLeitura(@param:StringRes val rotulo: Int, @param:ColorRes val cor: Int) {
    LIDO(R.string.status_lido, R.color.status_lido),
    LENDO(R.string.status_lendo, R.color.status_lendo),
    QUERO_LER(R.string.status_quero_ler, R.color.status_quero_ler)
}
