package br.upf.booki.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.upf.booki.model.User

@Dao
interface UserDao {

    /** Devolve o id gerado pelo Room (autoGenerate). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(user: User): Long

    /** null quando não existe usuário com esse e-mail. */
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): User?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): User?
}
