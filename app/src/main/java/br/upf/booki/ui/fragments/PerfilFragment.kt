package br.upf.booki.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.upf.booki.R
import br.upf.booki.data.AppDatabase
import br.upf.booki.ui.LoginActivity
import br.upf.booki.util.Sessao
import br.upf.booki.util.Util
import kotlinx.coroutines.launch

/** Dados do usuário logado e botão de sair. */
class PerfilFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_perfil, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sessao = Sessao(requireContext())
        val usuarioId = sessao.usuarioId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val usuario = AppDatabase.getInstance(requireContext()).userDao().buscarPorId(usuarioId)
            view.findViewById<TextView>(R.id.txtNomePerfil).text = usuario?.nome.orEmpty()
            view.findViewById<TextView>(R.id.txtEmailPerfil).text = usuario?.email.orEmpty()
        }

        view.findViewById<View>(R.id.btnSair).setOnClickListener {
            Util.dialogoConfirma(requireContext(), getString(R.string.sair), getString(R.string.confirma_sair)) {
                sessao.sair()
                // Limpa a pilha para o botão voltar não retornar à estante.
                startActivity(
                    Intent(requireContext(), LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.nav_perfil)
    }
}
