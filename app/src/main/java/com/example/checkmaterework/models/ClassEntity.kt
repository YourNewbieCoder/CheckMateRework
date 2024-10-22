package com.example.checkmaterework.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val classId: Int = 0,
    val className: String
)