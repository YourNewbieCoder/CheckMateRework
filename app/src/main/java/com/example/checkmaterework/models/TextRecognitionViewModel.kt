package com.example.checkmaterework.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TextRecognitionViewModel : ViewModel() {
    val recognizedText: MutableLiveData<String> = MutableLiveData()
}
