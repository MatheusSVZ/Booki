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
import br.upf.booki.api.LivroPesquisa
import br.upf.booki.util.CarregadorCapa
import com.google.android.material.button.MaterialButton

class PesquisaLivroAdapter(
    private val aoVerLoja: (LivroPesquisa) -> Unit,
    private val aoAdicionarDesejo: (LivroPesquisa) -> Unit
) : RecyclerView.Adapter<PesquisaLivroAdapter.ResultadoViewHolder>() {

    private var resultados: List<LivroPesquisa> = emptyList()
    private var chavesSalvas: Set<String> = emptySet()

    fun atualizarResultados(novos: List<LivroPesquisa>) {
        resultados = novos
        notifyDataSetChanged()
    }

    fun atualizarSalvos(chaves: Set<String>) {
        chavesSalvas = chaves
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultadoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pesquisa_livro, parent, false)
        return ResultadoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultadoViewHolder, position: Int) {
        holder.mostrar(resultados[position])
    }

    override fun getItemCount() = resultados.size

    private fun estaSalvo(livro: LivroPesquisa): Boolean {
        val autor = livro.autor.ifBlank { "" }
        val chaveTa = chaveTituloAutor(livro.titulo, autor)
        if (chaveTa in chavesSalvas) return true
        val isbn = livro.isbn
        return !isbn.isNullOrBlank() && chaveIsbn(isbn) in chavesSalvas
    }

    inner class ResultadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgCapa: ImageView = view.findViewById(R.id.imgCapaPesquisa)
        private val placeholder: View = view.findViewById(R.id.imgPlaceholderPesquisa)
        private val titulo: TextView = view.findViewById(R.id.txtTituloPesquisa)
        private val autor: TextView = view.findViewById(R.id.txtAutorPesquisa)
        private val meta: TextView = view.findViewById(R.id.txtMetaPesquisa)
        private val preco: TextView = view.findViewById(R.id.txtPrecoPesquisa)
        private val btnVerLoja: MaterialButton = view.findViewById(R.id.btnVerLojaPesquisa)
        private val btnAddDesejo: MaterialButton = view.findViewById(R.id.btnAddDesejo)

        fun mostrar(livro: LivroPesquisa) {
            val context = itemView.context
            CarregadorCapa.carregar(imgCapa, livro.capaUrl, placeholder, maxLadoPx = 300)
            titulo.text = livro.titulo
            autor.text = livro.autor.ifBlank { context.getString(R.string.autor_desconhecido) }

            val detalhes = buildList {
                livro.paginas?.let { add(context.getString(R.string.paginas_total, it)) }
                livro.isbn?.let { add(context.getString(R.string.info_isbn, it)) }
            }
            if (detalhes.isNotEmpty()) {
                meta.text = detalhes.joinToString(" • ")
                meta.visibility = View.VISIBLE
            } else {
                meta.visibility = View.GONE
            }

            val precoReal = livro.precoTexto?.trim()?.takeIf { it.any { c -> c.isDigit() } }
            if (precoReal != null) {
                preco.text = precoReal
                preco.visibility = View.VISIBLE
            } else {
                preco.text = ""
                preco.visibility = View.GONE
            }

            btnVerLoja.setOnClickListener { aoVerLoja(livro) }

            val salvo = estaSalvo(livro)
            if (salvo) {
                btnAddDesejo.setText(R.string.btn_salvo)
                btnAddDesejo.isEnabled = false
                btnAddDesejo.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.booki_indicador_nav)
                )
                btnAddDesejo.setTextColor(ContextCompat.getColor(context, R.color.booki_primaria))
                btnAddDesejo.setOnClickListener(null)
            } else {
                btnAddDesejo.setText(R.string.btn_add_desejos)
                btnAddDesejo.isEnabled = true
                btnAddDesejo.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.booki_secundaria)
                )
                btnAddDesejo.setTextColor(ContextCompat.getColor(context, R.color.booki_texto))
                btnAddDesejo.setOnClickListener { aoAdicionarDesejo(livro) }
            }
        }
    }

    companion object {
        fun chaveIsbn(isbn: String): String = "isbn:${isbn.trim().uppercase()}"
        fun chaveTituloAutor(titulo: String, autor: String): String =
            "ta:${titulo.trim().lowercase()}|${autor.trim().lowercase()}"
    }
}
