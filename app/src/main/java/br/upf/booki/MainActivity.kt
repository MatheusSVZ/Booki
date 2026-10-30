package br.upf.booki

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.upf.booki.data.AppDatabase
import br.upf.booki.ui.LoginActivity
import br.upf.booki.ui.fragments.EstanteFragment
import br.upf.booki.ui.fragments.PerfilFragment
import br.upf.booki.ui.fragments.RecomendacoesFragment
import br.upf.booki.util.Sessao
import br.upf.booki.util.Util
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

/** Tela principal: a barra inferior troca entre os fragments Estante, Recomendações e Perfil. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessao = Sessao(this)
        val usuarioId = sessao.usuarioId
        if (usuarioId == null) {
            irParaLogin()
            return
        }

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbarMain)
        setSupportActionBar(toolbar)
        // A BottomNavigationView já se afasta da barra de navegação sozinha.
        Util.aplicarMargensDoSistema(findViewById(R.id.raizMain), toolbar, ajustarInferior = false)

        val navegacao = findViewById<BottomNavigationView>(R.id.navegacaoInferior)
        navegacao.setOnItemSelectedListener { item ->
            mostrar(
                when (item.itemId) {
                    R.id.navRecomendacoes -> RecomendacoesFragment()
                    R.id.navPerfil -> PerfilFragment()
                    else -> EstanteFragment()
                }
            )
            true
        }
        // Só cria o primeiro fragment na primeira abertura; ao girar a tela o Android restaura o atual.
        if (savedInstanceState == null) {
            navegacao.selectedItemId = R.id.navEstante
        }

        lifecycleScope.launch {
            val usuario = AppDatabase.getInstance(this@MainActivity).userDao().buscarPorId(usuarioId)
            if (usuario == null) {
                // Sessão antiga de um usuário que não existe mais.
                sessao.sair()
                irParaLogin()
            }
        }
    }

    private fun mostrar(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerFragments, fragment)
            .commit()
    }

    private fun irParaLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
