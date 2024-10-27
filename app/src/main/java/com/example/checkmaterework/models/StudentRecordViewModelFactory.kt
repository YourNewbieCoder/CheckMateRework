package com.example.checkmaterework.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class StudentRecordViewModelFactory(
    private val studentRecordDao: StudentRecordDAO,
    private val studentDao: StudentDAO

) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentRecordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentRecordViewModel(studentRecordDao, studentDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}