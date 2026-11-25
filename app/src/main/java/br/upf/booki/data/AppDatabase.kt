package br.upf.booki.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.upf.booki.model.ItemDesejo
import br.upf.booki.model.Livro
import br.upf.booki.model.User

@Database(entities = [User::class, Livro::class, ItemDesejo::class], version = 4, exportSchema = false)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun livroDao(): LivroDao

    abstract fun itemDesejoDao(): ItemDesejoDao

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
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instancia = it }
            }
    }
}
