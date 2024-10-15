package com.example.checkmaterework.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AnswerKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAnswerSheet(answerSheet: AnswerSheetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertQuestions(questions: List<QuestionEntity>)

//    // Add a query to retrieve the questions for a specific answerSheetId
    @Query("SELECT * FROM questions WHERE answerSheetId = :answerSheetId")
    fun getQuestionsByAnswerSheetId(answerSheetId: Int): List<QuestionEntity>

    @Transaction
    fun insertAnswerSheetWithQuestions(answerSheet: AnswerSheetEntity, questions: List<QuestionEntity>) {
        val answerSheetId = insertAnswerSheet(answerSheet)
        questions.forEach { it.answerSheetId = answerSheetId.toInt() }
        insertQuestions(questions)
    }

    @Query("DELETE FROM questions") // Replace `question_table` with the actual name of your table
    fun clearAll()
    fun clearQuestionsByAnswerSheetId(answerSheetId: Int) {
    }
}
