package com.example.checkmaterework.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AnswerSheetViewModel: ViewModel() {
    var sheetName = MutableLiveData<String>()
    var numberOfItems = MutableLiveData<String>()
}