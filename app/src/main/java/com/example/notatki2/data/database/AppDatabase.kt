package com.example.notatki2.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Убедитесь, что пути к вашим Entity классам (BoardEntity, NoteEntity) здесь указаны верно,
// если они находятся в других пакетах.
// import com.example.notatki2.data.model.BoardEntity // Пример, если они в подпапке model
// import com.example.notatki2.data.model.NoteEntity

@Database(entities = [BoardEntity::class, NoteEntity::class], version = 3, exportSchema = false) // ВЕРСИЯ УВЕЛИЧЕНА!
abstract class AppDatabase : RoomDatabase() {

    abstract fun boardDao(): BoardDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notatki_database" // Убедитесь, что это имя вашей базы данных
                )
                    // ЭТА СТРОКА КРИТИЧЕСКИ ВАЖНА ДЛЯ ИСПРАВЛЕНИЯ ОШИБКИ СХЕМЫ:
                    // Она удалит старую базу данных при несовпадении схемы и создаст новую.
                    // ВНИМАНИЕ: Все данные в локальной базе будут удалены при изменении версии!
                    // Для продакшена потребуются полноценные миграции.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
