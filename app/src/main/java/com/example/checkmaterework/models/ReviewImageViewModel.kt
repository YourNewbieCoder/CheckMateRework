package com.example.checkmaterework.models

import androidx.lifecycle.ViewModel

class ReviewImageViewModel(
    private val imageCaptureDAO: ImageCaptureDAO,
    private val studentDAO: StudentDAO,
    private val classDAO: ClassDAO,
    private val answerSheetDAO: AnswerSheetDAO,
    private val studentRecordDAO: StudentRecordDAO // Added DAO for student records
) : ViewModel() {

    // Retrieve image capture data by ID
    suspend fun getImageCaptureById(id: Int): ImageCaptureEntity? {
        return imageCaptureDAO.getImageCaptureById(id)
    }

    // Retrieve student by ID
    suspend fun getStudentById(id: Int): StudentEntity? {
        return studentDAO.getStudentById(id)
    }

    // Retrieve class by ID
    suspend fun getClassById(id: Int): ClassEntity? {
        return classDAO.getClassById(id)
    }

    // Retrieve answer sheet by ID
    suspend fun getAnswerSheetById(id: Int): AnswerSheetEntity? {
    return answerSheetDAO.getAnswerSheetById(id) }

    // Retrieve class by name (section name)
    suspend fun getClassByName(className: String): ClassEntity? {
        return classDAO.getClassByName(className)
    }

    // Insert student and return ID
    suspend fun insertStudent(student: StudentEntity): Long {
        return studentDAO.insertStudent(student)
    }

    // Insert ImageCapture
    suspend fun insertImageCapture(imageCapture: ImageCaptureEntity) {
        imageCaptureDAO.insertImageCapture(imageCapture)
    }

    suspend fun getStudentByNameAndClass(studentName: String, classId: Int): StudentEntity? {
        return studentDAO.getStudentByNameAndClass(studentName, classId)
    }

    suspend fun insertStudentRecord(studentRecord: StudentRecordEntity) {
        studentRecordDAO.insertStudentRecord(studentRecord)
    }
}

