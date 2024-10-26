package com.example.checkmaterework.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ClassDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addClass(newClass: ClassEntity): Long

    @Update
    suspend fun updateClass(classEntity: ClassEntity)

    @Delete
    suspend fun deleteClass(classEntity: ClassEntity)

    @Query("SELECT * FROM classes")
    suspend fun getAllClasses(): List<ClassEntity>

    @Query("SELECT * FROM classes WHERE classId = :classId LIMIT 1")
    suspend fun getClassById(classId: Int): ClassEntity?

    @Query("SELECT * FROM classes WHERE className = :className LIMIT 1")
    suspend fun getClassByName(className: String): ClassEntity?

//    @Insert
//    suspend fun insertClass(classEntity: ClassEntity): Long
}