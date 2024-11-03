package com.example.checkmaterework.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentRecordDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentRecord(studentRecord: StudentRecordEntity)

    @Query("SELECT * FROM student_records WHERE classId = :classId")
    suspend fun getRecordsByClassId(classId: Int): List<StudentRecordEntity>

    @Query("SELECT * FROM student_records WHERE classId = :classId AND answerSheetId = :answerSheetId")
    suspend fun getRecordsByClassAndAnswerSheet(classId: Int, answerSheetId: Int): List<StudentRecordEntity>

    @Query("SELECT * FROM student_records WHERE recordId = :recordId")
    suspend fun getRecordById(recordId: Int): StudentRecordEntity?
}
