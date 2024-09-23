package com.example.checkmaterework.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ImageCaptureViewModelFactory(private val imageCaptureDAO: ImageCaptureDAO) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageCaptureViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImageCaptureViewModel(imageCaptureDAO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}