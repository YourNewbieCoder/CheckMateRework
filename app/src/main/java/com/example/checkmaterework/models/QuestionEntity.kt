package com.example.checkmaterework.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [ForeignKey(
        entity = AnswerSheetEntity::class,
        parentColumns = ["id"],
        childColumns = ["answerSheetId"],
        onDelete = CASCADE
    )],
    indices = [Index(value = ["answerSheetId"])]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val questionId: Int = 0,
    var answerSheetId: Int,   // Foreign key referencing AnswerSheetEntity
    val questionNumber: Int,
    val answer: String,
    val answerType: AnswerType // Add this field to specify the type of answer
)

enum class AnswerType {
    MULTIPLE_CHOICE,
    IDENTIFICATION,
    WORD_PROBLEM
}
