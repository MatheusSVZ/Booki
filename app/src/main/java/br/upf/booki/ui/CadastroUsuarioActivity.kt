package br.upf.booki.ui

import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import br.upf.booki.R
import br.upf.booki.data.AppDatabase
import br.upf.booki.model.User
import br.upf.booki.model.ValidadorUsuario
import br.upf.booki.model.ValidadorUsuario.Campo
import br.upf.booki.util.Senha
import br.upf.booki.util.Util
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/** Cadastro de um novo usuário local. Devolve o e-mail cadastrado para a tela de login. */
class CadastroUsuarioActivity : AppCompatActivity() {

    private lateinit var campos: Map<Campo, TextInputLayout>
    private lateinit var btnCadastrar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_usuario)

        val toolbar = findViewById<Toolbar>(R.id.toolbarCadastro)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        Util.aplicarMargensDoSistema(findViewById(R.id.raizCadastro), toolbar)

        campos = mapOf(
            Campo.NOME to findViewById(R.id.campoNome),
            Campo.EMAIL to findViewById(R.id.campoEmail),
            Campo.SENHA to findViewById(R.id.campoSenha),
            Campo.CONFIRMACAO to findViewById(R.id.campoConfirmacao)
        )
        btnCadastrar = findViewById(R.id.btnCadastrar)
        btnCadastrar.setOnClickListener { cadastrar() }
    }

    private fun texto(campo: Campo) = campos.getValue(campo).editText?.text?.toString().orEmpty()

    private fun cadastrar() {
        val nome = texto(Campo.NOME).trim()
        val email = texto(Campo.EMAIL).trim().lowercase()
        val senha = texto(Campo.SENHA)
        val confirmacao = texto(Campo.CONFIRMACAO)

        val erros = ValidadorUsuario.validarCadastro(nome, email, senha, confirmacao)
        campos.forEach { (campo, layout) -> layout.error = erros[campo] }
        if (erros.isNotEmpty()) return

        btnCadastrar.isEnabled = false
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@CadastroUsuarioActivity).userDao()
            // Regra de negócio: um e-mail só pode ter uma conta.
            val emailEmUso = dao.buscarPorEmail(email) != null
            val salvou = !emailEmUso && try {
                dao.inserir(User(nome = nome, email = email, senhaHash = Senha.hash(senha, email)))
                true
            } catch (e: SQLiteConstraintException) {
                false
            }
            btnCadastrar.isEnabled = true

            if (!salvou) {
                campos.getValue(Campo.EMAIL).error = getString(R.string.erro_email_em_uso)
                return@launch
            }
            Util.mensagem(this@CadastroUsuarioActivity, getString(R.string.conta_criada))
            setResult(RESULT_OK, Intent().putExtra(EXTRA_EMAIL, email))
            finish()
        }
    }

    companion object {
        const val EXTRA_EMAIL = "email"
    }
}
