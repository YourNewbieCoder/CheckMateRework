package com.example.checkmaterework.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnswerKeyViewModel(private val answerKeyDao: AnswerKeyDao) : ViewModel() {
    // MutableLiveData to hold a list of QuestionEntities for each answer sheet
    private val _savedAnswerKeys = MutableLiveData<List<QuestionEntity>>()
    val savedAnswerKeys: LiveData<List<QuestionEntity>> get() = _savedAnswerKeys

    // Fetches and loads the questions for a specific answer sheet
    fun loadAnswerKeysForSheet(answerSheetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val questions = answerKeyDao.getQuestionsByAnswerSheetId(answerSheetId)

//            Log.d("AnswerKeyViewModel", "Questions loaded: $questions")
            Log.d("AnswerKeyViewModel", "Loading questions for AnswerSheet ID: $answerSheetId")
            Log.d("AnswerKeyViewModel", "Retrieved questions: ${questions.joinToString("\n")}")

            _savedAnswerKeys.postValue(questions)
        }
    }

    fun saveAnswersToDatabase(answerSheetId: Int, answers: List<Answer>) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear all questions before inserting new ones (if necessary)
//            answerKeyDao.clearAll() // Optional, only if you want to start fresh

            // Clear all questions for the specific answer sheet if necessary (optional)
            answerKeyDao.clearQuestionsByAnswerSheetId(answerSheetId)

            // Create a list to hold all QuestionEntity objects
            val questionEntities = answers.map { answer ->
                when(answer) {
                    is Answer.MultipleChoice -> QuestionEntity(
                        answerSheetId = answerSheetId,
                        questionNumber = answer.questionNumber,
                        answer = answer.selectedAnswer,
                        answerType = AnswerType.MULTIPLE_CHOICE // Set the type here
                    )
                    is Answer.Identification -> QuestionEntity(
                        answerSheetId = answerSheetId,
                        questionNumber = answer.questionNumber,
                        answer = answer.answer,
                        answerType = AnswerType.IDENTIFICATION // Set the type here
                    )
                    is Answer.WordProblemAnswer -> {
                        // Handle saving all fields for word problem
                        QuestionEntity(
                            answerSheetId = answerSheetId,
                            questionNumber = answer.questionNumber,
                            answer = """
                                Asked: ${answer.asked}
                                Given: ${answer.given}
                                Operation: ${answer.operation}
                                Number Sentence: ${answer.numberSentence}
                                Solution: ${answer.solutionAnswer}
                            """.trimIndent(), // You can format it as a multiline string or store them separately if needed
                            answerType = AnswerType.WORD_PROBLEM // Set the type here
                        )
                    }
                }
            }
            answerKeyDao.insertQuestions(questionEntities)
            Log.d("AnswerKeyViewModel", "Inserted questions: $questionEntities") // Log inserted questions
        }
    }

//    fun getAnswersForSheet(answerSheetId: Int) = liveData(Dispatchers.IO) {
//        val questions = answerKeyDao.getQuestionsByAnswerSheetId(answerSheetId)
//        emit(questions)
//    }
}