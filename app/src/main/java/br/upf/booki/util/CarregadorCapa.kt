package br.upf.booki.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object CarregadorCapa {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val cache: LruCache<String, Bitmap> by lazy {
        val memoriaMaxKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        val tamanhoCacheKb = (memoriaMaxKb / 8).coerceIn(4 * 1024, 16 * 1024)
        object : LruCache<String, Bitmap>(tamanhoCacheKb) {
            override fun sizeOf(key: String, value: Bitmap): Int =
                (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    fun carregar(
        imageView: ImageView,
        origem: String?,
        placeholder: View? = null,
        maxLadoPx: Int = 600
    ) {
        val chave = origem?.trim()?.ifBlank { null }
        imageView.tag = chave

        if (chave == null) {
            mostrarFallback(imageView, placeholder)
            return
        }

        val emCache = cache.get(chave)
        if (emCache != null && !emCache.isRecycled) {
            aplicarBitmap(imageView, placeholder, emCache)
            return
        }

        mostrarFallback(imageView, placeholder)
        escopo.launch {
            val bitmap = withContext(Dispatchers.IO) {
                carregarBitmap(chave, maxLadoPx)
            }
            if (imageView.tag != chave) return@launch
            if (bitmap != null) {
                cache.put(chave, bitmap)
                aplicarBitmap(imageView, placeholder, bitmap)
            } else {
                mostrarFallback(imageView, placeholder)
            }
        }
    }

    fun criarArquivoTemporarioCamera(context: Context): File {
        val pasta = File(context.cacheDir, "camera").apply { mkdirs() }
        return File(pasta, "foto_${System.currentTimeMillis()}.jpg")
    }

    suspend fun salvarCapaLocal(context: Context, uri: Uri, maxLarguraPx: Int = 800): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val opcoesLimites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcoesLimites) }
                if (opcoesLimites.outWidth <= 0 || opcoesLimites.outHeight <= 0) return@runCatching null

                val opcoesDecode = BitmapFactory.Options().apply {
                    inSampleSize = calcularSampleSize(opcoesLimites.outWidth, opcoesLimites.outHeight, maxLarguraPx)
                }
                val decodificado = resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opcoesDecode)
                } ?: return@runCatching null

                val rotacao = lerRotacaoExif(context, uri)
                val orientado = if (rotacao != 0) rotacionar(decodificado, rotacao) else decodificado
                if (orientado !== decodificado) decodificado.recycle()

                val finalBitmap = escalarSeNecessario(orientado, maxLarguraPx)
                if (finalBitmap !== orientado) orientado.recycle()

                val pastaCapas = File(context.filesDir, "capas").apply { mkdirs() }
                val destino = File(pastaCapas, "capa_${System.currentTimeMillis()}.jpg")
                FileOutputStream(destino).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                cache.put(destino.absolutePath, finalBitmap)
                destino.absolutePath
            }.getOrNull()
        }

    private fun mostrarFallback(imageView: ImageView, placeholder: View?) {
        imageView.setImageDrawable(null)
        imageView.visibility = View.GONE
        placeholder?.visibility = View.VISIBLE
    }

    private fun aplicarBitmap(imageView: ImageView, placeholder: View?, bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
        imageView.visibility = View.VISIBLE
        placeholder?.visibility = View.GONE
    }

    private fun carregarBitmap(origem: String, maxLadoPx: Int): Bitmap? = runCatching {
        if (origem.startsWith("http://", ignoreCase = true) || origem.startsWith("https://", ignoreCase = true)) {
            val urlSegura = origem.replaceFirst("http://", "https://", ignoreCase = true)
            val conexao = (URL(urlSegura).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            try {
                if (conexao.responseCode !in 200..299) return@runCatching null
                val bytes = conexao.inputStream.use { it.readBytes() }
                decodificarBytes(bytes, maxLadoPx)
            } finally {
                conexao.disconnect()
            }
        } else {
            val arquivo = File(origem)
            if (!arquivo.exists() || !arquivo.isFile) return@runCatching null
            decodificarArquivo(arquivo, maxLadoPx)
        }
    }.getOrNull()

    private fun decodificarBytes(bytes: ByteArray, maxLadoPx: Int): Bitmap? {
        if (bytes.isEmpty()) return null
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        val opcoes = BitmapFactory.Options().apply {
            inSampleSize = calcularSampleSize(limites.outWidth, limites.outHeight, maxLadoPx)
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opcoes) ?: return null
        val escalado = escalarSeNecessario(bitmap, maxLadoPx)
        if (escalado !== bitmap) bitmap.recycle()
        return escalado
    }

    private fun decodificarArquivo(arquivo: File, maxLadoPx: Int): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(arquivo.absolutePath, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        val opcoes = BitmapFactory.Options().apply {
            inSampleSize = calcularSampleSize(limites.outWidth, limites.outHeight, maxLadoPx)
        }
        val bitmap = BitmapFactory.decodeFile(arquivo.absolutePath, opcoes) ?: return null
        val escalado = escalarSeNecessario(bitmap, maxLadoPx)
        if (escalado !== bitmap) bitmap.recycle()
        return escalado
    }

    private fun calcularSampleSize(largura: Int, altura: Int, maxLadoPx: Int): Int {
        var sample = 1
        while ((largura / sample) > maxLadoPx * 2 || (altura / sample) > maxLadoPx * 2) {
            sample *= 2
        }
        return sample
    }

    private fun escalarSeNecessario(bitmap: Bitmap, maxLadoPx: Int): Bitmap {
        val maiorLado = maxOf(bitmap.width, bitmap.height)
        if (maiorLado <= maxLadoPx) return bitmap
        val fator = maxLadoPx.toFloat() / maiorLado.toFloat()
        val novaLargura = (bitmap.width * fator).toInt().coerceAtLeast(1)
        val novaAltura = (bitmap.height * fator).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, novaLargura, novaAltura, true)
    }

    private fun lerRotacaoExif(context: Context, uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)

    private fun rotacionar(bitmap: Bitmap, graus: Int): Bitmap {
        val matriz = Matrix().apply { postRotate(graus.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }
}
