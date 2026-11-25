package br.upf.booki.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "desejos",
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
data class ItemDesejo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val usuarioId: Int,
    val titulo: String,
    val autor: String,
    val isbn: String? = null,
    val precoTexto: String? = null,
    val linkLoja: String? = null,
    val dataAdicao: LocalDate = LocalDate.now(),
    val capaUrl: String? = null
)
