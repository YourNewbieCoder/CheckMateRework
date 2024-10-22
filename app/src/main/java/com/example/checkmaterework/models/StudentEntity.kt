package com.example.checkmaterework.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("students",
    foreignKeys = [ForeignKey(
        entity = ClassEntity::class,
        parentColumns = arrayOf("classId"),
        childColumns = arrayOf("classId"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["classId"])]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val studentId: Int = 0,
    val studentName: String,
    val classId: Int // Link the student to the class
//    val score: Int,
) {
}