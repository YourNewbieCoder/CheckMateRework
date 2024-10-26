package com.example.checkmaterework.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface StudentRecordDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentRecord(studentRecord: StudentRecordEntity)
}
