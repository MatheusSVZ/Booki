package br.upf.booki

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.upf.booki.data.AppDatabase
import br.upf.booki.ui.LoginActivity
import br.upf.booki.ui.fragments.DesejosFragment
import br.upf.booki.ui.fragments.EstanteFragment
import br.upf.booki.ui.fragments.PerfilFragment
import br.upf.booki.ui.fragments.RecomendacoesFragment
import br.upf.booki.util.Sessao
import br.upf.booki.util.Util
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

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
        findViewById<TextView>(R.id.tvHeaderTitle).text = Util.formatarTituloBooki(this)
        Util.aplicarMargensDoSistema(
            findViewById(R.id.raizMain),
            findViewById(R.id.headerBooki),
            ajustarInferior = false
        )

        val navegacao = findViewById<BottomNavigationView>(R.id.navegacaoInferior)
        navegacao.setOnItemSelectedListener { item ->
            mostrar(
                when (item.itemId) {
                    R.id.navDesejos -> DesejosFragment()
                    R.id.navRecomendacoes -> RecomendacoesFragment()
                    R.id.navPerfil -> PerfilFragment()
                    else -> EstanteFragment()
                }
            )
            true
        }
        if (savedInstanceState == null) {
            navegacao.selectedItemId = R.id.navEstante
        }

        lifecycleScope.launch {
            val usuario = AppDatabase.getInstance(this@MainActivity).userDao().buscarPorId(usuarioId)
            if (usuario == null) {
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
