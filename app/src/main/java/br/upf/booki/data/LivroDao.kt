package br.upf.booki.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.upf.booki.model.Livro
import br.upf.booki.model.StatusLeitura

@Dao
interface LivroDao {

    @Insert
    suspend fun inserir(livro: Livro): Long

    @Update
    suspend fun atualizar(livro: Livro): Int

    @Delete
    suspend fun excluir(livro: Livro): Int

    @Query("SELECT * FROM livros WHERE usuarioId = :usuarioId ORDER BY titulo COLLATE NOCASE")
    suspend fun listar(usuarioId: Int): List<Livro>

    @Query("SELECT * FROM livros WHERE usuarioId = :usuarioId AND status = :status ORDER BY titulo COLLATE NOCASE")
    suspend fun listarPorStatus(usuarioId: Int, status: StatusLeitura): List<Livro>

    /** Sempre filtra pelo usuário, para ninguém abrir o livro de outra conta. */
    @Query("SELECT * FROM livros WHERE id = :id AND usuarioId = :usuarioId LIMIT 1")
    suspend fun buscarPorId(id: Int, usuarioId: Int): Livro?

    @Query("SELECT * FROM livros WHERE usuarioId = :usuarioId AND isbn = :isbn LIMIT 1")
    suspend fun buscarPorIsbn(usuarioId: Int, isbn: String): Livro?

    /** Livros lidos com as maiores notas (base para as recomendações). */
    @Query(
        "SELECT * FROM livros WHERE usuarioId = :usuarioId AND status = 'LIDO' AND nota IS NOT NULL " +
            "ORDER BY nota DESC, dataConclusao DESC LIMIT :limite"
    )
    suspend fun melhoresAvaliados(usuarioId: Int, limite: Int): List<Livro>
}
