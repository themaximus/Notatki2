package com.example.notatki2.data.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface BoardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: BoardEntity)

    @Update
    suspend fun updateBoard(board: BoardEntity)

    @Delete
    suspend fun deleteBoard(board: BoardEntity)

    @Query("SELECT * FROM boards WHERE id = :boardId")
    suspend fun getBoardById(boardId: String): BoardEntity?

    // Метод для получения LiveData списка досок (для ViewModel)
    @Query("SELECT * FROM boards ORDER BY name ASC")
    fun getAllBoardsLiveData(): LiveData<List<BoardEntity>>

    // Новый метод для получения обычного списка досок (для фоновых задач, конфигурации виджета)
    @Query("SELECT * FROM boards ORDER BY name ASC")
    suspend fun getAllBoardsList(): List<BoardEntity>
}
