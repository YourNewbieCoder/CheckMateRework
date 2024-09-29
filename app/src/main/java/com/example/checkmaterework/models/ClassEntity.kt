package com.example.checkmaterework.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes_table")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val className: String
) {
}