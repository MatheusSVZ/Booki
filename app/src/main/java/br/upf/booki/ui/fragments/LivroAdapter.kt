package br.upf.booki.ui.fragments

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.upf.booki.R
import br.upf.booki.model.Livro
import br.upf.booki.model.StatusLeitura
import br.upf.booki.util.CarregadorCapa
import br.upf.booki.util.Util
import com.google.android.material.card.MaterialCardView
import java.util.Locale

/** Adapter da estante: destaca o primeiro livro em leitura no topo e exibe os demais no estilo de cards. */
class LivroAdapter(
    private val aoClicar: (Livro) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Item {
        data class DestaqueLendo(val livro: Livro) : Item()
        data class Comum(val livro: Livro) : Item()
    }

    private var itens: List<Item> = emptyList()

    fun atualizar(novos: List<Livro>, exibirDestaqueLendo: Boolean = true) {
        val destaque = if (exibirDestaqueLendo) {
            novos.firstOrNull { it.status == StatusLeitura.LENDO }
        } else {
            null
        }
        itens = buildList {
            if (destaque != null) {
                add(Item.DestaqueLendo(destaque))
            }
            novos.filter { it.id != destaque?.id }.forEach { add(Item.Comum(it)) }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (itens[position]) {
        is Item.DestaqueLendo -> TIPO_DESTAQUE
        is Item.Comum -> TIPO_COMUM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_DESTAQUE) {
            val view = inflater.inflate(R.layout.item_lendo_agora, parent, false)
            DestaqueViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_livro, parent, false)
            LivroViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = itens[position]) {
            is Item.DestaqueLendo -> (holder as DestaqueViewHolder).mostrar(item.livro)
            is Item.Comum -> (holder as LivroViewHolder).mostrar(item.livro)
        }
    }

    override fun getItemCount() = itens.size

    inner class DestaqueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card: MaterialCardView = view.findViewById(R.id.cardLendoAgora)
        private val imgCapa: ImageView = view.findViewById(R.id.imgCapaDestaque)
        private val titulo: TextView = view.findViewById(R.id.txtTituloDestaque)
        private val autor: TextView = view.findViewById(R.id.txtAutorDestaque)
        private val info: TextView = view.findViewById(R.id.txtInfoDestaque)

        fun mostrar(livro: Livro) {
            val context = itemView.context
            CarregadorCapa.carregar(imgCapa, livro.capaUrl, maxLadoPx = 400)
            titulo.text = livro.titulo
            autor.text = livro.autor

            val detalhes = buildList {
                livro.paginas?.let { add(context.getString(R.string.paginas_total, it)) }
                livro.dataInicio?.let { add(context.getString(R.string.inicio_resumo, Util.dataParaTexto(it))) }
            }
            if (detalhes.isNotEmpty()) {
                info.text = detalhes.joinToString(" • ")
                info.visibility = View.VISIBLE
            } else {
                info.visibility = View.GONE
            }

            card.setOnClickListener { aoClicar(livro) }
        }
    }

    inner class LivroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val capa: MaterialCardView = view.findViewById(R.id.capaStatus)
        private val imgCapa: ImageView = view.findViewById(R.id.imgCapaItem)
        private val titulo: TextView = view.findViewById(R.id.txtTituloItem)
        private val autor: TextView = view.findViewById(R.id.txtAutorItem)
        private val status: TextView = view.findViewById(R.id.txtStatusItem)
        private val nota: TextView = view.findViewById(R.id.txtNotaItem)

        fun mostrar(livro: Livro) {
            val context = itemView.context
            val corStatus = ContextCompat.getColor(context, livro.status.cor)
            capa.setCardBackgroundColor(corStatus)
            CarregadorCapa.carregar(imgCapa, livro.capaUrl, maxLadoPx = 300)
            titulo.text = livro.titulo
            autor.text = livro.autor

            status.setText(livro.status.rotulo)
            status.backgroundTintList = ColorStateList.valueOf(corStatus)
            val corTextoStatus = if (livro.status == StatusLeitura.LENDO) R.color.booki_texto else R.color.white
            status.setTextColor(ContextCompat.getColor(context, corTextoStatus))

            val avaliacao = livro.nota
            if (avaliacao != null) {
                val notaFormatada = String.format(Locale.forLanguageTag("pt-BR"), "%.1f", avaliacao)
                nota.text = context.getString(R.string.nota_resumo, notaFormatada)
                nota.visibility = View.VISIBLE
            } else {
                nota.visibility = View.GONE
            }

            itemView.setOnClickListener { aoClicar(livro) }
        }
    }

    private companion object {
        const val TIPO_DESTAQUE = 0
        const val TIPO_COMUM = 1
    }
}

