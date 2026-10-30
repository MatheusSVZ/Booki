package br.upf.booki.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open Library Search API (https://openlibrary.org/dev/docs/api/search).
 * Não precisa de chave. Usada para preencher o cadastro pelo ISBN
 * e para buscar livros por autor ou assunto (recomendações).
 */
interface OpenLibraryApi {

    @GET("search.json")
    suspend fun buscar(
        @Query("isbn") isbn: String? = null,
        @Query("author") autor: String? = null,
        @Query("subject") assunto: String? = null,
        @Query("q") texto: String? = null,
        @Query("sort") ordem: String? = null,
        @Query("limit") limite: Int = 10,
        @Query("fields") campos: String = CAMPOS
    ): OpenLibraryResposta

    companion object {
        const val CAMPOS = "key,title,author_name,isbn,subject,number_of_pages_median,first_publish_year,ratings_average"
    }
}

data class OpenLibraryResposta(
    val numFound: Int?,
    val docs: List<OpenLibraryLivro>?
)

data class OpenLibraryLivro(
    val key: String?,
    val title: String?,
    @SerializedName("author_name") val autores: List<String>?,
    val isbn: List<String>?,
    @SerializedName("subject") val assuntos: List<String>?,
    @SerializedName("number_of_pages_median") val paginas: Int?,
    @SerializedName("first_publish_year") val anoPublicacao: Int?,
    @SerializedName("ratings_average") val mediaAvaliacoes: Double?
)
