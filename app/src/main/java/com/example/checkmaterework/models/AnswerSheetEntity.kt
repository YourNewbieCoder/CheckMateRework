package com.example.checkmaterework.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "answer_sheets")
data class AnswerSheetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val items: Int,
    val examTypesList: List<Pair<String, Int>>
) {
}