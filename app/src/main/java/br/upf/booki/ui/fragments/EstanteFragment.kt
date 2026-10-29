package br.upf.booki.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.upf.booki.R
import br.upf.booki.data.AppDatabase
import br.upf.booki.model.StatusLeitura
import br.upf.booki.ui.LivroDetalheActivity
import br.upf.booki.ui.LivroFormActivity
import br.upf.booki.util.Sessao
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

/** Estante do usuário: lista os livros, filtra por status e abre o cadastro. */
class EstanteFragment : Fragment() {

    private lateinit var adapter: LivroAdapter
    private lateinit var grupoFiltro: ChipGroup
    private lateinit var txtVazia: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_estante, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = LivroAdapter { livro ->
            startActivity(
                Intent(requireContext(), LivroDetalheActivity::class.java)
                    .putExtra(LivroFormActivity.EXTRA_ID, livro.id)
            )
        }
        val lista = view.findViewById<RecyclerView>(R.id.listaLivros)
        lista.layoutManager = LinearLayoutManager(requireContext())
        lista.adapter = adapter

        txtVazia = view.findViewById(R.id.txtEstanteVazia)
        grupoFiltro = view.findViewById(R.id.grupoFiltro)
        grupoFiltro.setOnCheckedStateChangeListener { _, _ -> carregar() }

        view.findViewById<View>(R.id.fabNovoLivro).setOnClickListener {
            startActivity(Intent(requireContext(), LivroFormActivity::class.java))
        }
    }

    /** Ciclo de vida: ao voltar do cadastro, edição ou exclusão, a lista é recarregada. */
    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.nav_estante)
        carregar()
    }

    private fun filtroSelecionado(): StatusLeitura? = when (grupoFiltro.checkedChipId) {
        R.id.chipLido -> StatusLeitura.LIDO
        R.id.chipLendo -> StatusLeitura.LENDO
        R.id.chipQueroLer -> StatusLeitura.QUERO_LER
        else -> null
    }

    private fun carregar() {
        val usuarioId = Sessao(requireContext()).usuarioId ?: return
        val filtro = filtroSelecionado()
        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).livroDao()
            val livros = if (filtro == null) dao.listar(usuarioId) else dao.listarPorStatus(usuarioId, filtro)
            adapter.atualizar(livros)
            txtVazia.visibility = if (livros.isEmpty()) View.VISIBLE else View.GONE
            txtVazia.setText(if (filtro == null) R.string.estante_vazia else R.string.filtro_vazio)
        }
    }
}
