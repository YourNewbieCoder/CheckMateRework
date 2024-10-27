package com.example.checkmaterework.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface StudentDAO {

    // Insert a new student into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addStudent(student: StudentEntity)

    // Get all students from the database
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY studentName ASC")
    suspend fun getStudentsByClass(classId: Int): List<StudentEntity>

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

//    // Fetch all students
//    @Query("SELECT * FROM students ORDER BY studentName ASC")
//    suspend fun getAllStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE studentId = :id")
    suspend fun getStudentById(id: Int): StudentEntity?

//    @Update
//    suspend fun updateStudent(student: StudentEntity)

    @Insert
    suspend fun insertStudent(student: StudentEntity): Long

    @Query("SELECT * FROM students WHERE studentName = :studentName AND classId = :classId LIMIT 1")
    suspend fun getStudentByNameAndClass(studentName: String, classId: Int): StudentEntity?

    @Query("SELECT * FROM students WHERE studentId IN (:studentIds)")
    suspend fun getStudentNamesByIds(studentIds: List<Int>): List<StudentEntity>

}