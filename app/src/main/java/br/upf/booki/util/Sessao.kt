package br.upf.booki.util

import android.content.Context

/** Guarda quem está logado (SharedPreferences), para não pedir login toda vez. */
class Sessao(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("booki_sessao", Context.MODE_PRIVATE)

    /** Id do usuário logado ou null se ninguém estiver logado. */
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
