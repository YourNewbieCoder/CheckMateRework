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
    suspend fun insert(student: StudentEntity)

    // Get all students from the database
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY studentName ASC")
    suspend fun getStudentsByClass(classId: Int): List<StudentEntity>

    // Fetch all students
    @Query("SELECT * FROM students ORDER BY studentName ASC")
    suspend fun getAllStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Int): StudentEntity?

    @Query("SELECT * FROM students WHERE classId = :classId")
    suspend fun getStudentsByClassId(classId: Int): List<StudentEntity>

    @Update
    suspend fun update(student: StudentEntity)

    @Delete
    suspend fun delete(student: StudentEntity)

    @Insert
    suspend fun insertStudent(student: StudentEntity): Long


}