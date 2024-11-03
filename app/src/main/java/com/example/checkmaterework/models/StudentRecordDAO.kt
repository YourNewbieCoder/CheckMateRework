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

    @Query("""
    SELECT sr.* 
    FROM student_records sr 
    INNER JOIN students s ON sr.studentId = s.studentId
    WHERE sr.classId = :classId AND sr.answerSheetId = :answerSheetId 
    ORDER BY s.studentName ASC
""")
    suspend fun getRecordsByClassAndAnswerSheet(classId: Int, answerSheetId: Int): List<StudentRecordEntity>

    @Query("SELECT * FROM student_records WHERE recordId = :recordId")
    suspend fun getRecordById(recordId: Int): StudentRecordEntity?
}
