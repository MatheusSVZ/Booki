package br.upf.booki.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun criar(urlBase: String): Retrofit = Retrofit.Builder()
        .baseUrl(urlBase)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val googleBooks: GoogleBooksApi by lazy {
        criar("https://www.googleapis.com/").create(GoogleBooksApi::class.java)
    }

    val openLibrary: OpenLibraryApi by lazy {
        criar("https://openlibrary.org/").create(OpenLibraryApi::class.java)
    }
}
