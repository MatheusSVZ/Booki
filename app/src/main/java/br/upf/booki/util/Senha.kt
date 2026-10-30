package br.upf.booki.util

import java.security.MessageDigest

/** Gera o hash SHA-256 da senha, usando o e-mail como "sal". */
object Senha {
    fun hash(senha: String, email: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("${email.lowercase()}:$senha".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
