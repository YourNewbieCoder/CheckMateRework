package com.example.checkmaterework.models

data class ViewAnalysisItem(
    val question: String,
    val correctCount: Int,
    val incorrectCount: Int,
    val isMostCorrect: Boolean = false,
    val isLeastCorrect: Boolean = false
)
