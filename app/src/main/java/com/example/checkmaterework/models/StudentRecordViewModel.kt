package com.example.checkmaterework.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class StudentRecordViewModel(
    private val studentRecordDAO: StudentRecordDAO,
    private val studentDAO: StudentDAO
) : ViewModel() {

    private val _studentRecordList = MutableLiveData<List<StudentRecordEntity>>()
    val studentRecordList: LiveData<List<StudentRecordEntity>> get() = _studentRecordList

    private val _studentNamesMap = MutableLiveData<Map<Int, String>>()
    val studentNamesMap: LiveData<Map<Int, String>> get() = _studentNamesMap

    fun getRecordsByClassId(classId: Int) {
        viewModelScope.launch {
            val records = studentRecordDAO.getRecordsByClassId(classId)
            _studentRecordList.postValue(records)  // Update LiveData with the result on the main thread

            // Fetch names for each studentId in the records
            val studentIds = records.map { it.studentId }
            val namesMap = studentDAO.getStudentNamesByIds(studentIds).associate { it.studentId to it.studentName }
            _studentNamesMap.postValue(namesMap)
        }
    }

    fun getRecordsByClassAndAnswerSheet(classId: Int, answerSheetId: Int) {
        viewModelScope.launch {
            val records = studentRecordDAO.getRecordsByClassAndAnswerSheet(classId, answerSheetId)
            _studentRecordList.postValue(records)

            // Fetch names for each studentId in the records
            val studentIds = records.map { it.studentId }
            val namesMap = studentDAO.getStudentNamesByIds(studentIds).associate { it.studentId to it.studentName }
            _studentNamesMap.postValue(namesMap)
        }
    }

    private val _studentRecord = MutableLiveData<StudentRecordEntity?>()
    val studentRecord: LiveData<StudentRecordEntity?> get() = _studentRecord

    fun getRecordById(recordId: Int) {
        viewModelScope.launch {
            val record = studentRecordDAO.getRecordById(recordId)
            _studentRecord.postValue(record)
        }
    }

}