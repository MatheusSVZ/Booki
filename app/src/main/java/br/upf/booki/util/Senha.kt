package br.upf.booki.util

import java.security.MessageDigest

object Senha {
    fun hash(senha: String, email: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("${email.lowercase()}:$senha".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
