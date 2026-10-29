package br.upf.booki.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Verifica se o aparelho tem acesso à internet antes de chamar uma API. */
object Conexao {
    fun temInternet(context: Context): Boolean {
        val gerenciador = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val rede = gerenciador.activeNetwork ?: return false
        val recursos = gerenciador.getNetworkCapabilities(rede) ?: return false
        return recursos.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
