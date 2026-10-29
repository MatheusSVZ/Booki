package br.upf.booki

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import br.upf.booki.data.AppDatabase
import br.upf.booki.ui.LoginActivity
import br.upf.booki.util.Sessao
import br.upf.booki.util.Util
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var sessao: Sessao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessao = Sessao(this)
        val usuarioId = sessao.usuarioId
        if (usuarioId == null) {
            irParaLogin()
            return
        }

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbarMain)
        setSupportActionBar(toolbar)
        Util.aplicarMargensDoSistema(findViewById(R.id.raizMain), toolbar)

        lifecycleScope.launch {
            val usuario = AppDatabase.getInstance(this@MainActivity).userDao().buscarPorId(usuarioId)
            if (usuario == null) {
                // Sessão antiga de um usuário que não existe mais.
                sessao.sair()
                irParaLogin()
            } else {
                findViewById<TextView>(R.id.txtBoasVindas).text =
                    getString(R.string.ola_usuario, usuario.nome)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.acaoSair) {
            Util.dialogoConfirma(this, getString(R.string.sair), getString(R.string.confirma_sair)) {
                sessao.sair()
                irParaLogin()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun irParaLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
