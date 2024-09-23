package com.example.checkmaterework.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImageCaptureViewModel(private val dao: ImageCaptureDAO) : ViewModel() {

    private val _imageCaptureList = MutableLiveData<MutableList<ImageCaptureEntity>>()
    val imageCaptureList: LiveData<MutableList<ImageCaptureEntity>> get() = _imageCaptureList

    init {
        // Load all image captures when the ViewModel is created
        viewModelScope.launch {
            _imageCaptureList.value = dao.getAllImageCaptures().toMutableList()
        }
    }

    // Function to insert a new image capture record into the database
    fun insertImageCapture(imageCapture: ImageCaptureEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertImageCapture(imageCapture)
            val currentImages = _imageCaptureList.value ?: mutableListOf()
            currentImages.add(imageCapture)
            _imageCaptureList.postValue(currentImages)  // Use postValue for background thread updates
        }
    }

    // Function to update an existing image capture record
    fun updateImageCapture(imageCapture: ImageCaptureEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateImageCapture(imageCapture)
            val currentImages = _imageCaptureList.value
            currentImages?.let {
                val index = it.indexOfFirst { img -> img.id == imageCapture.id }
                if (index != -1) {
                    it[index] = imageCapture
                    _imageCaptureList.postValue(it)
                }
            }
        }
    }

    // Function to delete an image capture record from the database
    fun deleteImageCapture(imageCapture: ImageCaptureEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteImageCapture(imageCapture)
            val currentImages = _imageCaptureList.value
            currentImages?.let {
                it.remove(imageCapture)
                _imageCaptureList.postValue(it)
            }
        }
    }
}
