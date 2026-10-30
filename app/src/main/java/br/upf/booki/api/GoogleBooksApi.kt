package br.upf.booki.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Books API (https://developers.google.com/books).
 * Usada para consultar o preço de venda dos livros (campo saleInfo).
 */
interface GoogleBooksApi {

    @GET("books/v1/volumes")
    suspend fun buscar(
        @Query("q") consulta: String,
        @Query("key") chave: String?,
        @Query("country") pais: String = "BR",
        @Query("maxResults") maximo: Int = 10,
        @Query("printType") tipo: String = "books"
    ): VolumesResposta
}

data class VolumesResposta(
    val totalItems: Int?,
    val items: List<Volume>?
)

data class Volume(
    val id: String?,
    val volumeInfo: VolumeInfo?,
    val saleInfo: SaleInfo?
)

data class VolumeInfo(
    val title: String?,
    val authors: List<String>?,
    val categories: List<String>?,
    val pageCount: Int?,
    val infoLink: String?
)

data class SaleInfo(
    /** FOR_SALE, NOT_FOR_SALE, FREE... */
    val saleability: String?,
    val listPrice: Preco?,
    val retailPrice: Preco?,
    val buyLink: String?
)

data class Preco(
    val amount: Double?,
    val currencyCode: String?
)
