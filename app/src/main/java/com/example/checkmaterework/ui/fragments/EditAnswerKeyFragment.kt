package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentEditAnswerKeyBinding
import com.example.checkmaterework.models.Answer
import com.example.checkmaterework.models.AnswerKeyViewModel
import com.example.checkmaterework.models.AnswerKeyViewModelFactory
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.AnswerType
import com.example.checkmaterework.models.QuestionEntity
import com.example.checkmaterework.models.TextRecognitionViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.FileNotFoundException

class EditAnswerKeyFragment(private val answerSheet: AnswerSheetEntity) : Fragment(), ToolbarTitleProvider {

    private lateinit var editAnswerKeyBinding: FragmentEditAnswerKeyBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var isCameraActive = false
    private var imageCapture: ImageCapture? = null
    private var answerSheetId: Int = 0

    // ViewModel for handling text recognition
    private lateinit var textRecognitionViewModel: TextRecognitionViewModel

    private lateinit var answerKeyViewModel: AnswerKeyViewModel

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            showToast("Camera permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            answerSheetId = it.getInt("ANSWER_SHEET_ID")
        }

        // Initialize the ViewModel
        textRecognitionViewModel = ViewModelProvider(this)[TextRecognitionViewModel::class.java]

        // Initialize ViewModel (assuming you are using a ViewModelFactory for DI)
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).answerKeyDao()
        answerKeyViewModel = ViewModelProvider(this, AnswerKeyViewModelFactory(dao))
            .get(AnswerKeyViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        editAnswerKeyBinding = FragmentEditAnswerKeyBinding.inflate(inflater, container, false)
        return editAnswerKeyBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up button click for adding key with camera
        editAnswerKeyBinding.buttonAddKeyWithCamera.setOnClickListener {
            showImageSourceOptionsDialog()
        }

        // Observe the recognized text from the ViewModel and update the UI
        textRecognitionViewModel.recognizedText.observe(viewLifecycleOwner) { recognizedText ->
        }

        // Button to scan the current camera frame
        editAnswerKeyBinding.buttonScan.setOnClickListener {
            captureCurrentFrame()
        }

        // Load answer key data from the provided answer sheet
        loadAnswerKeyData(answerSheet)

        setupObservers()
        
        // Set up the Save button listener
        editAnswerKeyBinding.buttonSave.setOnClickListener {
            saveAnswers()
        }
    }

    private fun gatherAnswersFromInput(): List<Answer> {
        val answerKeyContainer = editAnswerKeyBinding.answerKeyContainer
        val answers = mutableListOf<Answer>()

        for (i in 0 until answerKeyContainer.childCount) {
            when (val view = answerKeyContainer.getChildAt(i)) {
                is ChipGroup -> {
                    // For Multiple Choice Questions
                    val questionNumber = view.tag as? Int
                    if (questionNumber != null) {
                        val selectedChipId = view.checkedChipId
                        if (selectedChipId != View.NO_ID) {
                            val selectedAnswer = view.findViewById<Chip>(selectedChipId).text.toString()
                            answers.add(Answer.MultipleChoice(questionNumber, selectedAnswer))
                            Log.d("EditAnswerKeyFragment", "Question $questionNumber: $selectedAnswer")
                        } else {
                            Log.d("EditAnswerKeyFragment", "No answer selected for question $questionNumber.")
                        }
                    }
                }
//                is LinearLayout -> {
//                    // For Identification Questions
//                    for (j in 0 until view.childCount) {
//                        val child = view.getChildAt(j)
//                        if (child is TextInputLayout) {
//                            val questionNumber = child.tag as? Int
//                            if (questionNumber != null) {
//                                val answerText = child.editText?.text.toString().trim()
//                                if (answerText.isNotEmpty()) {
//                                    answers.add(Answer.Identification(questionNumber, answerText))
//                                    Log.d("EditAnswerKeyFragment", "Question $questionNumber: $answerText")
//                                } else {
//                                    Log.d("EditAnswerKeyFragment", "No answer entered for question $questionNumber.")
//                                }
//                            }
//                        }
//                    }
//                }
                is LinearLayout -> {
                    val wordProblemAnswers = mutableMapOf<String, String>()
                    var wordProblemNumber: Int? = null // To track the question number for word problems

                    for (j in 0 until view.childCount) {
                        val child = view.getChildAt(j)
                        if (child is TextInputLayout) {
                            val tag = child.tag

                            // Check if the tag is an Int (for identification questions)
                            val questionNumber = (tag as? Int) ?: tag.toString().toIntOrNull()

                            if (questionNumber != null) {
                                val answerText = child.editText?.text.toString()
                                if (answerText.isNotEmpty()) {
                                    answers.add(Answer.Identification(questionNumber, answerText))
                                    Log.d("EditAnswerKeyFragment", "Question $questionNumber: $answerText")
                                } else {
                                    Log.d("EditAnswerKeyFragment", "No answer entered for question $questionNumber.")
                                }
                            } else if (tag is String) {
                                // Handle Word Problems
                                wordProblemNumber = tag.split(":").firstOrNull()?.toIntOrNull()

                                val answerText = child.editText?.text.toString()
                                if (answerText.isNotEmpty()) {
                                    wordProblemAnswers[tag] = answerText
                                }
                            }
                        }
                    }

                    // After processing all child views, if we have answers for a word problem, store them
                    if (wordProblemNumber != null && wordProblemAnswers.isNotEmpty()) {
                        val asked = wordProblemAnswers["$wordProblemNumber: Asked"] ?: ""
                        val given = wordProblemAnswers["$wordProblemNumber: Given"] ?: ""
                        val operation = wordProblemAnswers["$wordProblemNumber: Operation"] ?: ""
                        val numberSentence = wordProblemAnswers["$wordProblemNumber: Number Sentence"] ?: ""
                        val solution = wordProblemAnswers["$wordProblemNumber: Solution/Answer"] ?: ""

                        answers.add(Answer.WordProblemAnswer(wordProblemNumber, asked, given, operation, numberSentence, solution))
                        Log.d("EditAnswerKeyFragment", "Word Problem $wordProblemNumber: Asked=$asked, Given=$given, Operation=$operation, NumberSentence=$numberSentence, Solution=$solution")
                    }
                }
            }
        }
        return answers
    }

    private fun saveAnswers() {
        val answerSheetId = answerSheet.id
        val answers = gatherAnswersFromInput() // Retrieve the answers

        if (answers.isNotEmpty()) {
            // Assume you have a method in your ViewModel to save answers
            answerKeyViewModel.saveAnswersToDatabase(answerSheetId, answers)

        } else {
            Log.e("EditAnswerKeyFragment", "No answers to save.")
        }

        // Navigate to DisplaySavedAnswersFragment to show saved answers
        val fragment = DisplaySavedAnswersFragment.newInstance(answerSheetId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showImageSourceOptionsDialog() {
        val options = arrayOf("Camera", "Gallery")
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Choose an Option").setItems(options) { dialog, which ->
            when (which) {
                0 -> checkCameraPermissionAndStart()
                1 -> openImagePicker()
            }
        }
        builder.show()
    }

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        // Change toolbar title to "Scanning Key..."
        setupToolbarTitle("Scanning Key...")

        isCameraActive = true // Set the camera state to active
        editAnswerKeyBinding.viewFinder.visibility = View.VISIBLE // Show camera preview
        editAnswerKeyBinding.buttonScan.visibility = View.VISIBLE // Show the "Check Paper" button

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraPreview()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setupToolbarTitle(title: String) {
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.title = title
    }

    private fun bindCameraPreview() {
        val preview = Preview.Builder()
            .setTargetResolution(Size(1280, 720))
            .build()
            .apply {
                surfaceProvider = editAnswerKeyBinding.viewFinder.surfaceProvider
            }

        // Create ImageCapture instance
        val imageCapture = ImageCapture.Builder()
            .setTargetResolution(Size(1280, 720))
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Unbind all use cases before rebinding
            cameraProvider?.unbindAll()

            // Bind the preview and image capture to the lifecycle
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            // Store imageCapture for later use in captureCurrentFrame
            this.imageCapture = imageCapture
        } catch (e: Exception) {
            showToast("Error binding camera preview: ${e.message}")
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun captureCurrentFrame() {
        // Set up an ImageCapture use case
        val imageCapture = this.imageCapture // Use the imageCapture instance bound in bindCameraPreview()

        // Ensure imageCapture is not null
        if (imageCapture == null) {
            showToast("ImageCapture is null")
            return
        }

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {

                    // Convert ImageProxy to Bitmap and handle rotation
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    val bitmap = image.toBitmap().rotate(rotationDegrees)

                    displayCapturedImage(bitmap)
                    image.close()
                }
                override fun onError(exc: ImageCaptureException) {
                    showToast("Failed to capture image: ${exc.message}")
                }
            }
        )
    }

    private fun Bitmap.rotate(degrees: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun displayCapturedImage(bitmap: Bitmap?) {
        setupToolbarTitle("Captured Image")
        requireActivity().runOnUiThread {
            bitmap?.let {
                // Set the captured image to the ImageView
                editAnswerKeyBinding.imageViewSelected.setImageBitmap(it)
                editAnswerKeyBinding.imageViewSelected.visibility = View.VISIBLE

                // Show the "Proceed" button
                editAnswerKeyBinding.buttonProceedWithImage.visibility = View.VISIBLE

                // Set click listener on the "Proceed" button
                editAnswerKeyBinding.buttonProceedWithImage.setOnClickListener {
                    recognizeTextFromBitmap(bitmap) // Call text recognition here
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICKER && resultCode == AppCompatActivity.RESULT_OK){
            data?.data?.let { uri ->
                displaySelectedImage(uri)
            }
        }
    }

    private fun displaySelectedImage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Set the selected image to the ImageView
            editAnswerKeyBinding.imageViewSelected.setImageBitmap(bitmap)
            editAnswerKeyBinding.imageViewSelected.visibility = View.VISIBLE

            setupToolbarTitle("Image from Gallery")

            // Show the "Proceed" button
            editAnswerKeyBinding.buttonProceedWithImage.visibility = View.VISIBLE

            // Set click listener on the "Proceed" button
            editAnswerKeyBinding.buttonProceedWithImage.setOnClickListener {
                recognizeTextFromBitmap(bitmap) // Call text recognition here
            }
        } catch (e: FileNotFoundException) {
            showToast("File not found: ${e.message}")
        }
    }

    private fun recognizeTextFromBitmap(bitmap: Bitmap?) {
        val image = bitmap?.let { InputImage.fromBitmap(it, 0) }

        // Use ML Kit's text recognition with the image
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        if (image != null) {
            recognizer.process(image).addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                textRecognitionViewModel.setRecognizedText(recognizedText)

                navigateToScannedKeyFragment(recognizedText)
            }
            .addOnFailureListener { e ->
                showToast("Text recognition failed: ${e.message}")
            }
        }
    }

    private fun navigateToScannedKeyFragment(recognizedText: String) {
        val fragment = ScannedKeyFragment.newInstance(recognizedText)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deactivateCamera() {
        isCameraActive = false // Set the camera state to inactive
        editAnswerKeyBinding.viewFinder.visibility = View.GONE // Hide camera preview
        editAnswerKeyBinding.buttonScan.visibility = View.GONE // Hide the "Check Paper" button

        cameraProvider?.unbindAll()

        // Reset the toolbar title to the original title
        setupToolbarTitle(getFragmentTitle()) // Assuming this method sets the toolbar title
    }

    private fun loadAnswerKeyData(answerSheet: AnswerSheetEntity) {
        editAnswerKeyBinding.textViewSheetNameKey.text = answerSheet.name // Set the name of the answer sheet
        val answerKeyContainer = editAnswerKeyBinding.answerKeyContainer // Get the container for answer key items
        answerKeyContainer.removeAllViews() // Clear any existing views

        var currentItemNumber = 1

        // Add appropriate views for each question type
        answerSheet.examTypesList.forEach { (examType, itemCount) ->
            when (examType) {
                "Multiple Choice" -> repeat(itemCount) { addMultipleChoiceView(answerKeyContainer, currentItemNumber++) }
                "Identification" -> repeat(itemCount) { addIdentificationView(answerKeyContainer, currentItemNumber++) }
                "Word Problem" -> repeat(itemCount / 5) { addWordProblemView(answerKeyContainer, currentItemNumber.also { currentItemNumber += 5 }) }
            }
        }
        Log.d("EditAnswerKeyFragment", "Number of views in answerKeyContainer: ${answerKeyContainer.childCount}")
    }

    private fun setupObservers() {
        // Assuming you have the answerSheetId available
        answerKeyViewModel.loadAnswerKeysForSheet(answerSheetId)

        answerKeyViewModel.savedAnswerKeys.observe(viewLifecycleOwner) { questions ->
            questions?.let {
                populateAnswerKeys(it)
            }
        }
    }

    private fun populateAnswerKeys(questions: List<QuestionEntity>) {
        questions.forEach { question ->
            when (question.answerType) {
                AnswerType.MULTIPLE_CHOICE -> {
                    // Find the ChipGroup for the specific question
                    val chipGroup = editAnswerKeyBinding.answerKeyContainer.findViewWithTag<ChipGroup>(question.questionNumber)
                    chipGroup?.let {
                        val selectedAnswerIndex = when (question.answer) {
                            "A" -> 0
                            "B" -> 1
                            "C" -> 2
                            "D" -> 3
                            else -> -1 // Default case if no valid answer is found
                        }

                        if (selectedAnswerIndex != -1) {
                            // Get the Chip corresponding to the selected index and check it
                            it.check(it.getChildAt(selectedAnswerIndex).id)
                        } else {
                            Log.e("EditAnswerKeyFragment", "Invalid answer for question ${question.questionNumber}: ${question.answer}")
                        }
                    } ?: Log.e("EditAnswerKeyFragment", "ChipGroup for question ${question.questionNumber} not found.")
                }
                AnswerType.IDENTIFICATION -> {
                    val identificationLayout = editAnswerKeyBinding.answerKeyContainer.findViewWithTag<TextInputLayout>(question.questionNumber)
                    identificationLayout?.editText?.setText(question.answer)
                }
                AnswerType.WORD_PROBLEM -> {
                    val prefix = "${question.questionNumber}:"
                    val askedLayout = editAnswerKeyBinding.answerKeyContainer.findViewWithTag<TextInputLayout>("$prefix Asked")
                    val givenLayout = editAnswerKeyBinding.answerKeyContainer.findViewWithTag<TextInputLayout>("$prefix Given")
                    val operationLayout = editAnswerKeyBinding.answerKeyContainer.findViewWithTag<TextInputLayout>("$prefix Operation")
                    val numberSentenceLayout = editAnswerKeyBinding.answerKeyContainer.findViewWithTag<TextInputLayout>("$prefix Number Sentence")
                    val solutionLayout = editAnswerKeyBinding.answerKeyContainer.findViewWithTag<TextInputLayout>("$prefix Solution/Answer")

//                    // Log existence of each layout
//                    Log.d("ViewTag", "Asked Layout found: ${askedLayout != null}")
//                    Log.d("ViewTag", "Given Layout found: ${givenLayout != null}")
//                    Log.d("ViewTag", "Operation Layout found: ${operationLayout != null}")
//                    Log.d("ViewTag", "Number Sentence Layout found: ${numberSentenceLayout != null}")
//                    Log.d("ViewTag", "Solution Layout found: ${solutionLayout != null}")

                    val answerParts = question.answer.split("\n").associate {
                        val parts = it.split(": ", limit = 2)
                        parts[0].trim() to (if (parts.size > 1) parts[1].trim() else "")
                    }

                    // Check and populate each field
                    askedLayout?.editText?.setText(answerParts["Asked"])
                    givenLayout?.editText?.setText(answerParts["Given"])
                    operationLayout?.editText?.setText(answerParts["Operation"])
                    numberSentenceLayout?.editText?.setText(answerParts["Number Sentence"])
                    solutionLayout?.editText?.setText(answerParts["Solution"])
                }
            }
        }
    }

    private fun addMultipleChoiceView(container: ViewGroup, currentItemNumber: Int) {
        val numberTextView = TextView(requireContext()).apply {
            text = "$currentItemNumber: "
            textSize = 20f // Adjust text size as needed
        }
        container.addView(numberTextView)

        val chipGroup = ChipGroup(requireContext()).apply {
            isSingleSelection = true // Only one answer can be selected
            tag = currentItemNumber // Set the tag to the current question number
        }
        for (option in listOf("A", "B", "C", "D")) {
            chipGroup.addView(Chip(requireContext()).apply {
                text = option
                isCheckable = true
            })
        }
        container.addView(chipGroup)
    }

    private fun addIdentificationView(container: ViewGroup, currentItemNumber: Int) {
        val questionLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val numberTextView = TextView(requireContext()).apply {
            text = "$currentItemNumber : "
            textSize = 20f // Adjust text size as needed
        }
        questionLayout.addView(numberTextView)

        // Create TextInputLayout for identification
        val identificationLayout = createTextInputLayout("Answer for $currentItemNumber").apply {
            tag = currentItemNumber // Set the tag to the current question number
        }
        questionLayout.addView(identificationLayout)

        container.addView(questionLayout)
    }

    private fun addWordProblemView(container: ViewGroup, currentItemNumber: Int) {
        val questionLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL // Change to vertical orientation
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val numberTextView = TextView(requireContext()).apply {
            text = "$currentItemNumber - ${currentItemNumber + 4} : "
            textSize = 20f
        }
        questionLayout.addView(numberTextView)

        val askedLayout = createTextInputLayout("Asked").apply { tag = "$currentItemNumber: Asked" }
        val givenLayout =createTextInputLayout("Given").apply { tag = "$currentItemNumber: Given" }
        val operationLayout = createTextInputLayout("Operation").apply { tag = "$currentItemNumber: Operation" }
        val numberSentenceLayout = createTextInputLayout("Number Sentence").apply { tag = "$currentItemNumber: Number Sentence" }
        val solutionLayout = createTextInputLayout("Solution/Answer").apply { tag = "$currentItemNumber: Solution/Answer" }

        // Add layouts to questionLayout
        questionLayout.addView(askedLayout)
        questionLayout.addView(givenLayout)
        questionLayout.addView(operationLayout)
        questionLayout.addView(numberSentenceLayout)
        questionLayout.addView(solutionLayout)

        container.addView(questionLayout)
    }

    private fun createTextInputLayout(hint: String): TextInputLayout {
        return TextInputLayout(requireContext()).apply {
            this.hint = hint
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.VISIBLE

            val textInputEditText = TextInputEditText(requireContext())
            this.hint = hint
            addView(textInputEditText)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun getFragmentTitle(): String {
        return "Edit ${answerSheet.name} Key"
    }

    private fun setupToolbar() {
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(activity.findViewById(R.id.myToolbar))

        val canGoBack = parentFragmentManager.backStackEntryCount > 0
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(canGoBack)
        activity.supportActionBar?.setDisplayShowHomeEnabled(canGoBack)

        activity.supportActionBar?.title = getFragmentTitle()

        activity.findViewById<Toolbar>(R.id.myToolbar).setNavigationOnClickListener {
            if (isCameraActive) {
                deactivateCamera() // Close the camera and revert UI
            } else {
                activity.onBackPressed() // Default back navigation
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (isCameraActive) {
                deactivateCamera() // Go back to the initial UI state
            } else {
                parentFragmentManager.popBackStack() // Default back navigation
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll() // Unbind camera when fragment is destroyed
        deactivateCamera() // Ensure the camera is deactivated
    }

    companion object {
        private const val REQUEST_CODE_IMAGE_PICKER = 1001  // Unique request code
    }
}
