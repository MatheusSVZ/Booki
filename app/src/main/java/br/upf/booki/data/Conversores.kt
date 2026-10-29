package br.upf.booki.data

import androidx.room.TypeConverter
import br.upf.booki.model.StatusLeitura
import java.time.LocalDate

/** Ensina o Room a gravar datas (como número de dias) e o status (como texto). */
class Conversores {
    @TypeConverter
    fun dataParaNumero(data: LocalDate?): Long? = data?.toEpochDay()

    @TypeConverter
    fun numeroParaData(dias: Long?): LocalDate? = dias?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun statusParaTexto(status: StatusLeitura): String = status.name

    @TypeConverter
    fun textoParaStatus(texto: String): StatusLeitura = StatusLeitura.valueOf(texto)
}
