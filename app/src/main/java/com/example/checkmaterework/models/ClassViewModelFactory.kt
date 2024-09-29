package com.example.checkmaterework.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ClassViewModelFactory(private val dao: ClassDAO): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassViewModel::class.java)) {
            @Suppress("unchecked_cast")
            return ClassViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}