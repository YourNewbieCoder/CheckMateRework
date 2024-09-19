package com.example.checkmaterework.models
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ClassViewModel(private val dao: ClassDAO) : ViewModel() {

    // MutableLiveData for the list of classes
    private val _classList = MutableLiveData<MutableList<ClassEntity>> ()
    val classList: LiveData<MutableList<ClassEntity>> get() = _classList

    init {
        // Fetch all classes when the ViewModel is initialized
        viewModelScope.launch {
            _classList.value = dao.getAllClasses().toMutableList()
        }
    }

    // Function to add a new class
    fun createClass(newClass: ClassEntity) {
        viewModelScope.launch {
            dao.insert(newClass)
            val currentClasses = _classList.value ?: mutableListOf()
            currentClasses.add(newClass)
            _classList.value = currentClasses
        }
    }

    // Function to update an existing class
    fun updateClass(updatedClass: ClassEntity) {
        viewModelScope.launch {
            dao.update(updatedClass)
            val currentClasses = _classList.value ?: mutableListOf()
            val index = currentClasses.indexOfFirst { it.id == updatedClass.id } // Use an ID or unique field for comparison
            if (index != -1) {
                currentClasses[index] = updatedClass
                _classList.value = currentClasses
            }
        }
    }

    // Function to delete a class
    fun deleteClass(classEntity: ClassEntity) {
        viewModelScope.launch {
            dao.delete(classEntity)
            val currentClasses = _classList.value ?: mutableListOf()
            currentClasses.remove(classEntity)
            _classList.value = currentClasses
        }
    }
}