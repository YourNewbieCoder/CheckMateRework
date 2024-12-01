package com.example.checkmaterework.models

data class ParsedAnswer(
    val questionNumber: Int? = null,  // For multiple-choice or identification type questions
    val answer: String,               // The answer or part of the answer
    val asked: String? = null,        // For word problems
    val given: String? = null,        // For word problems
    val operation: String? = null,    // For word problems
    val numberSentence: String? = null, // For word problems
    val solution: String? = null      // For word problems
)
