package com.example.notatki2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.notatki2.data.database.AppDatabase
import com.example.notatki2.data.database.BoardEntity
import com.example.notatki2.data.database.NoteEntity
import com.example.notatki2.data.repository.NotatkiRepository
import kotlinx.coroutines.launch

class NotatkiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotatkiRepository
    val allBoards: LiveData<List<BoardEntity>> // Будет инициализировано в init
    private val _selectedBoardNotes = MutableLiveData<List<NoteEntity>>() // Для заметок выбранной доски

    // LiveData для текущей выбранной доски (если нужно)
    private val _currentSelectedBoard = MutableLiveData<BoardEntity?>()
    val currentSelectedBoard: LiveData<BoardEntity?> get() = _currentSelectedBoard

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        val boardDao = AppDatabase.getDatabase(application).boardDao()
        repository = NotatkiRepository(noteDao, boardDao) // Проверь порядок, если ошибка Argument type mismatch останется
        allBoards = repository.getAllBoardsLiveData() // Используем правильный метод
    }

    // Board operations
    fun insertBoard(board: BoardEntity) = viewModelScope.launch {
        repository.insertBoard(board)
    }

    fun updateBoard(board: BoardEntity) = viewModelScope.launch {
        repository.updateBoard(board)
    }

    fun deleteBoard(board: BoardEntity) = viewModelScope.launch {
        repository.deleteBoard(board)
    }

    suspend fun getBoardById(boardId: String): BoardEntity? {
        return repository.getBoardById(boardId)
    }

    // Этот метод может быть полезен, если нужно получить доску для виджета по ID в ViewModel
    // Но для самого виджета лучше использовать getBoardById напрямую в RemoteViewsFactory, если это возможно
    // Либо передавать ID доски в RemoteViewsFactory и там загружать.
    // Если этот метод нужен для UI, то он здесь.
    fun loadBoardForWidget(boardId: String) = viewModelScope.launch {
        // _currentSelectedBoard.value = repository.getBoardById(boardId) // Пример, если нужно обновить LiveData
    }


    /**
     * Получает список всех досок из репозитория (не LiveData).
     * Может быть использован для задач, где не нужна реактивность LiveData.
     */
    suspend fun getBoardsListFromRepository(): List<BoardEntity> {
        return repository.getAllBoards() // Используем правильный метод
    }

    // Note operations
    // Этот метод возвращает LiveData и подходит для наблюдения из UI
    fun getNotesForBoard(boardId: String): LiveData<List<NoteEntity>> {
        return repository.getNotesForBoardLiveData(boardId) // Используем правильный метод
    }

    // Этот метод возвращает List и подходит для операций, где не нужна LiveData (например, для виджета, если бы ViewModel его готовила)
    suspend fun getNotesListForBoard(boardId: String): List<NoteEntity> {
        return repository.getNotesForBoardList(boardId) // Используем правильный метод
    }


    fun insertNote(note: NoteEntity) = viewModelScope.launch {
        repository.insertNote(note)
    }

    fun updateNote(note: NoteEntity) = viewModelScope.launch {
        repository.updateNote(note)
    }

    fun deleteNote(note: NoteEntity) = viewModelScope.launch {
        repository.deleteNote(note)
    }

    suspend fun getNoteById(noteId: String): NoteEntity? {
        return repository.getNoteById(noteId)
    }

    // Пример метода для загрузки заметок для виджета, если бы это делалось через ViewModel
    // Обычно виджет загружает данные самостоятельно через RemoteViewsFactory
    suspend fun getNotesForWidget(boardId: String): List<NoteEntity> {
        return repository.getNotesForBoardList(boardId) // Используем правильный метод
    }

    fun setCurrentSelectedBoard(board: BoardEntity?) {
        _currentSelectedBoard.value = board
    }

    fun clearCurrentSelectedBoard() {
        _currentSelectedBoard.value = null
    }
}
