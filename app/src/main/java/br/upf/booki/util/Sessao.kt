package br.upf.booki.util

import android.content.Context

class Sessao(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("booki_sessao", Context.MODE_PRIVATE)

    val usuarioId: Int?
        get() = prefs.getInt(CHAVE_USUARIO, SEM_USUARIO).takeIf { it != SEM_USUARIO }

    fun entrar(usuarioId: Int) {
        prefs.edit().putInt(CHAVE_USUARIO, usuarioId).apply()
    }

    fun sair() {
        prefs.edit().remove(CHAVE_USUARIO).apply()
    }

    private companion object {
        const val CHAVE_USUARIO = "usuario_id"
        const val SEM_USUARIO = -1
    }
}
