package com.example.checkmaterework.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class StudentViewModel(
    private val studentDAO: StudentDAO,
    private val classDao: ClassDAO // Include the ClassDAO
) : ViewModel() {
    private val _studentList = MutableLiveData<MutableList<StudentEntity>> ()
    val studentList: LiveData<MutableList<StudentEntity>> get() = _studentList

    fun getStudentsForClass(classId: Int) {
        // Fetch students specifically for the given class
        viewModelScope.launch {
            _studentList.value = studentDAO.getStudentsByClass(classId).toMutableList()
        }
    }

//    // Get students for a specific class as LiveData
//    fun getStudentsForClass(classId: Int): LiveData<List<StudentEntity>> {
//        val students = MutableLiveData<List<StudentEntity>>()
//        viewModelScope.launch {
//            students.value = studentDAO.getStudentsByClass(classId)
//        }
//        return students
//    }

//    // Fetch students by classId
//    fun loadStudentsByClass(classId: Int) {
//        viewModelScope.launch {
//            _studentList.value = studentDAO.getStudentsByClass(classId).toMutableList()
//        }
//    }
//
//    fun loadAllStudents() {
//        viewModelScope.launch {
//            _studentList.value = studentDAO.getAllStudents().toMutableList()
//        }
//    }

//    fun addStudent(student: StudentEntity) {
//        viewModelScope.launch {
//            studentDAO.insertStudent(student)
//            val currentStudents = _studentList.value ?: mutableListOf()
//            currentStudents.add(student)
//            _studentList.value = currentStudents
//        }
//    }

    fun addStudent(student: StudentEntity) {
        viewModelScope.launch {
            val classExists = classDao.getClassById(student.classId) // Check if class exists
            if (classExists != null) {
                studentDAO.addStudent(student)
//                val currentStudents = _studentList.value ?: mutableListOf()
//                currentStudents.add(student)
//                _studentList.value = currentStudents
                // Re-fetch students after adding
                _studentList.value = studentDAO.getStudentsByClass(student.classId).toMutableList()
            } else {
                // Handle the case where classId does not exist
                Log.e("StudentViewModel", "Class with ID ${student.classId} does not exist.")
            }
        }
    }
//    // Add a new student and refresh the list for the class
//    fun addStudent(student: StudentEntity) {
//        viewModelScope.launch {
//            studentDAO.insertStudent(student)
//            // Re-fetch students after adding
//            _studentList.value = studentDAO.getStudentsByClass(student.classId).toMutableList()
//        }
//    }



//    fun updateStudent(updatedStudent: StudentEntity) {
//        viewModelScope.launch {
//            studentDAO.updateStudent(updatedStudent)
//            val currentStudents = _studentList.value ?: mutableListOf()
//            val index = currentStudents.indexOfFirst { it.studentId == updatedStudent.studentId } // Use name or unique ID for comparison
//            if (index != 1) {
//                currentStudents[index] = updatedStudent
//                _studentList.value = currentStudents
//            }
//        }
//    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            studentDAO.deleteStudent(student)
//            val currentStudents = _studentList.value ?: mutableListOf()
//            currentStudents.remove(student)
            _studentList.value = studentDAO.getStudentsByClass(student.classId).toMutableList()
        }
    }
}