package br.upf.booki.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.upf.booki.MainActivity
import br.upf.booki.R
import br.upf.booki.data.AppDatabase
import br.upf.booki.model.ValidadorUsuario
import br.upf.booki.util.Senha
import br.upf.booki.util.Sessao
import br.upf.booki.util.Util
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/** Primeira tela: login local contra a tabela de usuários do Room. */
class LoginActivity : AppCompatActivity() {

    private lateinit var campoEmail: TextInputLayout
    private lateinit var campoSenha: TextInputLayout
    private lateinit var editEmail: TextInputEditText
    private lateinit var editSenha: TextInputEditText
    private lateinit var btnEntrar: MaterialButton
    private lateinit var sessao: Sessao

    /** Ao voltar do cadastro com sucesso, já deixa o e-mail preenchido. */
    private val abrirCadastro =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            if (resultado.resultCode == RESULT_OK) {
                val email = resultado.data?.getStringExtra(CadastroUsuarioActivity.EXTRA_EMAIL)
                editEmail.setText(email)
                editSenha.text = null
                editSenha.requestFocus()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessao = Sessao(this)

        // Já existe alguém logado: vai direto para a estante.
        if (sessao.usuarioId != null) {
            abrirPrincipal()
            return
        }

        setContentView(R.layout.activity_login)
        Util.aplicarMargensDoSistema(findViewById(R.id.raizLogin), findViewById(R.id.cabecalhoLogin))

        campoEmail = findViewById(R.id.campoEmailLogin)
        campoSenha = findViewById(R.id.campoSenhaLogin)
        editEmail = findViewById(R.id.editEmailLogin)
        editSenha = findViewById(R.id.editSenhaLogin)
        btnEntrar = findViewById(R.id.btnEntrar)

        btnEntrar.setOnClickListener { entrar() }
        editSenha.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_DONE) {
                entrar()
                true
            } else {
                false
            }
        }
        findViewById<View>(R.id.btnIrCadastro).setOnClickListener {
            abrirCadastro.launch(Intent(this, CadastroUsuarioActivity::class.java))
        }
    }

    private fun entrar() {
        val email = editEmail.text.toString().trim().lowercase()
        val senha = editSenha.text.toString()

        campoEmail.error = null
        campoSenha.error = null
        var valido = true
        if (email.isEmpty()) {
            campoEmail.error = getString(R.string.erro_informe_email)
            valido = false
        } else if (!ValidadorUsuario.emailValido(email)) {
            campoEmail.error = getString(R.string.erro_email_invalido)
            valido = false
        }
        if (senha.isEmpty()) {
            campoSenha.error = getString(R.string.erro_informe_senha)
            valido = false
        }
        if (!valido) return

        btnEntrar.isEnabled = false
        lifecycleScope.launch {
            val usuario = AppDatabase.getInstance(this@LoginActivity).userDao().buscarPorEmail(email)
            btnEntrar.isEnabled = true
            if (usuario == null || usuario.senhaHash != Senha.hash(senha, email)) {
                Util.dialogoAlerta(
                    this@LoginActivity,
                    getString(R.string.titulo_falha_login),
                    getString(R.string.erro_login_incorreto)
                )
            } else {
                sessao.entrar(usuario.id)
                abrirPrincipal()
            }
        }
    }

    private fun abrirPrincipal() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
