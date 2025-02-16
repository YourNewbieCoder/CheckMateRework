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
import com.example.checkmaterework.models.ParsedAnswer
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
    private val questionPointsMap = mutableMapOf<Int, Int>()

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

        // Parse name and section from recognized text
        val (studentName, studentSection) = parseNameAndSection(recognizedText)

        // Populate the input fields with the parsed data
        reviewBinding.textInputStudentName.setText(studentName)
        reviewBinding.textInputSection.setText(studentSection)

        if (studentName != null) {
            Log.d("Student Info", "Name: $studentName")
        } else {
            Log.d("Student Info", "Name not detected")
        }

        if (studentSection != null) {
            Log.d("Student Info", "Section: $studentSection")
        } else {
            Log.d("Student Info", "Section not detected")
        }

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

    private fun parseRecognizedText(recognizedText: String): MutableList<ParsedAnswer> {
        val answersList = mutableListOf<ParsedAnswer>()
        val lines = recognizedText.split("\n") // Split lines

        // Temporary variables for word problem parts
        var currentQuestionRange: Pair<Int, Int>? = null
        var asked: String? = null
        var given: String? = null
        var operation: String? = null
        var numberSentence: String? = null
        var solution: String? = null

        // Iterate through the lines and parse the data based on patterns

        val pattern = Regex("""(\d+)\.\s*(.*)""") // Matches "1. answer"

        for (line in lines) {
            when {
                // Match question range (e.g., "1-5.")
                line.matches(Regex("""\d+-\d+\.\s*""")) -> {
                    val rangeParts = line.substringBefore(".").split("-").mapNotNull { it.toIntOrNull() }
                    if (rangeParts.size == 2) {
                        currentQuestionRange = Pair(rangeParts[0], rangeParts[1])
                    }
                }
                line.contains("Asked") -> {
                    asked = line.substringAfter("Asked:").trim()
                }
                line.contains("Given") -> {
                    given = line.substringAfter("Given:").trim()
                }
                line.contains("Operation") -> {
                    operation = line.substringAfter("Operation:").trim()
                }
                line.contains("Number Sentence") -> {
                    numberSentence = line.substringAfter("Number Sentence:").trim()
                }
                line.contains("Solution/Answer") -> {
                    solution = line.substringAfter("Solution/Answer:").trim()
                }
//                // Parsing Multiple Choice or Identification questions
//                line.matches(Regex("""\d+\.\s*[A-D]""")) -> {
//                    // This matches multiple choice answers like 1. A, 2. B
//                    val (questionNumber, answer) = parseMultipleChoiceOrIdentification(line)
//                    answersList.add(ParsedAnswer(questionNumber = questionNumber, answer = answer))
//                }
//                line.matches(Regex("""\d+\.\s*\w+""")) -> {
//                    // This matches identification answers like "1. Answer"
//                    val (questionNumber, answer) = parseMultipleChoiceOrIdentification(line)
//                    answersList.add(ParsedAnswer(questionNumber = questionNumber, answer = answer))
//                }
                else -> {
                    // Process any complete word problem if all parts are detected
                    if (currentQuestionRange != null &&
                        asked != null && given != null && operation != null &&
                        numberSentence != null && solution != null) {

                        val (start, end) = currentQuestionRange
                        val rangeQuestions = (start..end).toList()

                        // Add parsed answers for the word problem parts
                        answersList.add(ParsedAnswer(questionNumber = rangeQuestions[0], answer = "Asked: $asked",false))
                        answersList.add(ParsedAnswer(questionNumber = rangeQuestions[1], answer = "Given: $given",false))
                        answersList.add(ParsedAnswer(questionNumber = rangeQuestions[2], answer = "Operation: $operation",false))
                        answersList.add(ParsedAnswer(questionNumber = rangeQuestions[3], answer = "Number Sentence: $numberSentence",false))
                        answersList.add(ParsedAnswer(questionNumber = rangeQuestions[4], answer = "Solution/Answer: $solution",false))

                        // Reset variables for the next word problem
                        currentQuestionRange = null
                        asked = null
                        given = null
                        operation = null
                        numberSentence = null
                        solution = null
                    }
                }
            }

            val match = pattern.find(line)
            if (match != null) {
                val questionNumber = match.groupValues[1].toIntOrNull() ?: continue
                val answer = match.groupValues[2].trim()
                answersList.add(ParsedAnswer(questionNumber, answer, false))
            }
        }

        // Handle leftover word problem if not already added
        if (currentQuestionRange != null &&
            asked != null && given != null && operation != null &&
            numberSentence != null && solution != null) {

            val (start, end) = currentQuestionRange
            val rangeQuestions = (start..end).toList()

            answersList.add(ParsedAnswer(questionNumber = rangeQuestions[0], answer = "Asked: $asked",false))
            answersList.add(ParsedAnswer(questionNumber = rangeQuestions[1], answer = "Given: $given",false))
            answersList.add(ParsedAnswer(questionNumber = rangeQuestions[2], answer = "Operation: $operation",false))
            answersList.add(ParsedAnswer(questionNumber = rangeQuestions[3], answer = "Number Sentence: $numberSentence",false))
            answersList.add(ParsedAnswer(questionNumber = rangeQuestions[4], answer = "Solution/Answer: $solution",false))
        }

        return answersList
    }

//    // Parse Multiple Choice or Identification answers
//    private fun parseMultipleChoiceOrIdentification(line: String): Pair<Int, String> {
//        val parts = line.split(".").map { it.trim() }
//        val questionNumber = parts[0].toIntOrNull() ?: -1
//        val answer = parts.getOrElse(1) { "" }
//        return Pair(questionNumber, answer)
//    }

    private fun parseNameAndSection(recognizedText: String): Pair<String?, String?> {
        var name: String? = null
        var section: String? = null

        // Split the recognized text into lines
        val lines = recognizedText.split("\n")

        // Define regex patterns for name and section
        val namePattern = Regex("""(?i)name\s*[:\-]\s*(.+)""") // Matches "Name: John Doe"
        val sectionPattern = Regex("""(?i)section\s*[:\-]\s*(.+)""") // Matches "Section: 10-A"

        for (line in lines) {
            val trimmedLine = line.trim()

            // Match name
            if (name == null) {
                val nameMatch = namePattern.find(trimmedLine)
                if (nameMatch != null) {
                    name = nameMatch.groupValues[1].trim()
                }
            }

            // Match section
            if (section == null) {
                val sectionMatch = sectionPattern.find(trimmedLine)
                if (sectionMatch != null) {
                    section = sectionMatch.groupValues[1].trim()
                }
            }

            // Stop searching if both values are found
            if (name != null && section != null) break
        }
        return Pair(name, section)
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
                    val points = if (isCorrect) 1 else 0
//                    if (isCorrect) score++

                    // Initialize questionPointsMap
                    questionPointsMap[question.questionNumber] = points

                    // Update parsedAnswers with correctness
                    parsedAnswers.find { it.questionNumber == question.questionNumber }?.isCorrect = isCorrect

                    itemAnalysis.append("Q${question.questionNumber}: ${if (isCorrect) "Correct" else "Incorrect"}\n")

                }

                // Update the score and item analysis views
//                reviewBinding.textInputScore.setText("$score")
//                reviewBinding.textInputScore.text = Editable.Factory.getInstance().newEditable("Score: $score / ${correctAnswers.size}")
//                reviewBinding.textInputScore.text = Editable.Factory.getInstance().newEditable("$score")
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

            // Student's Answer (editable field)
            val studentAnswer = parsedAnswersMap[answerKey.questionNumber]?.answer ?: "N/A"
            val studentAnswerView = EditText(context).apply {
                setText(studentAnswer)
                setPadding(16, 16, 16, 16)
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setBackgroundResource(android.R.drawable.edit_text)

                // Set the text color based on whether the answer is correct or not
                setTextColor(if (studentAnswer.equals(answerKey.answer, ignoreCase = true)) Color.GREEN else Color.RED)
            }

            // Remarks
            val isCorrect = studentAnswer.equals(answerKey.answer, ignoreCase = true)
            val remarksView = createTextView(if (isCorrect) "Correct" else "Incorrect").apply {
                setTextColor(if (isCorrect) Color.GREEN else Color.RED)
            }

            // Points (editable field)
            val pointsInput = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                val initialPoints = if (studentAnswer.equals(answerKey.answer, ignoreCase = true)) 1 else 0
                setText("$initialPoints")
                setPadding(16, 16, 16, 16)
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER

                // Initialize points for this question
                questionPointsMap[answerKey.questionNumber] = initialPoints

                // Add text change listener to dynamically update the total score
                addTextChangedListener { text ->
                    val enteredPoints = text.toString().toIntOrNull() ?: 0

                    // Update the map with the new points
                    questionPointsMap[answerKey.questionNumber] = enteredPoints

                    // Calculate the total score dynamically
                    val currentPoints = (tag as? Int) ?: 0 // Retrieve previously stored points
                    totalScore += enteredPoints - currentPoints // Adjust total based on change
                    tag = enteredPoints // Update tag with new points

                    // Update the total score display
                    updateTotalScore(totalScore)

                    // Update remarks based on entered points
                    val remarks = if (enteredPoints > 0) "Correct" else "Incorrect"
                    remarksView.text = remarks
                    remarksView.setTextColor(if (enteredPoints > 0) Color.GREEN else Color.RED)

                    // Update item analysis dynamically
                    updateItemAnalysis()
                }
                tag = initialPoints
            }

            // Add text change listener to dynamically update Remarks, Points, and total score
            studentAnswerView.addTextChangedListener { text ->
                val updatedAnswer = text.toString()

                // Update the color based on whether the updated answer is correct or not
                studentAnswerView.setTextColor(if (updatedAnswer.equals(answerKey.answer, ignoreCase = true)) Color.GREEN else Color.RED)

                // Update Remarks
                val isUpdatedCorrect = updatedAnswer.equals(answerKey.answer, ignoreCase = true)
                remarksView.text = if (isUpdatedCorrect) "Correct" else "Incorrect"
                remarksView.setTextColor(if (isUpdatedCorrect) Color.GREEN else Color.RED)

                // Update Points
                val points = if (isUpdatedCorrect) 1 else 0
                pointsInput.setText("$points")

                // Update the total score dynamically
                val currentPoints = (pointsInput.tag as? Int) ?: 0 // Retrieve previously stored points
                totalScore += points - currentPoints // Adjust total based on change
                pointsInput.tag = points // Update tag with new points

                // Update the total score display
                updateTotalScore(totalScore)
            }

            // Add views to the table row
            tableRow.addView(studentAnswerView)
            tableRow.addView(remarksView)
            tableRow.addView(pointsInput)

            // Add the row to the table
            reviewBinding.answerKeyTable.addView(tableRow)

            // Add the initial points to the total score
            totalScore += if (studentAnswer.equals(answerKey.answer, ignoreCase = true)) 1 else 0
        }

        // Initialize the total score display
        updateTotalScore(totalScore)
    }

    private fun updateItemAnalysis() {
        val itemAnalysis = StringBuilder("Item Analysis:\n")

        for ((questionNumber, points) in questionPointsMap) {
            val status = if (points > 0) "Correct" else "Incorrect"
            itemAnalysis.append("Q$questionNumber: $status\n")
        }

        // Update the item analysis view
        reviewBinding.textViewItemAnalysis.text = itemAnalysis.toString()
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
//            itemAnalysis = parsedAnswers.joinToString("; ") {
//                "Q${it.questionNumber}: ${if (it.isCorrect) "Correct" else "Incorrect"}"
//            }
            itemAnalysis = questionPointsMap.entries.joinToString("; ") { (questionNumber, points) ->
                "Q$questionNumber: ${if (points > 0) "Correct" else "Incorrect"}"
            }

        }

        lifecycleScope.launch {
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

//            val itemAnalysis = questionPointsMap.entries.joinToString("; ") { (questionNumber, points) ->
//                "Q$questionNumber: ${if (points > 0) "Correct" else "Incorrect"}"
//            }

            val studentRecord = StudentRecordEntity(
                studentId = studentId.toInt(),
                classId = classId,
                answerSheetId = sheetId,
                score = score,
                itemAnalysis = itemAnalysis // Set the new property
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
