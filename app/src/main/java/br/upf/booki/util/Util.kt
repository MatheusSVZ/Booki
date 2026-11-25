package br.upf.booki.util

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import br.upf.booki.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

class Util {
    companion object {

        private val FORMATO_DATA: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

        fun formatarTituloBooki(context: Context): CharSequence {
            val texto = context.getString(R.string.app_name)
            val spannable = SpannableStringBuilder(texto)
            if (texto.endsWith("!")) {
                val corLaranja = ContextCompat.getColor(context, R.color.booki_secundaria)
                spannable.setSpan(
                    ForegroundColorSpan(corLaranja),
                    texto.length - 1,
                    texto.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
        }

        fun dialogoAlerta(context: Context, titulo: String, mensagem: String) {
            AlertDialog.Builder(context)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton(R.string.ok, null)
                .show()
        }

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

        fun textoParaData(texto: String): LocalDate? = try {
            LocalDate.parse(texto.trim(), FORMATO_DATA)
        } catch (e: DateTimeParseException) {
            null
        }

        fun dataParaTexto(data: LocalDate?): String = data?.format(FORMATO_DATA) ?: ""

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
