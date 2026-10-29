package br.upf.booki.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import br.upf.booki.R
import br.upf.booki.api.ConsultaLivros
import br.upf.booki.api.Resultado
import br.upf.booki.data.AppDatabase
import br.upf.booki.model.Livro
import br.upf.booki.model.StatusLeitura
import br.upf.booki.model.ValidadorLivro
import br.upf.booki.model.ValidadorLivro.Campo
import br.upf.booki.util.Conexao
import br.upf.booki.util.Sessao
import br.upf.booki.util.Util
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Formulário de livro: cria um livro novo ou edita um existente
 * (quando recebe putExtra("id", ...)).
 */
class LivroFormActivity : AppCompatActivity() {

    private lateinit var campoTitulo: TextInputLayout
    private lateinit var campoAutor: TextInputLayout
    private lateinit var campoIsbn: TextInputLayout
    private lateinit var campoCategoria: TextInputLayout
    private lateinit var campoPaginas: TextInputLayout
    private lateinit var campoDataInicio: TextInputLayout
    private lateinit var campoDataConclusao: TextInputLayout
    private lateinit var campoComentario: TextInputLayout
    private lateinit var grupoStatus: RadioGroup
    private lateinit var painelNota: View
    private lateinit var ratingNota: RatingBar
    private lateinit var txtErroNota: TextView
    private lateinit var btnSalvar: MaterialButton

    /** 0 significa livro novo. */
    private var livroId = 0
    private var usuarioId = 0
    private var dataInicio: LocalDate? = null
    private var dataConclusao: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val idUsuario = Sessao(this).usuarioId
        if (idUsuario == null) {
            finish()
            return
        }
        usuarioId = idUsuario
        livroId = intent.getIntExtra(EXTRA_ID, 0)

        setContentView(R.layout.activity_livro_form)
        val toolbar = findViewById<Toolbar>(R.id.toolbarLivroForm)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        Util.aplicarMargensDoSistema(findViewById(R.id.raizLivroForm), toolbar)
        if (livroId != 0) title = getString(R.string.titulo_editar_livro)

        campoTitulo = findViewById(R.id.campoTitulo)
        campoAutor = findViewById(R.id.campoAutor)
        campoIsbn = findViewById(R.id.campoIsbn)
        campoCategoria = findViewById(R.id.campoCategoria)
        campoPaginas = findViewById(R.id.campoPaginas)
        campoDataInicio = findViewById(R.id.campoDataInicio)
        campoDataConclusao = findViewById(R.id.campoDataConclusao)
        campoComentario = findViewById(R.id.campoComentario)
        grupoStatus = findViewById(R.id.grupoStatus)
        painelNota = findViewById(R.id.painelNota)
        ratingNota = findViewById(R.id.ratingNota)
        txtErroNota = findViewById(R.id.txtErroNota)
        btnSalvar = findViewById(R.id.btnSalvarLivro)

        configurarCampoData(campoDataInicio, { dataInicio }) { dataInicio = it }
        configurarCampoData(campoDataConclusao, { dataConclusao }) { dataConclusao = it }
        grupoStatus.setOnCheckedChangeListener { _, _ -> atualizarCamposPorStatus() }
        btnSalvar.setOnClickListener { salvar() }
        campoIsbn.setEndIconOnClickListener { preencherPeloIsbn() }

        if (savedInstanceState != null) {
            // Ao girar a tela os textos voltam sozinhos; as datas guardamos à parte.
            dataInicio = lerData(savedInstanceState, ESTADO_INICIO)
            dataConclusao = lerData(savedInstanceState, ESTADO_CONCLUSAO)
        } else if (livroId != 0) {
            carregarLivro()
        } else {
            preencherSugestao()
        }
        atualizarCamposPorStatus()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        dataInicio?.let { outState.putLong(ESTADO_INICIO, it.toEpochDay()) }
        dataConclusao?.let { outState.putLong(ESTADO_CONCLUSAO, it.toEpochDay()) }
    }

    private fun lerData(estado: Bundle, chave: String): LocalDate? =
        if (estado.containsKey(chave)) LocalDate.ofEpochDay(estado.getLong(chave)) else null

    private fun carregarLivro() {
        lifecycleScope.launch {
            val livro = AppDatabase.getInstance(this@LivroFormActivity).livroDao().buscarPorId(livroId, usuarioId)
            if (livro == null) {
                Util.mensagem(this@LivroFormActivity, getString(R.string.livro_nao_encontrado))
                finish()
                return@launch
            }
            preencher(livro)
        }
    }

    /** Dados vindos de outra tela (ex.: uma recomendação) para facilitar o cadastro. */
    private fun preencherSugestao() {
        campoTitulo.editText?.setText(intent.getStringExtra(EXTRA_TITULO))
        campoAutor.editText?.setText(intent.getStringExtra(EXTRA_AUTOR))
        campoIsbn.editText?.setText(intent.getStringExtra(EXTRA_ISBN))
        campoCategoria.editText?.setText(intent.getStringExtra(EXTRA_CATEGORIA))
        val paginas = intent.getIntExtra(EXTRA_PAGINAS, 0)
        if (paginas > 0) campoPaginas.editText?.setText(paginas.toString())
    }

    private fun preencher(livro: Livro) {
        campoTitulo.editText?.setText(livro.titulo)
        campoAutor.editText?.setText(livro.autor)
        campoIsbn.editText?.setText(livro.isbn)
        campoCategoria.editText?.setText(livro.categoria)
        campoPaginas.editText?.setText(livro.paginas?.toString())
        campoComentario.editText?.setText(livro.comentario)
        grupoStatus.check(
            when (livro.status) {
                StatusLeitura.LIDO -> R.id.radioLido
                StatusLeitura.LENDO -> R.id.radioLendo
                StatusLeitura.QUERO_LER -> R.id.radioQueroLer
            }
        )
        ratingNota.rating = livro.nota ?: 0f
        dataInicio = livro.dataInicio
        dataConclusao = livro.dataConclusao
        campoDataInicio.editText?.setText(Util.dataParaTexto(dataInicio))
        campoDataConclusao.editText?.setText(Util.dataParaTexto(dataConclusao))
        atualizarCamposPorStatus()
    }

    private fun statusSelecionado(): StatusLeitura = when (grupoStatus.checkedRadioButtonId) {
        R.id.radioLido -> StatusLeitura.LIDO
        R.id.radioLendo -> StatusLeitura.LENDO
        else -> StatusLeitura.QUERO_LER
    }

    /** Regra de negócio na tela: nota e conclusão só para "Lido"; início só para "Lendo" ou "Lido". */
    private fun atualizarCamposPorStatus() {
        val status = statusSelecionado()
        painelNota.visibility = if (status == StatusLeitura.LIDO) View.VISIBLE else View.GONE
        campoDataConclusao.visibility = if (status == StatusLeitura.LIDO) View.VISIBLE else View.GONE
        campoDataInicio.visibility = if (status == StatusLeitura.QUERO_LER) View.GONE else View.VISIBLE
    }

    private fun configurarCampoData(
        campo: TextInputLayout,
        valorAtual: () -> LocalDate?,
        aoEscolher: (LocalDate?) -> Unit
    ) {
        val edit = campo.editText ?: return
        edit.setOnClickListener {
            val inicial = valorAtual() ?: LocalDate.now()
            val dialogo = DatePickerDialog(
                this,
                { _, ano, mes, dia ->
                    val data = LocalDate.of(ano, mes + 1, dia)
                    aoEscolher(data)
                    edit.setText(Util.dataParaTexto(data))
                },
                inicial.year,
                inicial.monthValue - 1,
                inicial.dayOfMonth
            )
            // Não deixa escolher datas futuras.
            dialogo.datePicker.maxDate = LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            dialogo.show()
        }
        // O ícone "limpar" apaga o texto; aí a data também volta a ser nula.
        edit.doAfterTextChanged { texto -> if (texto.isNullOrEmpty()) aoEscolher(null) }
    }

    private fun texto(campo: TextInputLayout) = campo.editText?.text?.toString()?.trim().orEmpty()

    /** Busca o ISBN na Open Library e completa os campos que ainda estão vazios. */
    private fun preencherPeloIsbn() {
        val isbn = texto(campoIsbn)
        if (!ValidadorLivro.isbnValido(isbn)) {
            campoIsbn.error = getString(R.string.erro_isbn_para_busca)
            return
        }
        campoIsbn.error = null
        if (!Conexao.temInternet(this)) {
            Util.dialogoAlerta(this, getString(R.string.sem_internet_titulo), getString(R.string.sem_internet_msg))
            return
        }
        campoIsbn.isEndIconVisible = false
        lifecycleScope.launch {
            val resultado = ConsultaLivros.buscarPorIsbn(ValidadorLivro.normalizarIsbn(isbn))
            campoIsbn.isEndIconVisible = true
            when (resultado) {
                is Resultado.Sucesso -> {
                    val dados = resultado.valor
                    if (dados == null) {
                        Util.dialogoAlerta(
                            this@LivroFormActivity,
                            getString(R.string.isbn_nao_encontrado_titulo),
                            getString(R.string.isbn_nao_encontrado_msg)
                        )
                        return@launch
                    }
                    preencherSeVazio(campoTitulo, dados.titulo)
                    preencherSeVazio(campoAutor, dados.autor)
                    preencherSeVazio(campoCategoria, dados.categoria)
                    preencherSeVazio(campoPaginas, dados.paginas?.toString())
                    Util.mensagem(this@LivroFormActivity, getString(R.string.dados_preenchidos))
                }
                Resultado.SemConexao -> Util.dialogoAlerta(
                    this@LivroFormActivity,
                    getString(R.string.sem_internet_titulo),
                    getString(R.string.sem_internet_msg)
                )
                is Resultado.Indisponivel -> Util.dialogoAlerta(
                    this@LivroFormActivity,
                    getString(R.string.servico_indisponivel_titulo),
                    getString(R.string.servico_indisponivel_msg, resultado.codigo)
                )
            }
        }
    }

    private fun preencherSeVazio(campo: TextInputLayout, valor: String?) {
        if (texto(campo).isEmpty() && !valor.isNullOrBlank()) campo.editText?.setText(valor.take(150))
    }

    private fun salvar() {
        val status = statusSelecionado()
        val textoPaginas = texto(campoPaginas)
        val paginas = textoPaginas.toIntOrNull()
        val isbn = texto(campoIsbn).ifEmpty { null }

        val livro = Livro(
            id = livroId,
            usuarioId = usuarioId,
            titulo = texto(campoTitulo),
            autor = texto(campoAutor),
            isbn = isbn,
            categoria = texto(campoCategoria).ifEmpty { null },
            paginas = paginas,
            status = status,
            nota = if (status == StatusLeitura.LIDO && ratingNota.rating > 0f) ratingNota.rating else null,
            dataInicio = if (status == StatusLeitura.QUERO_LER) null else dataInicio,
            dataConclusao = if (status == StatusLeitura.LIDO) dataConclusao else null,
            comentario = texto(campoComentario).ifEmpty { null }
        )

        val erros = ValidadorLivro.validar(livro).toMutableMap()
        if (textoPaginas.isNotEmpty() && paginas == null) erros[Campo.PAGINAS] = getString(R.string.erro_paginas)
        campoTitulo.error = erros[Campo.TITULO]
        campoAutor.error = erros[Campo.AUTOR]
        campoIsbn.error = erros[Campo.ISBN]
        campoPaginas.error = erros[Campo.PAGINAS]
        campoDataInicio.error = erros[Campo.DATA_INICIO]
        campoDataConclusao.error = erros[Campo.DATA_CONCLUSAO]
        txtErroNota.text = erros[Campo.NOTA]
        txtErroNota.visibility = if (erros.containsKey(Campo.NOTA)) View.VISIBLE else View.GONE
        if (erros.isNotEmpty()) {
            Util.mensagem(this, getString(R.string.corrija_campos))
            return
        }

        // Guarda o ISBN sempre no mesmo formato (só dígitos), para comparar e consultar a API.
        val livroFinal = livro.copy(isbn = isbn?.let { ValidadorLivro.normalizarIsbn(it) })
        btnSalvar.isEnabled = false
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@LivroFormActivity).livroDao()
            val isbnFinal = livroFinal.isbn
            // Regra de negócio: o mesmo ISBN não pode aparecer duas vezes na estante.
            val repetido = isbnFinal != null && dao.buscarPorIsbn(usuarioId, isbnFinal)?.id.let { it != null && it != livroId }
            if (repetido) {
                btnSalvar.isEnabled = true
                campoIsbn.error = getString(R.string.erro_isbn_repetido)
                return@launch
            }
            if (livroId == 0) dao.inserir(livroFinal) else dao.atualizar(livroFinal)
            Util.mensagem(
                this@LivroFormActivity,
                getString(if (livroId == 0) R.string.livro_adicionado else R.string.livro_atualizado)
            )
            setResult(RESULT_OK)
            finish()
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_AUTOR = "autor"
        const val EXTRA_ISBN = "isbn"
        const val EXTRA_CATEGORIA = "categoria"
        const val EXTRA_PAGINAS = "paginas"
        private const val ESTADO_INICIO = "dataInicio"
        private const val ESTADO_CONCLUSAO = "dataConclusao"
    }
}
