package br.upf.booki.ui.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.upf.booki.R
import br.upf.booki.model.Livro

/** Adapter da lista de livros da estante. [aoClicar] recebe o livro tocado. */
class LivroAdapter(
    private val aoClicar: (Livro) -> Unit
) : RecyclerView.Adapter<LivroAdapter.LivroViewHolder>() {

    private var livros: List<Livro> = emptyList()

    fun atualizar(novos: List<Livro>) {
        livros = novos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LivroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_livro, parent, false)
        return LivroViewHolder(view)
    }

    override fun onBindViewHolder(holder: LivroViewHolder, position: Int) {
        holder.mostrar(livros[position])
    }

    override fun getItemCount() = livros.size

    inner class LivroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val faixa: View = view.findViewById(R.id.faixaStatus)
        private val titulo: TextView = view.findViewById(R.id.txtTituloItem)
        private val autor: TextView = view.findViewById(R.id.txtAutorItem)
        private val status: TextView = view.findViewById(R.id.txtStatusItem)
        private val rating: RatingBar = view.findViewById(R.id.ratingItem)

        fun mostrar(livro: Livro) {
            val cor = ContextCompat.getColor(itemView.context, livro.status.cor)
            faixa.setBackgroundColor(cor)
            titulo.text = livro.titulo
            autor.text = livro.autor
            status.setText(livro.status.rotulo)
            status.setTextColor(cor)
            rating.visibility = if (livro.nota != null) View.VISIBLE else View.GONE
            rating.rating = livro.nota ?: 0f
            itemView.setOnClickListener { aoClicar(livro) }
        }
    }
}
