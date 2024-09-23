package com.example.checkmaterework.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_captures")
data class ImageCaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sheetId: Int,           // Foreign key to AnswerSheetEntity
    val studentId: Int,         // Foreign key to StudentEntity
    val imagePath: String,      // Path to the captured image
    val sectionId: Int,         // Foreign key to ClassEntity
    val score: Int              // Student's score for this particular sheet
)
