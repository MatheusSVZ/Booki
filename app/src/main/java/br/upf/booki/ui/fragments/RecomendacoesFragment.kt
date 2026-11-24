package br.upf.booki.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import br.upf.booki.R

/**
 * Recomendações com base no histórico de leitura.
 * A implementação está descrita em docs/TAREFAS-MILLENA.md (branch feature/recomendacoes).
 */
class RecomendacoesFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_mensagem, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.txtMensagem).setText(R.string.recomendacoes_em_breve)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.nav_recomendacoes)
    }
}
