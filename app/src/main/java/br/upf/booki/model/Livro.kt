package br.upf.booki.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Livro da estante de um usuário. */
@Entity(
    tableName = "livros",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("usuarioId")]
)
data class Livro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val usuarioId: Int,
    val titulo: String,
    val autor: String,
    val isbn: String? = null,
    val categoria: String? = null,
    val paginas: Int? = null,
    val status: StatusLeitura,
    /** Nota de 0,5 a 5 estrelas; só existe para livros já lidos. */
    val nota: Float? = null,
    val dataInicio: LocalDate? = null,
    val dataConclusao: LocalDate? = null,
    val comentario: String? = null
)
