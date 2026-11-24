package br.upf.booki.model

/** Regras do cadastro de usuário. Cada erro é ligado ao campo correspondente. */
object ValidadorUsuario {

    enum class Campo { NOME, EMAIL, SENHA, CONFIRMACAO }

    const val SENHA_MINIMA = 6
    private val REGEX_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailValido(email: String) = REGEX_EMAIL.matches(email.trim())

    fun validarCadastro(
        nome: String,
        email: String,
        senha: String,
        confirmacao: String
    ): Map<Campo, String> {
        val erros = mutableMapOf<Campo, String>()
        if (nome.isBlank()) erros[Campo.NOME] = "Informe seu nome"
        else if (nome.trim().length < 2) erros[Campo.NOME] = "Nome muito curto"

        if (email.isBlank()) erros[Campo.EMAIL] = "Informe o e-mail"
        else if (!emailValido(email)) erros[Campo.EMAIL] = "E-mail inválido"

        if (senha.length < SENHA_MINIMA) erros[Campo.SENHA] = "A senha precisa ter ao menos $SENHA_MINIMA caracteres"
        if (confirmacao != senha) erros[Campo.CONFIRMACAO] = "As senhas não conferem"
        return erros
    }
}
