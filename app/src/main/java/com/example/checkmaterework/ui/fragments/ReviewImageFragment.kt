package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide

import com.example.checkmaterework.databinding.FragmentReviewImageBinding
import com.example.checkmaterework.models.AnswerKeyViewModel
import com.example.checkmaterework.models.AnswerKeyViewModelFactory
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.ImageCaptureEntity
import com.example.checkmaterework.models.ImageCaptureViewModel
import com.example.checkmaterework.models.ReviewImageViewModel
import com.example.checkmaterework.models.ReviewImageViewModelFactory
import com.example.checkmaterework.models.StudentEntity
import kotlinx.coroutines.launch

class ReviewImageFragment : Fragment() {

    private lateinit var reviewBinding: FragmentReviewImageBinding
    private lateinit var imageCaptureViewModel: ImageCaptureViewModel
    private lateinit var reviewImageViewModel: ReviewImageViewModel
    private lateinit var answerKeyViewModel: AnswerKeyViewModel
    private var sheetId: Int = 0
    private lateinit var imagePath: String
    private lateinit var recognizedText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            sheetId = it.getInt(ARG_SHEET_ID)
            imagePath = it.getString(ARG_IMAGE_PATH) ?: ""
            recognizedText = it.getString(ARG_RECOGNIZED_TEXT) ?: ""
        }

        val dao = AnswerSheetDatabase.getDatabase(requireContext()).imageCaptureDao()
        val studentDao = AnswerSheetDatabase.getDatabase(requireContext()).studentDao()
        val classDao = AnswerSheetDatabase.getDatabase(requireContext()).classDao()
        val answerSheetDao = AnswerSheetDatabase.getDatabase(requireContext()).answerSheetDao()
        val answerKeyDao = AnswerSheetDatabase.getDatabase(requireContext()).answerKeyDao()

        reviewImageViewModel = ViewModelProvider(this, ReviewImageViewModelFactory(dao, studentDao, classDao, answerSheetDao))
            .get(ReviewImageViewModel::class.java)

        answerKeyViewModel = ViewModelProvider(this, AnswerKeyViewModelFactory(answerKeyDao))
            .get(AnswerKeyViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        reviewBinding = FragmentReviewImageBinding.inflate(inflater, container, false)
        return reviewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageCaptureId = arguments?.getInt("imageCaptureId") ?: return

        lifecycleScope.launch {
            val imageCapture = reviewImageViewModel.getImageCaptureById(imageCaptureId)
//            val student = imageCapture?.studentId?.let { reviewImageViewModel.getStudentById(it) }
//            val classEntity = imageCapture?.sectionId?.let { reviewImageViewModel.getClassById(it) }

//            student?.let {
//                // Use student.lastName, student.firstName, etc.
//                Log.d("Student Info", "Student Name: ${it.studentName}")
//            }
        }

        // Load captured image using Glide or similar library
        Glide.with(this).load(imagePath).into(reviewBinding.imageViewCaptured)
        reviewBinding.textViewRecognizedText.text = recognizedText

        // Parse recognized text
        val parsedAnswers = parseRecognizedText(recognizedText)
        reviewBinding.textViewParsedAnswers.text = parsedAnswers.toString()

        // Compare recognized answers with answer key
        compareAnswers(parsedAnswers)

        // Set save button listener
        reviewBinding.buttonSave.setOnClickListener {
            saveImageCapture()
        }
    }

    private fun parseRecognizedText(recognizedText: String): Map<Int, String> {
        val answerMap = mutableMapOf<Int, String>()
        val lines = recognizedText.split("\n")
        val questionRegex = Regex("Q(\\d+):\\s*(\\w+)")
        for (line in lines) {
            val matchResult = questionRegex.find(line)
            if (matchResult != null) {
                val questionNumber = matchResult.groupValues[1].toInt()
                val answer = matchResult.groupValues[2]
                answerMap[questionNumber] = answer
            }
        }
        return answerMap
    }

    private fun compareAnswers(studentAnswers: Map<Int, String>) {
        answerKeyViewModel.loadAnswerKeysForSheet(sheetId)
        answerKeyViewModel.savedAnswerKeys.observe(viewLifecycleOwner) { correctAnswers ->
            lifecycleScope.launch {
                var score = 0
                for (question in correctAnswers) {
                    val studentAnswer = studentAnswers[question.questionNumber]
                    if (studentAnswer == question.answer) {
                        score++
                    }
                }
                reviewBinding.textInputScore.setText("$score")
            }
        }
    }

//    private fun compareAnswers(sheetId: Int, recognizedText: String) {
//        val correctAnswers = reviewImageViewModel.getAnswerKeyForSheet(sheetId) // Load the answer key from the database
//        val studentAnswers = parseRecognizedText(recognizedText) // Parse the OCR recognized text
//
//        // Compare and calculate the score
//        var score = 0
//        correctAnswers.forEachIndexed { index, correctAnswer ->
//            if (correctAnswer == studentAnswers[index]) {
//                score++
//            }
//        }
//
//        // Update the score and display it
//        reviewBinding.textViewScore.text = "Score: $score"
//    }

//    private fun getAnswerKeyForSheet(sheetId: Int) {
//        // Fetch the answer key based on the sheet ID
//        return answerKeyViewModel.loadAnswerKeysForSheet(sheetId)
//    }



    private fun saveImageCapture() {
        val studentName = reviewBinding.textInputStudentName.text.toString()
        val sectionName = reviewBinding.textInputSection.text.toString()
        val score = reviewBinding.textInputScore.text.toString().toIntOrNull() ?: 0

        // Step 1: Create or retrieve the StudentEntity
        lifecycleScope.launch {
//            val classEntity = reviewImageViewModel.getClassByName(sectionName)
//            val classId = classEntity?.classId ?: 0

//            val student = StudentEntity(studentName = studentName, score = score, classId = classId)
//            val studentId = reviewImageViewModel.insertStudent(student)

//            // Step 2: Create and save ImageCaptureEntity
//            val imageCapture = ImageCaptureEntity(
//                sheetId = sheetId,
//                studentId = studentId.toInt(),
//                imagePath = imagePath,
//                sectionId = classId,
//                score = score
//            )
//            reviewImageViewModel.insertImageCapture(imageCapture)

            // Navigate back or show a success message
            requireActivity().onBackPressed()
        }

//        val imageCapture = ImageCaptureEntity(sheetId = sheetId, imagePath = imagePath, lastName = lastName, firstName = firstName, score = score, section = section)
//
//        imageCaptureViewModel.insertImageCapture(imageCapture)
//        // Navigate back or show a success message
//        requireActivity().onBackPressed()
    }

    companion object {
        private const val ARG_SHEET_ID = "sheet_id"
        private const val ARG_IMAGE_PATH = "image_path"
        private const val ARG_RECOGNIZED_TEXT = "recognized_text"

        @JvmStatic
        fun newInstance(sheetId: Int, imagePath: String, recognizedText: String): ReviewImageFragment =
            ReviewImageFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SHEET_ID, sheetId)
                    putString(ARG_IMAGE_PATH, imagePath)
                    putString(ARG_RECOGNIZED_TEXT, recognizedText)
                }
            }
    }
}
