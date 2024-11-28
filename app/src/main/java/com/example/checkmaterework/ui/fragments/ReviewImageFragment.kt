package com.example.checkmaterework.ui.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.checkmaterework.R

import com.example.checkmaterework.databinding.FragmentReviewImageBinding
import com.example.checkmaterework.models.AnswerKeyViewModel
import com.example.checkmaterework.models.AnswerKeyViewModelFactory
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.ImageCaptureEntity
import com.example.checkmaterework.models.ImageCaptureViewModel
import com.example.checkmaterework.models.QuestionEntity
import com.example.checkmaterework.models.ReviewImageViewModel
import com.example.checkmaterework.models.ReviewImageViewModelFactory
import com.example.checkmaterework.models.StudentEntity
import com.example.checkmaterework.models.StudentRecordEntity
import kotlinx.coroutines.launch

class ReviewImageFragment : Fragment(), ToolbarTitleProvider {

    private lateinit var reviewBinding: FragmentReviewImageBinding
    private lateinit var imageCaptureViewModel: ImageCaptureViewModel
    private lateinit var reviewImageViewModel: ReviewImageViewModel
    private lateinit var answerKeyViewModel: AnswerKeyViewModel
    private var sheetId: Int = 0
    private lateinit var imagePath: String
    private lateinit var recognizedText: String
    private var parsedAnswers: MutableList<ParsedAnswer> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            sheetId = it.getInt(ARG_SHEET_ID)
            imagePath = it.getString(ARG_IMAGE_PATH) ?: ""
            recognizedText = it.getString(ARG_RECOGNIZED_TEXT) ?: ""
        }

        val database = AnswerSheetDatabase.getDatabase(requireContext())

        val dao = database.imageCaptureDao()
        val studentDao = database.studentDao()
        val classDao = database.classDao()
        val answerSheetDao = database.answerSheetDao()
        val answerKeyDao = database.answerKeyDao()
        val studentRecordDao = database.studentRecordDao()

        reviewImageViewModel = ViewModelProvider(this, ReviewImageViewModelFactory(dao, studentDao, classDao, answerSheetDao, studentRecordDao))
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
            val student = imageCapture?.studentId?.let { reviewImageViewModel.getStudentById(it) }
            val classEntity = imageCapture?.sectionId?.let { reviewImageViewModel.getClassById(it) }

            student?.let {
                // Use student.lastName, student.firstName, etc.
                Log.d("Student Info", "Student Name: ${it.studentName}")
            }
        }

        // Load captured image using Glide or similar library
        Glide.with(this).load(imagePath).into(reviewBinding.imageViewCaptured)

        // Display recognized text or "No detected text" if empty
        reviewBinding.textViewRecognizedText.text = recognizedText.ifEmpty {
            "No detected text"
        }

        // Parse recognized text and compare with answer key
        parsedAnswers = parseRecognizedText(recognizedText)

        // Display parsed answers with analysis
        val parsedAnswersText = parsedAnswers.joinToString("\n") { parsedAnswer ->
            "Q${parsedAnswer.questionNumber}: ${parsedAnswer.answer} - " +
                    if (parsedAnswer.isCorrect) "Correct" else "Incorrect"
        }
        reviewBinding.textViewParsedAnswers.text = parsedAnswersText

        // Pass parsed answers as a Map to compareAnswers
        val studentAnswers = parsedAnswers.associate { it.questionNumber to it.answer }
        compareAnswers(studentAnswers)

        // Set save button listener
        reviewBinding.buttonSave.setOnClickListener {
            saveStudentRecord()
        }
    }

    data class ParsedAnswer(
        val questionNumber: Int,
        val answer: String,
        var isCorrect: Boolean
    )

//    private fun parseRecognizedText(recognizedText: String): MutableList<ParsedAnswer> {
//        val answersList = mutableListOf<ParsedAnswer>()
//
//        // Extract each question-answer pair from recognizedText
//        recognizedText.split("\n").forEach { line ->
//            val parts = line.split("|")
//            if (parts.size == 2) {
//                val questionNumber = parts[0].trim().removePrefix("|").toIntOrNull()
//                val answer = parts[1].trim()
//
//                questionNumber?.let {
//                    answersList.add(ParsedAnswer(it, answer, false)) // Mark correct later in compareAnswers
//                }
//            }
//        }
//        return answersList
//    }

    private fun parseRecognizedText(recognizedText: String): MutableList<ParsedAnswer> {
        val answersList = mutableListOf<ParsedAnswer>()
        val lines = recognizedText.split("\n") // Split lines

        val pattern = Regex("""(\d+)\.\s*(.*)""") // Matches "1. answer"
//        val pattern = Regex("""(\d+)\|\s*(.*)""") // Matches "1. answer"
//        val pattern = Regex("""\|\s*(\d+)\s*\|\s*(.*?)\s*\|""") // Matches "| 1 | answer |"

        for (line in lines) {
            val match = pattern.find(line)
            if (match != null) {
                val questionNumber = match.groupValues[1].toIntOrNull() ?: continue
                val answer = match.groupValues[2].trim()
                answersList.add(ParsedAnswer(questionNumber, answer, false))
            }
        }

        return answersList
    }

//    private fun parseRecognizedText(recognizedText: String): Map<Int, String> {
//        val answerMap = mutableMapOf<Int, String>()
//        val lines = recognizedText.split("\n")
//        val questionRegex = Regex("Q(\\d+):\\s*(\\w+)")
//        for (line in lines) {
//            val matchResult = questionRegex.find(line)
//            if (matchResult != null) {
//                val questionNumber = matchResult.groupValues[1].toInt()
//                val answer = matchResult.groupValues[2]
//                answerMap[questionNumber] = answer
//            }
//        }
//        return answerMap
//    }

    private fun compareAnswers(studentAnswers: Map<Int, String>) {
        answerKeyViewModel.loadAnswerKeysForSheet(sheetId)
        answerKeyViewModel.savedAnswerKeys.observe(viewLifecycleOwner) { correctAnswers ->
            lifecycleScope.launch {
                var score = 0
                val itemAnalysis = StringBuilder("Item Analysis:\n")

                // Compare student answers with correct answers and build analysis
                for (question in correctAnswers) {
                    val studentAnswer = studentAnswers[question.questionNumber]?: "No detected answer"
                    val isCorrect = studentAnswer.equals(question.answer, ignoreCase = true)
                    if (isCorrect) score++

                    // Update parsedAnswers with correctness
                    parsedAnswers.find { it.questionNumber == question.questionNumber }?.isCorrect = isCorrect

                    itemAnalysis.append("Q${question.questionNumber}: ${if (isCorrect) "Correct" else "Incorrect"}\n")

                }

                // Update the score and item analysis views
//                reviewBinding.textInputScore.setText("$score")
//                reviewBinding.textInputScore.text = Editable.Factory.getInstance().newEditable("Score: $score / ${correctAnswers.size}")
                reviewBinding.textInputScore.text = Editable.Factory.getInstance().newEditable("$score")
                reviewBinding.textViewItemAnalysis.text = itemAnalysis.toString()

                // Display the answer key in the table
                displayAnswerKeyInTable(correctAnswers, parsedAnswers)
            }
        }
    }

    private fun displayAnswerKeyInTable(answerKeyList: List<QuestionEntity>?, parsedAnswers: List<ParsedAnswer>) {
        // Clear the table first (if it's already populated)
        reviewBinding.answerKeyTable.removeAllViews()

        // Add table header
        val headerRow = TableRow(context).apply {
            addView(createHeaderTextView("Q#"))
            addView(createHeaderTextView("Correct Answer"))
            addView(createHeaderTextView("Student's Answer"))
            addView(createHeaderTextView("Remarks"))
            addView(createHeaderTextView("Points"))
        }
        reviewBinding.answerKeyTable.addView(headerRow)

        // Create a map of parsed answers for quick lookup
        val parsedAnswersMap = parsedAnswers.associateBy { it.questionNumber }

        // Initialize the total score
        var totalScore = 0

        // Add each answer key as a row
        for (answerKey in answerKeyList ?: emptyList()) {
            val tableRow  = TableRow(context)

            // Question Number
            tableRow.addView(createTextView(answerKey.questionNumber.toString()))

            // Correct Answer
            tableRow.addView(createTextView(answerKey.answer))

            // Student's Answer
            val studentAnswer = parsedAnswersMap[answerKey.questionNumber]?.answer ?: "N/A"
            val studentAnswerView = createTextView(studentAnswer).apply {
                setTextColor(if (studentAnswer.equals(answerKey.answer, ignoreCase = true)) Color.GREEN else Color.RED)
            }
            tableRow.addView(studentAnswerView)

            // Remarks
            val isCorrect = studentAnswer.equals(answerKey.answer, ignoreCase = true)
            val remarksView = createTextView(if (isCorrect) "Correct" else "Incorrect").apply {
                setTextColor(if (isCorrect) Color.GREEN else Color.RED)
            }
            tableRow.addView(remarksView)

            // Points (editable field)
            val pointsInput = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                val initialPoints = if (isCorrect) 1 else 0
                setText("$initialPoints")
                setPadding(16, 16, 16, 16)
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER

                // Add text change listener to dynamically update the total score
                addTextChangedListener { text ->
                    val enteredPoints = text.toString().toIntOrNull() ?: 0

                    // Calculate the total score dynamically
                    val currentPoints = (tag as? Int) ?: 0 // Retrieve previously stored points
                    totalScore += enteredPoints - currentPoints // Adjust total based on change
                    tag = enteredPoints // Update tag with new points

                    // Update the total score display
                    updateTotalScore(totalScore)
                }
                tag = initialPoints
            }

            tableRow.addView(pointsInput)
            reviewBinding.answerKeyTable.addView(tableRow)

            // Add the initial points to the total score
            totalScore += if (isCorrect) 1 else 0
        }

        // Initialize the total score display
        updateTotalScore(totalScore)
    }

    private fun createHeaderTextView(text: String) = TextView(context).apply {
        this.text = text
        setPadding(16, 16, 16, 16)
        setTypeface(null, Typeface.BOLD)
        setTextColor(Color.BLACK)
        gravity = Gravity.CENTER
    }

    private fun createTextView(text: String) = TextView(context).apply {
        this.text = text
        setPadding(16, 16, 16, 16)
        setTextColor(Color.BLACK)
        gravity = Gravity.CENTER
    }

    private fun updateTotalScore(totalScore: Int) {
        // Update the score field
        reviewBinding.textInputScore.text = Editable.Factory.getInstance().newEditable("$totalScore")
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

    private fun saveStudentRecord() {
        val studentName = reviewBinding.textInputStudentName.text.toString()
        val sectionName = reviewBinding.textInputSection.text.toString()
        val score = reviewBinding.textInputScore.text.toString().toIntOrNull() ?: 0

        // Generate `itemAnalysis` if `parsedAnswers` is empty
        val itemAnalysis: String
        if (parsedAnswers.isEmpty()) {
            Log.d("SaveRecord", "Parsed answers not initialized, generating item analysis directly.")

            answerKeyViewModel.loadAnswerKeysForSheet(sheetId)
            val correctAnswers = answerKeyViewModel.savedAnswerKeys.value ?: emptyList()

            itemAnalysis = correctAnswers.joinToString("; ") { question ->
                val studentAnswer = "" // Replace with logic to retrieve the student's answer if necessary
                val isCorrect = studentAnswer.equals(question.answer, ignoreCase = true)
                "Q${question.questionNumber}: ${if (isCorrect) "Correct" else "Incorrect"}"
            }
        } else {
            // Use `parsedAnswers` if available
            itemAnalysis = parsedAnswers.joinToString("; ") {
                "Q${it.questionNumber}: ${if (it.isCorrect) "Correct" else "Incorrect"}"
            }
        }

        lifecycleScope.launch {
//            val classEntity = reviewImageViewModel.getClassByName(sectionName)
//            val classId = classEntity?.classId ?: 0

//            val student = StudentEntity(studentName = studentName, classId = classId)
//            val studentId = reviewImageViewModel.insertStudent(student)

            // Check if the class exists by name
            val classEntity = reviewImageViewModel.getClassByName(sectionName)
            if (classEntity == null) {
                Log.d("SaveRecord", "Class with name '$sectionName' not found.")
                return@launch
            }
            val classId = classEntity.classId
            Log.d("SaveRecord", "Class found: ID = $classId, Name = ${classEntity.className}")

            // Check if student exists by name and class ID
            val existingStudent = reviewImageViewModel.getStudentByNameAndClass(studentName, classId)
            val studentId: Long
            if (existingStudent != null) {
                studentId = existingStudent.studentId.toLong()
                Log.d("SaveRecord", "Existing student found: ID = $studentId, Name = $studentName")
            } else {
                // Insert new student if not found
                val newStudent = StudentEntity(studentName = studentName, classId = classId)
                studentId = reviewImageViewModel.insertStudent(newStudent)
                Log.d("SaveRecord", "New student created: ID = $studentId, Name = $studentName")
            }

            // Validate if the answer sheet ID is correct
            val answerSheet = reviewImageViewModel.getAnswerSheetById(sheetId)
            if (answerSheet == null) {
                Log.d("SaveRecord", "Answer sheet with ID '$sheetId' not found.")
                return@launch
            }
            Log.d("SaveRecord", "Answer sheet found: ID = $sheetId")
//
//            // Check if student exists
//            val existingStudent = reviewImageViewModel.getStudentByNameAndClass(studentName, classId)
//            val studentId: Long
//
//            if (existingStudent != null) {
//                studentId = existingStudent.studentId.toLong()
//            } else {
//                // Insert new student if not found
//                val newStudent = StudentEntity(studentName = studentName, classId = classId)
//                studentId = reviewImageViewModel.insertStudent(newStudent)
//            }

            val studentRecord = StudentRecordEntity(
                studentId = studentId.toInt(),
                classId = classId,
                answerSheetId = sheetId,
                score = score,
                itemAnalysis = itemAnalysis // Set the new property
//                examDate = getCurrentDate()
            )
            reviewImageViewModel.insertStudentRecord(studentRecord)
            Log.d("SaveRecord", "Student record saved: StudentID = ${studentRecord.studentId}, ClassID = ${studentRecord.classId}, AnswerSheetID = ${studentRecord.answerSheetId}, Score = ${studentRecord.score}, Item Analysis = ${studentRecord.itemAnalysis}")

            requireActivity().onBackPressed()
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.save_score_title)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    private fun setupToolbar() {
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(activity.findViewById(R.id.myToolbar))

        val canGoBack = parentFragmentManager.backStackEntryCount > 0
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(canGoBack)
        activity.supportActionBar?.setDisplayShowHomeEnabled(canGoBack)

        activity.supportActionBar?.title = getFragmentTitle()

        if (canGoBack) {
            activity.findViewById<Toolbar>(R.id.myToolbar).setNavigationOnClickListener {
                activity.onBackPressed()
            }
        }
    }

    companion object {
        private const val ARG_SHEET_ID = "sheet_id"
        private const val ARG_IMAGE_PATH = "image_path"
        private const val ARG_RECOGNIZED_TEXT = "recognized_text"

        @JvmStatic
        fun newInstance(sheetId: Int, recognizedText: String): ReviewImageFragment =
            ReviewImageFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SHEET_ID, sheetId)
                    putString(ARG_RECOGNIZED_TEXT, recognizedText)
                }
            }
    }
}
