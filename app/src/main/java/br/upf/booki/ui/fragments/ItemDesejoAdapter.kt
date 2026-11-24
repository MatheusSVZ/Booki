package br.upf.booki.ui.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.upf.booki.R
import br.upf.booki.model.ItemDesejo
import br.upf.booki.util.CarregadorCapa
import br.upf.booki.util.Util
import com.google.android.material.button.MaterialButton

/** Adapter dos livros salvos na Lista de Desejos do usuário (Room). */
class ItemDesejoAdapter(
    private val aoVerLoja: (ItemDesejo) -> Unit,
    private val aoRemover: (ItemDesejo) -> Unit
) : RecyclerView.Adapter<ItemDesejoAdapter.DesejoViewHolder>() {

    private var itens: List<ItemDesejo> = emptyList()

    fun atualizar(novos: List<ItemDesejo>) {
        itens = novos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DesejoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_desejo, parent, false)
        return DesejoViewHolder(view)
    }

    override fun onBindViewHolder(holder: DesejoViewHolder, position: Int) {
        holder.mostrar(itens[position])
    }

    override fun getItemCount() = itens.size

    inner class DesejoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgCapa: ImageView = view.findViewById(R.id.imgCapaDesejo)
        private val placeholder: View = view.findViewById(R.id.imgPlaceholderDesejo)
        private val titulo: TextView = view.findViewById(R.id.txtTituloDesejo)
        private val autor: TextView = view.findViewById(R.id.txtAutorDesejo)
        private val data: TextView = view.findViewById(R.id.txtDataDesejo)
        private val preco: TextView = view.findViewById(R.id.txtPrecoDesejo)
        private val btnVerLoja: MaterialButton = view.findViewById(R.id.btnVerLojaDesejo)
        private val btnRemover: MaterialButton = view.findViewById(R.id.btnRemoverDesejo)

        fun mostrar(item: ItemDesejo) {
            val context = itemView.context
            CarregadorCapa.carregar(imgCapa, item.capaUrl, placeholder, maxLadoPx = 300)
            titulo.text = item.titulo
            autor.text = item.autor.ifBlank { context.getString(R.string.autor_desconhecido) }
            data.text = context.getString(R.string.adicionado_em, Util.dataParaTexto(item.dataAdicao))

            if (!item.precoTexto.isNullOrBlank()) {
                preco.text = item.precoTexto
                preco.setTextColor(ContextCompat.getColor(context, R.color.booki_primaria))
            } else {
                preco.setText(R.string.consultar_preco_loja)
                preco.setTextColor(ContextCompat.getColor(context, R.color.booki_texto_secundario))
            }

            btnVerLoja.setOnClickListener { aoVerLoja(item) }
            btnRemover.setOnClickListener { aoRemover(item) }
        }
    }
}

