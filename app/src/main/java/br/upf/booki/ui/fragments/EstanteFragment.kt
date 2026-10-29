package br.upf.booki.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import br.upf.booki.R

/** Estante do usuário (a lista de livros entra na feature de CRUD). */
class EstanteFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_mensagem, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageView>(R.id.imgMensagem).setImageResource(R.drawable.ic_estante)
        view.findViewById<TextView>(R.id.txtMensagem).setText(R.string.estante_vazia)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.nav_estante)
    }
}
