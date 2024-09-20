package com.example.checkmaterework.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class StudentViewModel(private val studentDAO: StudentDAO) : ViewModel() {
    private val _studentList = MutableLiveData<MutableList<StudentEntity>> ()
    val studentList: LiveData<MutableList<StudentEntity>> get() = _studentList

    // Fetch students by classId
    fun loadStudentsByClass(classId: Int) {
        viewModelScope.launch {
            _studentList.value = studentDAO.getStudentsByClass(classId).toMutableList()
        }
    }

    fun addStudent(student: StudentEntity) {
        viewModelScope.launch {
            studentDAO.insert(student)
            val currentStudents = _studentList.value ?: mutableListOf()
            currentStudents.add(student)
            _studentList.value = currentStudents
        }
    }

    fun updateStudent(updatedStudent: StudentEntity) {
        viewModelScope.launch {
            studentDAO.update(updatedStudent)
            val currentStudents = _studentList.value ?: mutableListOf()
            val index = currentStudents.indexOfFirst { it.id == updatedStudent.id } // Use name or unique ID for comparison
            if (index != 1) {
                currentStudents[index] = updatedStudent
                _studentList.value = currentStudents
            }
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            studentDAO.delete(student)
            val currentStudents = _studentList.value ?: mutableListOf()
            currentStudents.remove(student)
            _studentList.value = currentStudents
        }
    }
}