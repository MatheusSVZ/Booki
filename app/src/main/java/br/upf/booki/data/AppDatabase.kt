package br.upf.booki.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.upf.booki.model.Livro
import br.upf.booki.model.User

/** Banco único do app (singleton): todas as telas usam a mesma instância. */
@Database(entities = [User::class, Livro::class], version = 2, exportSchema = false)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun livroDao(): LivroDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "booki.db"
                )
                    // Projeto acadêmico: ao mudar a versão do banco, recria as tabelas.
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instancia = it }
            }
    }
}
