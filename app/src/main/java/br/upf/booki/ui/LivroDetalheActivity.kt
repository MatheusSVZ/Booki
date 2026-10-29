package br.upf.booki.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import br.upf.booki.R
import br.upf.booki.data.AppDatabase
import br.upf.booki.model.Livro
import br.upf.booki.model.StatusLeitura
import br.upf.booki.util.Sessao
import br.upf.booki.util.Util
import kotlinx.coroutines.launch

/** Mostra um livro e permite compartilhar, editar e excluir. Recebe putExtra("id", ...). */
class LivroDetalheActivity : AppCompatActivity() {

    private var livroId = 0
    private var usuarioId = 0

    /** Livro mostrado na tela; null enquanto carrega. */
    private var livro: Livro? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val idUsuario = Sessao(this).usuarioId
        livroId = intent.getIntExtra(LivroFormActivity.EXTRA_ID, 0)
        if (idUsuario == null || livroId == 0) {
            finish()
            return
        }
        usuarioId = idUsuario

        setContentView(R.layout.activity_livro_detalhe)
        val toolbar = findViewById<Toolbar>(R.id.toolbarLivroDetalhe)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        Util.aplicarMargensDoSistema(findViewById(R.id.raizLivroDetalhe), toolbar)
    }

    /** Recarrega sempre que a tela volta ao primeiro plano (ex.: depois de editar). */
    override fun onResume() {
        super.onResume()
        carregar()
    }

    private fun carregar() {
        lifecycleScope.launch {
            val encontrado = AppDatabase.getInstance(this@LivroDetalheActivity)
                .livroDao().buscarPorId(livroId, usuarioId)
            if (encontrado == null) {
                Util.mensagem(this@LivroDetalheActivity, getString(R.string.livro_nao_encontrado))
                finish()
                return@launch
            }
            livro = encontrado
            mostrar(encontrado)
        }
    }

    private fun mostrar(livro: Livro) {
        findViewById<TextView>(R.id.txtTituloDetalhe).text = livro.titulo
        findViewById<TextView>(R.id.txtAutorDetalhe).text = livro.autor

        val status = findViewById<TextView>(R.id.txtStatusDetalhe)
        status.setText(livro.status.rotulo)
        status.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, livro.status.cor))

        val rating = findViewById<RatingBar>(R.id.ratingDetalhe)
        rating.visibility = if (livro.nota != null) View.VISIBLE else View.GONE
        rating.rating = livro.nota ?: 0f

        val linhas = mutableListOf<String>()
        livro.isbn?.let { linhas += getString(R.string.info_isbn, it) }
        livro.categoria?.let { linhas += getString(R.string.info_categoria, it) }
        livro.paginas?.let { linhas += getString(R.string.info_paginas, it) }
        livro.dataInicio?.let { linhas += getString(R.string.info_inicio, Util.dataParaTexto(it)) }
        livro.dataConclusao?.let { linhas += getString(R.string.info_conclusao, Util.dataParaTexto(it)) }
        livro.nota?.let { linhas += getString(R.string.info_nota, formatarNota(it)) }
        if (linhas.isEmpty()) linhas += getString(R.string.info_vazia)
        findViewById<TextView>(R.id.txtInfoDetalhe).text = linhas.joinToString("\n")

        val temComentario = !livro.comentario.isNullOrBlank()
        findViewById<View>(R.id.txtComentarioTitulo).visibility = if (temComentario) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.txtComentarioDetalhe).text = livro.comentario
    }

    private fun formatarNota(nota: Float) = if (nota % 1f == 0f) nota.toInt().toString()
    else nota.toString().replace('.', ',')

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_livro_detalhe, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val atual = livro ?: return super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.acaoCompartilhar -> compartilhar(atual)
            R.id.acaoEditar -> startActivity(
                Intent(this, LivroFormActivity::class.java).putExtra(LivroFormActivity.EXTRA_ID, atual.id)
            )
            R.id.acaoExcluir -> confirmarExclusao(atual)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /** Intent implícita: o Android oferece os apps que sabem enviar texto. */
    private fun compartilhar(livro: Livro) {
        val texto = when (livro.status) {
            StatusLeitura.LIDO -> {
                val nota = livro.nota?.let { getString(R.string.compartilhar_nota, formatarNota(it)) }.orEmpty()
                getString(R.string.compartilhar_lido, livro.titulo, livro.autor, nota)
            }
            StatusLeitura.LENDO -> getString(R.string.compartilhar_lendo, livro.titulo, livro.autor)
            StatusLeitura.QUERO_LER -> getString(R.string.compartilhar_quero_ler, livro.titulo, livro.autor)
        }
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, livro.titulo)
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(envio, getString(R.string.compartilhar_com)))
    }

    private fun confirmarExclusao(livro: Livro) {
        Util.dialogoConfirma(
            this,
            getString(R.string.excluir_livro),
            getString(R.string.confirma_excluir, livro.titulo)
        ) {
            lifecycleScope.launch {
                AppDatabase.getInstance(this@LivroDetalheActivity).livroDao().excluir(livro)
                Util.mensagem(this@LivroDetalheActivity, getString(R.string.livro_excluido))
                finish()
            }
        }
    }
}
