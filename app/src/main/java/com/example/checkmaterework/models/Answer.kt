package com.example.checkmaterework.models

sealed class Answer {
    data class MultipleChoice(val questionNumber: Int, val selectedAnswer: String) : Answer()
    data class Identification(val questionNumber: Int, val answer: String) : Answer()
    data class WordProblemAnswer(
        val questionNumber: Int,
        val asked: String,
        val given: String,
        val operation: String,
        val numberSentence: String,
        val solutionAnswer: String
    ) : Answer()
}


