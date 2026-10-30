package br.upf.booki.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Usuário do app. A senha nunca é guardada em texto puro, só o hash. */
@Entity(tableName = "usuarios", indices = [Index(value = ["email"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val email: String,
    val senhaHash: String
)
