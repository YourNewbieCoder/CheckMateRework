package com.example.checkmaterework.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AnswerSheetViewModel(private val dao: AnswerSheetDAO): ViewModel() {
    private val _createdSheetList = MutableLiveData<MutableList<AnswerSheetEntity>>()
    val createdSheetList: LiveData<MutableList<AnswerSheetEntity>> get() =_createdSheetList

    init {
        viewModelScope.launch{
            _createdSheetList.value = dao.getAllAnswerSheets().toMutableList()
        }
    }

    // Accepts an AnswerSheet object
    fun createSheet(sheet: AnswerSheetEntity) {
        viewModelScope.launch{
            dao.insert(sheet)
            val currentSheets = _createdSheetList.value ?: mutableListOf()
            currentSheets.add(sheet)
            _createdSheetList.value = currentSheets
        }
    }

    fun updateSheet(updatedSheet: AnswerSheetEntity) {
        viewModelScope.launch {
            dao.update(updatedSheet)
            val currentSheets = _createdSheetList.value ?: mutableListOf()
            val index = currentSheets.indexOfFirst { it.id == updatedSheet.id } // Use name or unique ID for comparison
            if (index != 1) {
                currentSheets[index] = updatedSheet
                _createdSheetList.value = currentSheets
            }
        }
    }

    fun deleteSheet(sheet: AnswerSheetEntity) {
        viewModelScope.launch {
            dao.delete(sheet)
            val currentSheets = _createdSheetList.value ?: mutableListOf()
            currentSheets.remove(sheet)
            _createdSheetList.value = currentSheets
        }
    }
}