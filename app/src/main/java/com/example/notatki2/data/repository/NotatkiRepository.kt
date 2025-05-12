package com.example.notatki2.data.repository

import androidx.lifecycle.LiveData
import com.example.notatki2.data.database.BoardDao
import com.example.notatki2.data.database.BoardEntity
import com.example.notatki2.data.database.NoteDao
import com.example.notatki2.data.database.NoteEntity

class NotatkiRepository(private val noteDao: NoteDao, private val boardDao: BoardDao) {

    // Board operations
    suspend fun insertBoard(board: BoardEntity) {
        boardDao.insertBoard(board)
    }

    suspend fun updateBoard(board: BoardEntity) {
        boardDao.updateBoard(board)
    }

    suspend fun deleteBoard(board: BoardEntity) {
        // Consider deleting all notes associated with this board as well
        noteDao.deleteNotesByBoardId(board.id)
        boardDao.deleteBoard(board)
    }

    suspend fun getBoardById(boardId: String): BoardEntity? = boardDao.getBoardById(boardId)

    // Возвращает LiveData списка досок, используется в ViewModel
    fun getAllBoardsLiveData(): LiveData<List<BoardEntity>> {
        return boardDao.getAllBoardsLiveData()
    }

    // Возвращает обычный список досок, используется для задач, где LiveData не нужна (например, конфигурация виджета)
    suspend fun getAllBoards(): List<BoardEntity> {
        return boardDao.getAllBoardsList()
    }

    // Note operations
    suspend fun insertNote(note: NoteEntity) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }

    // This returns LiveData, suitable for observing in UI controllers
    fun getNotesForBoardLiveData(boardId: String): LiveData<List<NoteEntity>> {
        return noteDao.getNotesForBoard(boardId) // Предполагается, что NoteDao.getNotesForBoard возвращает LiveData
    }

    // This is a suspend function that directly returns a List, suitable for background tasks/widgets
    suspend fun getNotesForBoardList(boardId: String): List<NoteEntity> {
        return noteDao.getNotesForBoardList(boardId) // Предполагается, что NoteDao.getNotesForBoardList возвращает List
    }

    suspend fun getNoteById(noteId: String): NoteEntity? {
        return noteDao.getNoteById(noteId)
    }

    suspend fun deleteNotesByBoardId(boardId: String){
        noteDao.deleteNotesByBoardId(boardId)
    }
}
