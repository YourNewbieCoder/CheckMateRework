package com.example.checkmaterework.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class StudentRecordViewModelFactory(private val dao: StudentRecordDAO) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentRecordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentRecordViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}