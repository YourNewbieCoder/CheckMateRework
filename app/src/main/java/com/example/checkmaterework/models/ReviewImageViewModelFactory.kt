package com.example.checkmaterework.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ReviewImageViewModelFactory(
    private val imageCaptureDAO: ImageCaptureDAO,
    private val studentDAO: StudentDAO,
    private val classDAO: ClassDAO,
    private val answerSheetDAO: AnswerSheetDAO,
    private val studentRecordDAO: StudentRecordDAO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewImageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReviewImageViewModel(imageCaptureDAO, studentDAO, classDAO, answerSheetDAO, studentRecordDAO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}