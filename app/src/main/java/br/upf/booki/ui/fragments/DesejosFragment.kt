package br.upf.booki.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.upf.booki.R
import br.upf.booki.api.ConsultaLivros
import br.upf.booki.api.LivroPesquisa
import br.upf.booki.api.Resultado
import br.upf.booki.data.AppDatabase
import br.upf.booki.model.ItemDesejo
import br.upf.booki.util.Conexao
import br.upf.booki.util.Sessao
import br.upf.booki.util.Util
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/** Aba Desejos: pesquisa livros e valores no Google Books e gerencia a lista de desejos no Room. */
class DesejosFragment : Fragment() {

    private lateinit var painelPesquisa: View
    private lateinit var painelListaDesejos: View
    private lateinit var campoBusca: TextInputLayout
    private lateinit var editBusca: TextInputEditText
    private lateinit var progressoBusca: ProgressBar
    private lateinit var txtEstadoPesquisa: TextView
    private lateinit var txtDesejosVazios: TextView

    private lateinit var pesquisaAdapter: PesquisaLivroAdapter
    private lateinit var desejoAdapter: ItemDesejoAdapter
    private var buscando = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_desejos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        painelPesquisa = view.findViewById(R.id.painelPesquisa)
        painelListaDesejos = view.findViewById(R.id.painelListaDesejos)
        campoBusca = view.findViewById(R.id.campoBuscaLivro)
        editBusca = view.findViewById(R.id.editBuscaLivro)
        progressoBusca = view.findViewById(R.id.progressoBusca)
        txtEstadoPesquisa = view.findViewById(R.id.txtEstadoPesquisa)
        txtDesejosVazios = view.findViewById(R.id.txtDesejosVazios)

        pesquisaAdapter = PesquisaLivroAdapter(
            aoVerLoja = { livro -> abrirNaLoja(livro.linkLoja, livro.titulo, livro.autor) },
            aoAdicionarDesejo = { livro -> salvarDesejo(livro) }
        )
        val listaResultados = view.findViewById<RecyclerView>(R.id.listaResultados)
        listaResultados.layoutManager = LinearLayoutManager(requireContext())
        listaResultados.adapter = pesquisaAdapter

        desejoAdapter = ItemDesejoAdapter(
            aoVerLoja = { item -> abrirNaLoja(item.linkLoja, item.titulo, item.autor) },
            aoRemover = { item -> confirmarRemocao(item) }
        )
        val listaDesejos = view.findViewById<RecyclerView>(R.id.listaDesejos)
        listaDesejos.layoutManager = LinearLayoutManager(requireContext())
        listaDesejos.adapter = desejoAdapter

        val grupoModo = view.findViewById<ChipGroup>(R.id.grupoModoDesejos)
        grupoModo.setOnCheckedStateChangeListener { _, _ ->
            atualizarVisao(grupoModo.checkedChipId == R.id.chipPesquisar)
        }

        editBusca.doAfterTextChanged { campoBusca.error = null }
        campoBusca.setEndIconOnClickListener { executarBusca() }
        editBusca.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                executarBusca()
                true
            } else {
                false
            }
        }

        atualizarVisao(true)
        carregarDesejos()
    }

    override fun onResume() {
        super.onResume()
        carregarDesejos()
    }

    private fun atualizarVisao(modoPesquisar: Boolean) {
        painelPesquisa.visibility = if (modoPesquisar) View.VISIBLE else View.GONE
        painelListaDesejos.visibility = if (modoPesquisar) View.GONE else View.VISIBLE
        if (!modoPesquisar) {
            esconderTeclado()
            carregarDesejos()
        }
    }

    private fun executarBusca() {
        if (buscando) return
        val termo = editBusca.text?.toString()?.trim().orEmpty()
        if (termo.isEmpty()) {
            campoBusca.error = getString(R.string.pesquisa_termo_vazio)
            return
        }
        campoBusca.error = null
        esconderTeclado()

        if (!Conexao.temInternet(requireContext())) {
            Util.dialogoAlerta(
                requireContext(),
                getString(R.string.sem_internet_titulo),
                getString(R.string.sem_internet_msg)
            )
            return
        }

        buscando = true
        campoBusca.isEnabled = false
        editBusca.isEnabled = false
        campoBusca.isEndIconVisible = false
        progressoBusca.visibility = View.VISIBLE
        txtEstadoPesquisa.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resultado = ConsultaLivros.pesquisarLivros(termo)
                when (resultado) {
                    is Resultado.Sucesso -> {
                        val livros = resultado.valor
                        pesquisaAdapter.atualizarResultados(livros)
                        if (livros.isEmpty()) {
                            txtEstadoPesquisa.setText(R.string.pesquisa_vazia)
                            txtEstadoPesquisa.visibility = View.VISIBLE
                        } else {
                            txtEstadoPesquisa.visibility = View.GONE
                        }
                    }
                    Resultado.SemConexao -> {
                        Util.dialogoAlerta(
                            requireContext(),
                            getString(R.string.sem_internet_titulo),
                            getString(R.string.sem_internet_msg)
                        )
                        if (pesquisaAdapter.itemCount == 0) {
                            txtEstadoPesquisa.setText(R.string.sem_internet_msg)
                            txtEstadoPesquisa.visibility = View.VISIBLE
                        }
                    }
                    is Resultado.Indisponivel -> {
                        val mensagem = getString(R.string.pesquisa_indisponivel, resultado.codigo)
                        Util.dialogoAlerta(
                            requireContext(),
                            getString(R.string.servico_indisponivel_titulo),
                            mensagem
                        )
                        if (pesquisaAdapter.itemCount == 0) {
                            txtEstadoPesquisa.text = mensagem
                            txtEstadoPesquisa.visibility = View.VISIBLE
                        }
                    }
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

    private fun carregarDesejos() {
        val usuarioId = Sessao(requireContext()).usuarioId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val itens = AppDatabase.getInstance(requireContext()).itemDesejoDao().listar(usuarioId)
            desejoAdapter.atualizar(itens)
            txtDesejosVazios.visibility = if (itens.isEmpty()) View.VISIBLE else View.GONE
            pesquisaAdapter.atualizarSalvos(montarChavesSalvas(itens))
        }
    }

    private fun salvarDesejo(livro: LivroPesquisa) {
        val usuarioId = Sessao(requireContext()).usuarioId ?: return
        val autor = livro.autor.ifBlank { getString(R.string.autor_desconhecido) }
        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).itemDesejoDao()
            if (dao.existe(usuarioId, livro.titulo, autor, livro.isbn)) {
                Util.mensagem(requireContext(), getString(R.string.desejo_ja_salvo))
                carregarDesejos()
                return@launch
            }
            dao.inserir(
                ItemDesejo(
                    usuarioId = usuarioId,
                    titulo = livro.titulo,
                    autor = autor,
                    isbn = livro.isbn,
                    precoTexto = livro.precoTexto,
                    linkLoja = livro.linkLoja,
                    capaUrl = livro.capaUrl
                )
            )
            Util.mensagem(requireContext(), getString(R.string.desejo_salvo))
            carregarDesejos()
        }
    }

    private fun confirmarRemocao(item: ItemDesejo) {
        Util.dialogoConfirma(
            requireContext(),
            getString(R.string.remover_desejo_titulo),
            getString(R.string.confirma_remover_desejo, item.titulo)
        ) {
            viewLifecycleOwner.lifecycleScope.launch {
                AppDatabase.getInstance(requireContext()).itemDesejoDao().excluir(item)
                Util.mensagem(requireContext(), getString(R.string.desejo_removido))
                carregarDesejos()
            }
        }
    }

    private fun abrirNaLoja(linkLoja: String?, titulo: String, autor: String) {
        val url = if (!linkLoja.isNullOrBlank()) {
            linkLoja
        } else {
            val consulta = Uri.encode("$titulo $autor livro".trim())
            "https://www.google.com/search?tbm=shop&q=$consulta"
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Util.mensagem(requireContext(), getString(R.string.erro_abrir_navegador))
        }
    }

    private fun montarChavesSalvas(itens: List<ItemDesejo>): Set<String> = buildSet {
        itens.forEach { item ->
            add(PesquisaLivroAdapter.chaveTituloAutor(item.titulo, item.autor))
            if (item.autor == getString(R.string.autor_desconhecido)) {
                add(PesquisaLivroAdapter.chaveTituloAutor(item.titulo, ""))
            }
            item.isbn?.takeIf { it.isNotBlank() }?.let { add(PesquisaLivroAdapter.chaveIsbn(it)) }
        }
    }

    private fun esconderTeclado() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val foco = view?.findFocus() ?: editBusca
        imm?.hideSoftInputFromWindow(foco.windowToken, 0)
    }
}

