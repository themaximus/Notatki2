package com.example.notatki2.data.database // Убедитесь, что это ваш правильный пакет

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID // Для генерации уникальных ID

@Entity(tableName = "boards") // Название таблицы в базе данных
data class BoardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // Уникальный ID доски, генерируется автоматически
    var name: String, // Название доски
    var createdAt: Long, // <<< ДОБАВЛЕНО ЭТО ПОЛЕ
    var order: Int,      // <<< И ЭТО ПОЛЕ ТОЖЕ ДОБАВЛЕНО
    var isWidgetBoard: Boolean = false // Флаг, указывающий, является ли эта доска специальной для виджета
)
