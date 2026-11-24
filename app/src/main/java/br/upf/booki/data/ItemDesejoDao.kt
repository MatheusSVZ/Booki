package br.upf.booki.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import br.upf.booki.model.ItemDesejo

@Dao
interface ItemDesejoDao {

    @Insert
    suspend fun inserir(item: ItemDesejo): Long

    @Delete
    suspend fun excluir(item: ItemDesejo): Int

    @Query("SELECT * FROM desejos WHERE usuarioId = :usuarioId ORDER BY id DESC")
    suspend fun listar(usuarioId: Int): List<ItemDesejo>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM desejos WHERE usuarioId = :usuarioId AND (" +
            "(:isbn IS NOT NULL AND isbn = :isbn) OR " +
            "(LOWER(titulo) = LOWER(:titulo) AND LOWER(autor) = LOWER(:autor))" +
            "))"
    )
    suspend fun existe(usuarioId: Int, titulo: String, autor: String, isbn: String?): Boolean
}

