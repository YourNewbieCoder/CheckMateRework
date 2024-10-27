package com.example.checkmaterework.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class StudentRecordViewModel(private val studentRecordDAO: StudentRecordDAO) : ViewModel() {
    private val _studentRecordList = MutableLiveData<List<StudentRecordEntity>>()
    val studentRecordList: LiveData<List<StudentRecordEntity>> get() = _studentRecordList

    fun getRecordsByClassId(classId: Int) {
        viewModelScope.launch {
            val records = studentRecordDAO.getRecordsByClassId(classId)
            _studentRecordList.postValue(records)  // Update LiveData with the result on the main thread
        }
    }

}