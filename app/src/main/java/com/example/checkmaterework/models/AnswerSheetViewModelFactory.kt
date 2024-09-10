package com.example.checkmaterework.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AnswerSheetViewModelFactory(private val dao: AnswerSheetDAO): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnswerSheetViewModel::class.java)) {
            @Suppress("unchecked_cast")
            return AnswerSheetViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}