package com.example.checkmaterework.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AnswerSheetViewModel: ViewModel() {
    private val _createdSheetList = MutableLiveData<MutableList<AnswerSheet>>()
    val createdSheetList: LiveData<MutableList<AnswerSheet>> get() =_createdSheetList

    init {
        _createdSheetList.value = mutableListOf()
    }

    // Accepts an AnswerSheet object
    fun createSheet(sheet: AnswerSheet) {
        val currentSheets = _createdSheetList.value ?: mutableListOf()
        currentSheets.add(sheet)
        _createdSheetList.value = currentSheets
    }
}