package com.example.checkmaterework.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AnswerSheetDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answerSheet: AnswerSheetEntity)

    @Update
    suspend fun update(answerSheet: AnswerSheetEntity)

    @Delete
    suspend fun delete(answerSheet: AnswerSheetEntity)

    @Query("SELECT * FROM answer_sheets")
    suspend fun getAllAnswerSheets(): List<AnswerSheetEntity>

    @Query("SELECT * FROM answer_sheets WHERE id = :id")
    suspend fun getAnswerSheetById(id: Int): AnswerSheetEntity?
}