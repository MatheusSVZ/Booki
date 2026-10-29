package br.upf.booki.util

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import br.upf.booki.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Funções utilitárias usadas por todas as telas (diálogos, mensagens e datas).
 */
class Util {
    companion object {

        private val FORMATO_DATA: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

        /** Mostra um aviso simples com um botão OK. */
        fun dialogoAlerta(context: Context, titulo: String, mensagem: String) {
            AlertDialog.Builder(context)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton(R.string.ok, null)
                .show()
        }

        /** Pede confirmação; [aoConfirmar] só roda se o usuário tocar em "Sim". */
        fun dialogoConfirma(
            context: Context,
            titulo: String,
            mensagem: String,
            aoConfirmar: () -> Unit
        ) {
            AlertDialog.Builder(context)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton(R.string.sim) { _, _ -> aoConfirmar() }
                .setNegativeButton(R.string.nao, null)
                .show()
        }

        fun mensagem(context: Context, texto: String) {
            Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()
        }

        /** Converte "dd/MM/aaaa" em LocalDate; devolve null se o texto for inválido. */
        fun textoParaData(texto: String): LocalDate? = try {
            LocalDate.parse(texto.trim(), FORMATO_DATA)
        } catch (e: DateTimeParseException) {
            null
        }

        fun dataParaTexto(data: LocalDate?): String = data?.format(FORMATO_DATA) ?: ""

        /**
         * A partir do Android 15 o app desenha atrás das barras do sistema.
         * Empurra o [topo] (normalmente a Toolbar) para baixo da barra de status e,
         * se [ajustarInferior] for true, afasta a [raiz] da barra de navegação e do teclado.
         */
        fun aplicarMargensDoSistema(raiz: View, topo: View = raiz, ajustarInferior: Boolean = true) {
            val topoOriginal = topo.paddingTop
            val inferiorOriginal = raiz.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, insets ->
                val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
                topo.updatePadding(top = topoOriginal + barras.top)
                if (ajustarInferior) {
                    raiz.updatePadding(bottom = inferiorOriginal + maxOf(barras.bottom, teclado.bottom))
                }
                insets
            }
        }
    }
}
