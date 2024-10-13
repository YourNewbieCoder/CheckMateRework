package com.example.checkmaterework.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AnswerKeyViewModelFactory(private val dao: AnswerKeyDao): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnswerKeyViewModel::class.java)) {
            @Suppress("unchecked_cast")
            return AnswerKeyViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
