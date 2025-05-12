package com.example.notatki2.data.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE boardId = :boardId ORDER BY orderInList ASC")
    fun getNotesForBoard(boardId: String): LiveData<List<NoteEntity>> // For ViewModel

    @Query("SELECT * FROM notes WHERE boardId = :boardId ORDER BY orderInList ASC")
    suspend fun getNotesForBoardList(boardId: String): List<NoteEntity> // For Widget/Background tasks

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Query("DELETE FROM notes WHERE boardId = :boardId")
    suspend fun deleteNotesByBoardId(boardId: String)

    @Query("SELECT * FROM notes")
    fun getAllNotesLiveData(): LiveData<List<NoteEntity>> // Example: if you need all notes
}
