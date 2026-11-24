package br.upf.booki.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.upf.booki.R
import br.upf.booki.api.ConsultaLivros
import br.upf.booki.api.LivroPesquisa
import br.upf.booki.api.Resultado
import br.upf.booki.data.AppDatabase
import br.upf.booki.model.Livro
import br.upf.booki.model.StatusLeitura
import br.upf.booki.model.ValidadorLivro
import br.upf.booki.model.ValidadorLivro.Campo
import br.upf.booki.util.CarregadorCapa
import br.upf.booki.util.Conexao
import br.upf.booki.util.Sessao
import br.upf.booki.util.Util
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Formulário de livro: cria um livro novo ou edita um existente
 * (quando recebe putExtra("id", ...)).
 */
class LivroFormActivity : AppCompatActivity() {

    private lateinit var campoBusca: TextInputLayout
    private lateinit var editBusca: TextInputEditText
    private lateinit var progressoBusca: ProgressBar
    private lateinit var imgCapaForm: ImageView
    private lateinit var imgPlaceholderCapaForm: View
    private lateinit var btnAlterarCapa: MaterialButton
    private lateinit var btnRemoverCapa: MaterialButton
    private lateinit var campoTitulo: TextInputLayout
    private lateinit var campoAutor: TextInputLayout
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
    /** ISBN mantido internamente quando vem da API ou da edição, sem campo visual no formulário. */
    private var isbnInterno: String? = null
    /** URL remota (API) ou caminho absoluto local (`filesDir/capas/...`). */
    private var capaUrl: String? = null
    private var arquivoFotoTemp: File? = null
    private var uriFotoTemp: Uri? = null
    private var dataInicio: LocalDate? = null
    private var dataConclusao: LocalDate? = null
    private var buscando = false

    private val selecionarGaleria =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) importarCapaLocal(uri)
        }

    private val capturarFoto =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
            val uri = uriFotoTemp
            val arquivoTemp = arquivoFotoTemp
            uriFotoTemp = null
            arquivoFotoTemp = null
            if (sucesso && uri != null) {
                importarCapaLocal(uri, arquivoTemp)
            } else {
                arquivoTemp?.delete()
            }
        }

    private val pedirPermissaoCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            if (concedida) {
                abrirCamera()
            } else {
                Util.mensagem(this, getString(R.string.permissao_camera_negada))
            }
        }

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

        campoBusca = findViewById(R.id.campoBuscaLivroForm)
        editBusca = findViewById(R.id.editBuscaLivroForm)
        progressoBusca = findViewById(R.id.progressoBuscaForm)
        imgCapaForm = findViewById(R.id.imgCapaForm)
        imgPlaceholderCapaForm = findViewById(R.id.imgPlaceholderCapaForm)
        btnAlterarCapa = findViewById(R.id.btnAlterarCapa)
        btnRemoverCapa = findViewById(R.id.btnRemoverCapa)
        campoTitulo = findViewById(R.id.campoTitulo)
        campoAutor = findViewById(R.id.campoAutor)
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
        btnAlterarCapa.setOnClickListener { mostrarOpcoesCapa() }
        btnRemoverCapa.setOnClickListener {
            capaUrl = null
            atualizarPreviewCapa()
        }

        campoBusca.setEndIconOnClickListener { buscarPorPalavrasChave() }
        editBusca.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                buscarPorPalavrasChave()
                true
            } else {
                false
            }
        }
        listOf(campoBusca, campoTitulo, campoAutor, campoPaginas).forEach { campo ->
            campo.editText?.doAfterTextChanged { campo.error = null }
        }

        if (savedInstanceState != null) {
            // Ao girar a tela os textos voltam sozinhos; datas, capa e ISBN interno guardamos à parte.
            isbnInterno = savedInstanceState.getString(ESTADO_ISBN)
            capaUrl = savedInstanceState.getString(ESTADO_CAPA)
            dataInicio = lerData(savedInstanceState, ESTADO_INICIO)
            dataConclusao = lerData(savedInstanceState, ESTADO_CONCLUSAO)
            atualizarPreviewCapa()
        } else if (livroId != 0) {
            carregarLivro()
        } else {
            preencherSugestao()
            atualizarPreviewCapa()
        }
        atualizarCamposPorStatus()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        isbnInterno?.let { outState.putString(ESTADO_ISBN, it) }
        capaUrl?.let { outState.putString(ESTADO_CAPA, it) }
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
        isbnInterno = normalizarIsbnSeValido(intent.getStringExtra(EXTRA_ISBN))
        capaUrl = intent.getStringExtra(EXTRA_CAPA)?.trim()?.ifBlank { null }
        campoCategoria.editText?.setText(intent.getStringExtra(EXTRA_CATEGORIA))
        val paginas = intent.getIntExtra(EXTRA_PAGINAS, 0)
        if (paginas > 0) campoPaginas.editText?.setText(paginas.toString())
    }

    private fun preencher(livro: Livro) {
        campoTitulo.editText?.setText(livro.titulo)
        campoAutor.editText?.setText(livro.autor)
        isbnInterno = normalizarIsbnSeValido(livro.isbn)
        capaUrl = livro.capaUrl?.trim()?.ifBlank { null }
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
        atualizarPreviewCapa()
        atualizarCamposPorStatus()
    }

    private fun mostrarOpcoesCapa() {
        val opcoes = arrayOf(
            getString(R.string.escolher_galeria),
            getString(R.string.tirar_foto)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.escolher_capa_titulo)
            .setItems(opcoes) { _, indice ->
                if (indice == 0) {
                    selecionarGaleria.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    verificarPermissaoEAbrirCamera()
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun verificarPermissaoEAbrirCamera() {
        val concedida = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (concedida) {
            abrirCamera()
        } else {
            pedirPermissaoCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamera() {
        try {
            val arquivo = CarregadorCapa.criarArquivoTemporarioCamera(this)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", arquivo)
            arquivoFotoTemp = arquivo
            uriFotoTemp = uri
            capturarFoto.launch(uri)
        } catch (_: ActivityNotFoundException) {
            arquivoFotoTemp?.delete()
            arquivoFotoTemp = null
            uriFotoTemp = null
            Util.mensagem(this, getString(R.string.camera_indisponivel))
        } catch (_: IllegalArgumentException) {
            arquivoFotoTemp?.delete()
            arquivoFotoTemp = null
            uriFotoTemp = null
            Util.mensagem(this, getString(R.string.camera_indisponivel))
        }
    }

    private fun importarCapaLocal(uri: Uri, arquivoTempParaApagar: File? = null) {
        btnAlterarCapa.isEnabled = false
        lifecycleScope.launch {
            try {
                val caminho = CarregadorCapa.salvarCapaLocal(this@LivroFormActivity, uri)
                if (caminho != null) {
                    capaUrl = caminho
                    atualizarPreviewCapa()
                    Util.mensagem(this@LivroFormActivity, getString(R.string.capa_atualizada))
                } else {
                    Util.mensagem(this@LivroFormActivity, getString(R.string.erro_salvar_capa))
                }
            } finally {
                arquivoTempParaApagar?.delete()
                btnAlterarCapa.isEnabled = true
            }
        }
    }

    private fun atualizarPreviewCapa() {
        val temCapa = !capaUrl.isNullOrBlank()
        CarregadorCapa.carregar(imgCapaForm, capaUrl, imgPlaceholderCapaForm)
        btnAlterarCapa.setText(if (temCapa) R.string.alterar_capa else R.string.adicionar_capa)
        btnRemoverCapa.visibility = if (temCapa) View.VISIBLE else View.GONE
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

    /** Busca livros por palavras-chave (Google Books com fallback Open Library) e abre seleção. */
    private fun buscarPorPalavrasChave() {
        if (buscando) return
        val termo = texto(campoBusca)
        if (termo.isEmpty()) {
            campoBusca.error = getString(R.string.erro_busca_form_vazia)
            return
        }
        campoBusca.error = null
        esconderTeclado()

        if (!Conexao.temInternet(this)) {
            Util.dialogoAlerta(this, getString(R.string.sem_internet_titulo), getString(R.string.sem_internet_msg))
            return
        }

        buscando = true
        campoBusca.isEnabled = false
        editBusca.isEnabled = false
        campoBusca.isEndIconVisible = false
        progressoBusca.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                when (val resultado = ConsultaLivros.pesquisarLivros(termo, limite = 10)) {
                    is Resultado.Sucesso -> {
                        val livros = resultado.valor
                        if (livros.isEmpty()) {
                            Util.mensagem(this@LivroFormActivity, getString(R.string.pesquisa_vazia))
                        } else {
                            mostrarBottomSheetResultados(livros)
                        }
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
            } finally {
                buscando = false
                campoBusca.isEnabled = true
                editBusca.isEnabled = true
                campoBusca.isEndIconVisible = true
                progressoBusca.visibility = View.GONE
            }
        }
    }

    private fun mostrarBottomSheetResultados(livros: List<LivroPesquisa>) {
        val dialogo = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_resultados_livro, null)
        val lista = view.findViewById<RecyclerView>(R.id.listaResultadosForm)
        lista.layoutManager = LinearLayoutManager(this)
        lista.adapter = ResultadoBuscaFormAdapter(livros) { escolhido ->
            aplicarResultadoSelecionado(escolhido)
            dialogo.dismiss()
        }
        dialogo.setContentView(view)
        dialogo.show()
    }

    private fun aplicarResultadoSelecionado(escolhido: LivroPesquisa) {
        campoTitulo.editText?.setText(escolhido.titulo.take(150))
        if (escolhido.autor.isNotBlank()) {
            campoAutor.editText?.setText(escolhido.autor.take(120))
        }
        escolhido.paginas?.let {
            campoPaginas.editText?.setText(it.toString())
        }
        escolhido.categoria?.takeIf { it.isNotBlank() }?.let {
            if (texto(campoCategoria).isEmpty()) campoCategoria.editText?.setText(it.take(60))
        }
        // ISBN fica guardado apenas internamente, sem aparecer no formulário.
        isbnInterno = normalizarIsbnSeValido(escolhido.isbn)
        // Capa vinda da API é preenchida automaticamente (pode ser trocada depois pelo usuário).
        if (!escolhido.capaUrl.isNullOrBlank()) {
            capaUrl = escolhido.capaUrl
            atualizarPreviewCapa()
        }
        Util.mensagem(this, getString(R.string.dados_preenchidos_busca))
    }

    private fun normalizarIsbnSeValido(isbn: String?): String? {
        val limpo = isbn?.let { ValidadorLivro.normalizarIsbn(it) }?.ifBlank { null } ?: return null
        return limpo.takeIf { ValidadorLivro.isbnValido(it) }
    }

    private fun esconderTeclado() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val foco = currentFocus ?: editBusca
        imm?.hideSoftInputFromWindow(foco.windowToken, 0)
    }

    private fun salvar() {
        val status = statusSelecionado()
        val textoPaginas = texto(campoPaginas)
        val paginas = textoPaginas.toIntOrNull()
        val isbnFinal = normalizarIsbnSeValido(isbnInterno)

        val livro = Livro(
            id = livroId,
            usuarioId = usuarioId,
            titulo = texto(campoTitulo),
            autor = texto(campoAutor),
            isbn = isbnFinal,
            categoria = texto(campoCategoria).ifEmpty { null },
            paginas = paginas,
            status = status,
            nota = if (status == StatusLeitura.LIDO && ratingNota.rating > 0f) ratingNota.rating else null,
            dataInicio = if (status == StatusLeitura.QUERO_LER) null else dataInicio,
            dataConclusao = if (status == StatusLeitura.LIDO) dataConclusao else null,
            comentario = texto(campoComentario).ifEmpty { null },
            capaUrl = capaUrl?.trim()?.ifBlank { null }
        )

        val erros = ValidadorLivro.validar(livro).toMutableMap()
        if (textoPaginas.isNotEmpty() && paginas == null) erros[Campo.PAGINAS] = getString(R.string.erro_paginas)
        campoTitulo.error = erros[Campo.TITULO]
        campoAutor.error = erros[Campo.AUTOR]
        campoPaginas.error = erros[Campo.PAGINAS]
        campoDataInicio.error = erros[Campo.DATA_INICIO]
        campoDataConclusao.error = erros[Campo.DATA_CONCLUSAO]
        txtErroNota.text = erros[Campo.NOTA]
        txtErroNota.visibility = if (erros.containsKey(Campo.NOTA)) View.VISIBLE else View.GONE
        if (erros.isNotEmpty()) {
            Util.mensagem(this, getString(R.string.corrija_campos))
            return
        }

        btnSalvar.isEnabled = false
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@LivroFormActivity).livroDao()
            if (livroId == 0) dao.inserir(livro) else dao.atualizar(livro)
            Util.mensagem(
                this@LivroFormActivity,
                getString(if (livroId == 0) R.string.livro_adicionado else R.string.livro_atualizado)
            )
            setResult(RESULT_OK)
            finish()
        }
    }

    private class ResultadoBuscaFormAdapter(
        private val itens: List<LivroPesquisa>,
        private val aoSelecionar: (LivroPesquisa) -> Unit
    ) : RecyclerView.Adapter<ResultadoBuscaFormAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_resultado_form, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.mostrar(itens[position])
        }

        override fun getItemCount() = itens.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val imgCapa: ImageView = view.findViewById(R.id.imgCapaResultadoForm)
            private val placeholder: View = view.findViewById(R.id.imgPlaceholderResultadoForm)
            private val titulo: TextView = view.findViewById(R.id.txtTituloResultadoForm)
            private val autor: TextView = view.findViewById(R.id.txtAutorResultadoForm)
            private val paginas: TextView = view.findViewById(R.id.txtPaginasResultadoForm)

            fun mostrar(livro: LivroPesquisa) {
                val context = itemView.context
                CarregadorCapa.carregar(imgCapa, livro.capaUrl, placeholder, maxLadoPx = 300)
                titulo.text = livro.titulo
                autor.text = livro.autor.ifBlank { context.getString(R.string.autor_desconhecido) }
                if (livro.paginas != null) {
                    paginas.text = context.getString(R.string.paginas_total, livro.paginas)
                    paginas.visibility = View.VISIBLE
                } else {
                    paginas.visibility = View.GONE
                }
                itemView.setOnClickListener { aoSelecionar(livro) }
            }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_AUTOR = "autor"
        const val EXTRA_ISBN = "isbn"
        const val EXTRA_CATEGORIA = "categoria"
        const val EXTRA_PAGINAS = "paginas"
        const val EXTRA_CAPA = "capaUrl"
        private const val ESTADO_ISBN = "isbnInterno"
        private const val ESTADO_CAPA = "capaUrl"
        private const val ESTADO_INICIO = "dataInicio"
        private const val ESTADO_CONCLUSAO = "dataConclusao"
    }
}
