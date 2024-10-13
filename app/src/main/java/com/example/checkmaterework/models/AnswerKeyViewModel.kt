package com.example.checkmaterework.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnswerKeyViewModel(private val answerKeyDao: AnswerKeyDao) : ViewModel() {

    fun saveAnswersToDatabase(answerSheetId: Int, answers: List<Answer>) {
        viewModelScope.launch(Dispatchers.IO) {
            // Create a list to hold all QuestionEntity objects
            val questionEntities = answers.map { answer ->
                QuestionEntity(
                    answerSheetId = answerSheetId,
                    questionNumber = answer.questionNumber,
                    answer = answer.answer
                )
            }
            answerKeyDao.insertQuestions(questionEntities)
        }
    }
}