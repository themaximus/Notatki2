package com.example.notatki2.data.database // Убедитесь, что это ваш правильный пакет

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID // Для генерации уникальных ID

@Entity(
    tableName = "notes", // Название таблицы в базе данных
    foreignKeys = [ForeignKey(
        entity = BoardEntity::class,      // Связь с таблицей boards
        parentColumns = ["id"],           // Поле id в BoardEntity
        childColumns = ["boardId"],       // Поле boardId в NoteEntity
        onDelete = ForeignKey.CASCADE     // Если доска удаляется, удалить все связанные с ней заметки
    )],
    indices = [Index(value = ["boardId"])] // Индекс для ускорения запросов по boardId
)
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // Уникальный ID заметки
    val boardId: String, // ID доски, к которой принадлежит заметка
    var content: String, // Содержимое заметки
    var createdAt: Long = System.currentTimeMillis(), // Время создания/обновления заметки
    var isDone: Boolean = false, // Статус выполнения (если нужно, как в вашем HTML)
    var priority: String = "medium", // Приоритет, как в вашем HTML (low, medium, high)
    var orderInList: Int = 0 // Порядок заметки в списке (если нужно для сортировки)
    // Вы можете добавить другие поля для заметки, если они понадобятся
)
