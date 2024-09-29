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
    suspend fun insert(classEntity: ClassEntity)

    @Update
    suspend fun update(classEntity: ClassEntity)

    @Delete
    suspend fun delete(classEntity: ClassEntity)

    @Query("SELECT * FROM classes_table")
    suspend fun getAllClasses(): List<ClassEntity>

    @Query("SELECT * FROM classes_table WHERE id = :id")
    suspend fun getClassById(id: Int): ClassEntity?

    @Query("SELECT * FROM classes_table WHERE className = :className LIMIT 1")
    suspend fun getClassByName(className: String): ClassEntity?

    @Insert
    suspend fun insertClass(classEntity: ClassEntity): Long
}