package com.example.checkmaterework.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class StudentViewModelFactory(
    private val studentDao: StudentDAO,
    private val classDao: ClassDAO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentViewModel(studentDao, classDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}