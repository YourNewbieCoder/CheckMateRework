package com.example.checkmaterework.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_records")
data class StudentRecordEntity(
    @PrimaryKey(autoGenerate = true) val recordId: Int = 0,
    val studentId: Int,
    val classId: Int,
    val answerSheetId: Int,
    val score: Int
)
