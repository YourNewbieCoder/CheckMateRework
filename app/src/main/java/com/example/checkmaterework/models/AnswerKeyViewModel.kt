package com.example.checkmaterework.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

    // Function to save parsed answers to the database
    fun saveParsedAnswersToDatabase(answerSheetId: Int, parsedAnswers: List<ParsedAnswer>) {
        viewModelScope.launch(Dispatchers.IO) {
            val questionEntities = parsedAnswers.map { parsedAnswer ->
                QuestionEntity(
                    answerSheetId = answerSheetId,
                    questionNumber = parsedAnswer.questionNumber,
                    answer = parsedAnswer.answer,
                    answerType = determineAnswerType(parsedAnswer)
                )
            }

            // Handle Word Problem answers separately
            val wordProblemEntities  = questionEntities.filter { parsedAnswer ->
                parsedAnswer.answer.contains(Regex("""\b(Asked|Given|Operation|Number Sentence|Solution/Answer)\b""", RegexOption.IGNORE_CASE))
            }.flatMap { parsedAnswer ->
                listOf(
                    QuestionEntity(
                        answerSheetId = answerSheetId,
                        questionNumber = parsedAnswer.questionNumber,
                        answer = "Asked: ${parsedAnswer.answer}", // You can refine this parsing logic
                        answerType = AnswerType.WORD_PROBLEM
                    ),
                    QuestionEntity(
                        answerSheetId = answerSheetId,
                        questionNumber = parsedAnswer.questionNumber + 1,
                        answer = "Given: ${parsedAnswer.answer}", // You can refine this parsing logic
                        answerType = AnswerType.WORD_PROBLEM
                    ),
                    QuestionEntity(
                        answerSheetId = answerSheetId,
                        questionNumber = parsedAnswer.questionNumber + 2,
                        answer = "Operation: ${parsedAnswer.answer}", // You can refine this parsing logic
                        answerType = AnswerType.WORD_PROBLEM
                    ),
                    QuestionEntity(
                        answerSheetId = answerSheetId,
                        questionNumber = parsedAnswer.questionNumber + 3,
                        answer = "Number Sentence: ${parsedAnswer.answer}", // You can refine this parsing logic
                        answerType = AnswerType.WORD_PROBLEM
                    ),
                    QuestionEntity(
                        answerSheetId = answerSheetId,
                        questionNumber = parsedAnswer.questionNumber + 4,
                        answer = "Solution/Answer: ${parsedAnswer.answer}", // You can refine this parsing logic
                        answerType = AnswerType.WORD_PROBLEM
                    )
                )
            }

            // Combine the two sets of entities
            val allQuestionEntities = questionEntities + wordProblemEntities

            // Save all the entities to the database
            answerKeyDao.insertQuestions(allQuestionEntities)
            Log.d("AnswerKeyViewModel", "Inserted questions: $allQuestionEntities") // Log inserted questions

            // Save parsed answers to the database
            answerKeyDao.insertQuestions(questionEntities)
            Log.d("AnswerKeyViewModel", "Saved parsed answers: $questionEntities")
        }
    }

    // Save answers manually to the database
    fun saveAnswersToDatabase(answerSheetId: Int, answers: List<Answer>) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear all questions before inserting new ones (if necessary)
//            answerKeyDao.clearAll() // Optional, only if you want to start fresh

            // Clear all questions for the specific answer sheet if necessary (optional)
            answerKeyDao.clearQuestionsByAnswerSheetId(answerSheetId)

            // Map answers to QuestionEntity objects and save
            val questionEntities = answers.flatMap { answer ->
                when(answer) {
                    is Answer.MultipleChoice -> listOf(
                        QuestionEntity(
                            answerSheetId = answerSheetId,
                            questionNumber = answer.questionNumber,
                            answer = answer.selectedAnswer,
                            answerType = AnswerType.MULTIPLE_CHOICE // Set the type here
                        )
                    )
                    is Answer.Identification -> listOf(
                        QuestionEntity(
                            answerSheetId = answerSheetId,
                            questionNumber = answer.questionNumber,
                            answer = answer.answer,
                            answerType = AnswerType.IDENTIFICATION // Set the type here
                        )
                    )
                    is Answer.WordProblemAnswer ->
                        // Save all components of word problems as separate entries
                        listOf(
                            QuestionEntity(
                                answerSheetId = answerSheetId,
                                questionNumber = answer.questionNumber,
                                answer = "Asked: ${answer.asked}",
                                answerType = AnswerType.WORD_PROBLEM
                            ),
                            QuestionEntity(
                                answerSheetId = answerSheetId,
                                questionNumber = answer.questionNumber + 1,
                                answer = "Given: ${answer.given}",
                                answerType = AnswerType.WORD_PROBLEM
                            ),
                            QuestionEntity(
                                answerSheetId = answerSheetId,
                                questionNumber = answer.questionNumber + 2,
                                answer = "Operation: ${answer.operation}",
                                answerType = AnswerType.WORD_PROBLEM
                            ),
                            QuestionEntity(
                                answerSheetId = answerSheetId,
                                questionNumber = answer.questionNumber + 3,
                                answer = "Number Sentence: ${answer.numberSentence}",
                                answerType = AnswerType.WORD_PROBLEM
                            ),
                            QuestionEntity(
                                answerSheetId = answerSheetId,
                                questionNumber = answer.questionNumber + 4,
                                answer = "Solution/Answer: ${answer.solutionAnswer}",
                                answerType = AnswerType.WORD_PROBLEM
                            )
                        )
                }
            }

            answerKeyDao.insertQuestions(questionEntities)
            Log.d("AnswerKeyViewModel", "Inserted questions: $questionEntities") // Log inserted questions
        }
    }

    // Determine the answer type for a parsed answer
    private fun determineAnswerType(parsedAnswer: ParsedAnswer): AnswerType {
        return when {
            // Detect multiple-choice answers (e.g., 1. A)
            parsedAnswer.answer.matches(Regex("[A-D]")) -> AnswerType.MULTIPLE_CHOICE

            // Detect word problem components based on labels like "Asked (A)", "Given (B)", etc.
            parsedAnswer.answer.contains(Regex("""\b(Asked|Given|Operation|Number Sentence|Solution/Answer)\b""", RegexOption.IGNORE_CASE)) -> {
                AnswerType.WORD_PROBLEM
            }

            // Default to identification if no specific pattern matches
            else -> AnswerType.IDENTIFICATION
        }
    }
}