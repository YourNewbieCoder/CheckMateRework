package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.BuildConfig
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentCheckBinding
import com.example.checkmaterework.models.AnswerKeyViewModel
import com.example.checkmaterework.models.AnswerKeyViewModelFactory
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.example.checkmaterework.models.AnswerSheetViewModelFactory
import com.example.checkmaterework.models.ImageCaptureViewModel
import com.example.checkmaterework.models.ImageCaptureViewModelFactory
import com.example.checkmaterework.ui.adapters.CheckSheetsAdapter
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CheckFragment : Fragment(), ToolbarTitleProvider {

    private lateinit var checkBinding: FragmentCheckBinding

    private lateinit var answerSheetViewModel: AnswerSheetViewModel
    private lateinit var answerKeyViewModel: AnswerKeyViewModel
    private lateinit var imageCaptureViewModel: ImageCaptureViewModel

    private lateinit var checkSheetsAdapter: CheckSheetsAdapter

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).answerSheetDao()
        answerSheetViewModel = ViewModelProvider(this, AnswerSheetViewModelFactory(dao))
            .get(AnswerSheetViewModel::class.java)

        val answerKeyDao = AnswerSheetDatabase.getDatabase(requireContext()).answerKeyDao()
        answerKeyViewModel = ViewModelProvider(this, AnswerKeyViewModelFactory(answerKeyDao))
            .get(AnswerKeyViewModel::class.java)

        val imageCaptureDao = AnswerSheetDatabase.getDatabase(requireContext()).imageCaptureDao()
        imageCaptureViewModel = ViewModelProvider(this, ImageCaptureViewModelFactory(imageCaptureDao))
            .get(ImageCaptureViewModel::class.java)

        cameraExecutor = Executors.newSingleThreadExecutor()


        generativeModel = GenerativeModel(
            "gemini-1.5-flash",
            BuildConfig.apiKey,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 8192
                responseMimeType = "text/plain"
            },
        )

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        checkBinding = FragmentCheckBinding.inflate(inflater, container, false)
        return checkBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkBinding.recyclerViewCreatedSheets.layoutManager = LinearLayoutManager(requireContext())

        // Set up the adapter
        checkSheetsAdapter = CheckSheetsAdapter(
            mutableListOf(),
            onCheckClick = { sheet -> onSheetSelected(sheet) }
        )

        checkBinding.recyclerViewCreatedSheets.adapter = checkSheetsAdapter

        answerSheetViewModel.createdSheetList.observe(viewLifecycleOwner) { sheets ->
            checkSheetsAdapter.updateSheetList(sheets)
        }

        // Request camera permissions
        requestCameraPermission()
    }

    private fun onSheetSelected(sheet: AnswerSheetEntity) {
        // Load the answer keys from the database for the selected sheet
        answerKeyViewModel.loadAnswerKeysForSheet(sheet.id)

        // Observe the savedAnswerKeys LiveData to see if the correct data is retrieved
        answerKeyViewModel.savedAnswerKeys.observe(viewLifecycleOwner) { questions ->
            if (questions.isNotEmpty()) {
                Log.d("CheckFragment", "Answer key retrieved: ${questions.joinToString("\n")}")
                // Optionally, show a Toast message to confirm
                Toast.makeText(requireContext(), "Answer key retrieved successfully", Toast.LENGTH_SHORT).show()

//                // Convert the answer key into a string format to include in the prompt
//                val answerKeyText = questions.joinToString("\n") {
//                    "Question ${it.questionNumber}: ${it.answer}"
//                }
//
//                // Generate the feedback with Gemini and include the answer key
//                generateFeedbackWithGemini(sheet, answerKeyText) { feedback ->
//                    // Send the generated feedback and image to the ReviewImageFragment
//                    val reviewImageFragment = ReviewImageFragment.newInstance(
//                        sheet.id,
//                        File(photoFile.absolutePath).absolutePath,
//                        feedback
//                    )
//                    parentFragmentManager.beginTransaction()
//                        .replace(R.id.frameContainer, reviewImageFragment)
//                        .addToBackStack(null)
//                        .commit()
//                }

            } else {
                Log.d("CheckFragment", "No answer key found for the selected sheet.")
            }
        }

        // Show the back arrow and toolbar
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(activity.findViewById(R.id.myToolbar))

        // Enable the back button
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity.supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set the toolbar title if needed
        activity.supportActionBar?.title = "Check ${sheet.name}"

        // Set click listener for the back button
        activity.findViewById<Toolbar>(R.id.myToolbar).setNavigationOnClickListener {
            closeCameraAndReturn() // Handle returning to the previous state
        }

        // Fade-in animation for smooth transition to the camera preview
        checkBinding.viewFinder.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(300L).setListener(null)
        }

        // Hide the RecyclerView
        checkBinding.recyclerViewCreatedSheets.visibility = View.GONE

        // Show the "Take Picture" button
        checkBinding.buttonCheck.apply {
            visibility = View.VISIBLE
            setOnClickListener { capturePhoto(sheet) }
        }

        openCameraToCheckSheet(sheet)

    }

    private fun openCameraToCheckSheet(sheet: AnswerSheetEntity) {
        // Make sure camera permissions are granted
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFeature = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFeature.addListener({
            //Camera Provider
            cameraProvider = cameraProviderFeature.get()

            //Preview
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = checkBinding.viewFinder.surfaceProvider
            }

            //ImageCapture
            imageCapture = ImageCapture.Builder().build()

            //Select Back Camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun capturePhoto(sheet: AnswerSheetEntity) {
        val imageCapture = imageCapture ?: return
        val photoFile = File(requireContext().filesDir, "image_${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Decode the captured image as a Bitmap
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                    // Process the image for text recognition
                    processCapturedImage(bitmap, sheet, photoFile)

                    // Send recognized text to ReviewImageFragment
//                    val reviewImageFragment = ReviewImageFragment.newInstance(sheet.id, photoFile.absolutePath)
//                    parentFragmentManager.beginTransaction()
//                        .replace(R.id.frameContainer, reviewImageFragment)
//                        .addToBackStack(null)
//                        .commit()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exc)
                }
            }
        )
    }

    private fun processCapturedImage(bitmap: Bitmap, sheet: AnswerSheetEntity, photoFile: File) {
        // Apply any preprocessing steps (optional)
//        val processedImage = preprocessImage(imageBitmap)

        // Prepare multimodal input content for Gemini
//        val inputText = "This is a student's answer sheet. Evaluate the student's answers."

        // Fetch the answer key for the selected sheet
        answerKeyViewModel.loadAnswerKeysForSheet(sheet.id)

        answerKeyViewModel.savedAnswerKeys.observe(viewLifecycleOwner) { questions ->
            if (questions.isNotEmpty()) {
                // Convert the answer key into a format suitable for inclusion in the prompt
                val answerKeyText = questions.joinToString("\n") { "${it.questionNumber}: ${it.answer}" }

                generateFeedbackWithGemini(bitmap, answerKeyText) { feedback ->
                    // Send the generated feedback, including the answer key and image, to the ReviewImageFragment
                    val reviewImageFragment = ReviewImageFragment.newInstance(
                        sheet.id,
                        photoFile.absolutePath,
                        feedback
                    )
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.frameContainer, reviewImageFragment)
                        .addToBackStack(null)
                        .commit()
                }
            } else {
                Log.d("CheckFragment", "No answer key found for the selected sheet.")
            }
        }
    }

    private fun generateFeedbackWithGemini(image: Bitmap, answerKeyText: String, onFeedbackGenerated: (String) -> Unit) {
//        val chatHistory = listOf(
//            content("user") {
//                text("You are a helpful teacher who checks student answers and provides a table for the outputs")
//            },
//            content("model") {
//                text("Please tell me what you're working on! I'm ready to help you check your answers and provide a table for the outputs. \n\nTell me:\n\n* **What is the problem you're solving?** (Give me the full problem statement.)\n* **What are the inputs?** (What are the values you're working with?)\n* **What are your answers?** (Tell me what you calculated or came up with.)\n\nI'll compare your answers to the correct solution and provide a table summarizing the inputs and outputs. Let's get started! \n")
//            },
//        )

        val chatHistory = listOf(
            content("user") {
                text("You are a checker and are assigned to check student papers images and list their answer for each number")
            },
            content("model") {
                text("Please provide me with the images of the student papers. I will then be able to check the answers and list them for each number. \n\nTo make the process easier, please tell me:\n\n* **What subject is the paper for?** (e.g., Math, English, Science)\n* **What are the specific questions being asked?** (e.g., Solve for X, Write a paragraph about...)\n\nOnce I have this information, I will be able to accurately check and list the answers. \n")
            },
            content("user") {
                text("You are a test checker and assigned to check student answer sheet images and list their answer for each number ")
            },
            content("model") {
                text("Please provide me with the images of the student answer sheets. I will then be able to check the answers and list them for each number. \n\nTo help me understand the context and provide accurate results, please tell me:\n\n* **What is the subject of the test?** (e.g., Math, English, Science)\n* **What type of test is it?** (e.g., Multiple choice, True/False, Fill-in-the-blank)\n* **Are there any specific instructions for how the answers should be recorded?** (e.g., Circle the correct answer, Write the answer in the blank) \n\nOnce I have this information, I can quickly and accurately check the answers and provide you with a list of the student's responses for each question. \n")
            },
            content("user") {
                text("You are a test checker and are assigned to check student answer sheet images. You are tasked to list the student answer in each number and identify the type of exam (multiple choice, identification, or word problem) used ")
            },
            content("model") {
                text("Please provide me with the images of the student answer sheets. I'll do my best to list the student's answers for each number and identify the type of exam used (multiple choice, identification, or word problem). \n\nTo help me understand the format of the test and accurately interpret the answers, please provide the following information:\n\n* **What is the subject of the test?** (e.g., Math, English, Science)\n* **Are there any specific instructions for how the answers should be recorded?** (e.g., Circle the correct answer, Write the answer in the blank, etc.) \n\nThe more information you provide, the more accurate and helpful my analysis will be. \n")
            },
            content("user") {
                text("You are a math test checker assigned to check grade 6 student answer sheet images and create the table for answers in each number. ")
            },
            content("model") {
                text("Okay! I'm ready to check those math answer sheets. Please provide the images of the student answer sheets. I'll create a table with the following information:\n\n* **Question Number:**  The number of the question from the test.\n* **Student Answer:**  The answer provided by the student.\n* **Correct Answer:** The correct answer for the question. \n* **Points Awarded:**  If applicable, the number of points earned by the student for that question.\n\nTo help me accurately assess the answers, please provide the following information:\n\n* **What grade level is the test for?** This helps me understand the expected complexity of the math concepts.\n* **Are there any specific instructions for how answers should be recorded?** (e.g., Circle the correct answer, Write the answer in the blank, Show your work, etc.) \n* **Is there an answer key available?** This will be very helpful to ensure accurate grading.\n\nOnce I have this information, I can quickly create a clear and organized table of the student's answers for each question. \n")
            },
            content("user") {
                text("You are a math test checker assigned to check grade 6 student answer sheet images and create the table for answers in each number. There are three types of examination you will check (multiple choice, identification, and word problems). If the test paper is Multiple choice, you will map the shaded answers to each number. If it is Identification you will map the answers in the box to each number and if it is a Word Problem (clue: Asked, Given, Operation, Number Sentence, Solution/Answer) you will map each component to the 5 point number (1-5) ")
            },
            content("model") {
                text("Okay, I'm ready to start checking those Grade 6 math answer sheets! Please provide me with the images of the student answer sheets, and I will create a table for the answers for each number. \n\nHere's how I will structure my tables for each type of question:\n\n**1. Multiple Choice:**\n\n| Question Number | Shaded Answer | Correct Answer | Points Awarded |\n|---|---|---|---|\n\n**2. Identification:**\n\n| Question Number | Student Answer | Correct Answer | Points Awarded |\n|---|---|---|---|\n\n**3. Word Problem:**\n\n| Question Number | Component | Student Answer | Correct Answer | Points Awarded |\n|---|---|---|---|---|\n|  |  Asked:  |   |  |  |\n|  | Given: |  |  |  |\n|  | Operation: |  |  |  |\n|  | Number Sentence: |  |  |  |\n|  | Solution/Answer: |  |  |  |\n\n**To help me accurately check the answers, please tell me:**\n\n* **What grade level is the test for?**  (You said Grade 6, but this helps confirm.)\n* **Is there an answer key available?**  This will be very helpful!\n* **For word problems, are there any specific guidelines for the \"Asked\" and \"Given\" sections?**  For example, do students need to write a complete sentence?\n\nOnce you provide me with the images and this information, I can get started creating the tables. \n")
            },
            content("user") {
                text("You are a math test checker assigned to check grade 6 student answer sheet images and create the table for answers in each number. There are three types of examination you will check (multiple choice, identification, and word problems). If the test paper is Multiple choice, you will map the shaded answers to each number. If it is Identification you will map the answers in the box to each number and if it is a Word Problem (clue: Asked, Given, Operation, Number Sentence, Solution/Answer) you will map each component to the 5-point number (1-5). The pointing system is 1 point for each number expect for Word Problem which is 5 points (1 pt for each component). You can consider if the answer is close to the answer for example in a word problem the answer for Asked is the number of fruits left, you can consider if the student's answer is fruit left")
            },
            content("model") {
                text("Okay, I understand the grading system!  I'm ready to check those Grade 6 math answer sheets. Please provide me with the images of the student answer sheets, and I'll create a table for each question.\n\nHere's how I'll structure the tables and handle the grading:\n\n**1. Multiple Choice:**\n\n| Question Number | Shaded Answer | Correct Answer | Points Awarded |\n|---|---|---|---|\n\n- **Points Awarded:** 1 point for each correct answer.\n\n**2. Identification:**\n\n| Question Number | Student Answer | Correct Answer | Points Awarded |\n|---|---|---|---|\n\n- **Points Awarded:** 1 point for each correct answer.\n\n**3. Word Problem:**\n\n| Question Number | Component | Student Answer | Correct Answer | Points Awarded |\n|---|---|---|---|---|\n|  | 1. Asked:  |   |  |  1  |\n|  | 2. Given: |  |  |  1  |\n|  | 3. Operation: |  |  |  1  |\n|  | 4. Number Sentence: |  |  |  1  |\n|  | 5. Solution/Answer: |  |  |  1  |\n\n- **Points Awarded:** 1 point for each component correctly identified and answered.\n- **Partial credit:** I will consider partial credit for the \"Asked\" section of word problems if the student's answer reflects an understanding of the question's core concept, even if not perfectly phrased. For example, if the \"Asked\" is \"How many fruits are left?\" and the student writes \"Number of fruits left,\"  I will give them the point.\n\n**To make sure I grade accurately, please provide:**\n\n* **Images of the student answer sheets:** \n* **An answer key:**  This is essential!\n* **Any specific instructions about how to handle partial credit or special cases:** (For example, should I award points for work shown even if the final answer is incorrect?)\n\nI'm ready to get started! \n")
            },
            content("user") {
                text("You are a math test checker assigned to check grade 6 student answer sheet images and create the table for answers in each number. There are three types of examination you will check (multiple choice, identification, and word problems). If the test paper is Multiple choice, you will map the shaded answers to each number. If it is Identification you will map the answers in the box to each number and if it is a Word Problem (clue: Asked, Given, Operation, Number Sentence, Solution/Answer) you will map each component to the 5-point number (1-5). The pointing system is 1 point for each number except for Word Problem which is 5 points (1 pt for each component). You can consider if the answer is close to the answer for example in a word problem the answer for Asked is the number of fruits left, you can consider if the student's answer is fruit left or in the final answer you can consider it there is no unit but don't check it if the answer is 84 but the student's final is 36 ")
            },
            content("model") {
                text("Okay, I'm ready to check those Grade 6 math answer sheets! I'll be very careful to follow your specific instructions about grading and partial credit. \n\nHere's a summary of how I'll approach the grading:\n\n**Multiple Choice:**\n\n| Question Number | Shaded Answer | Correct Answer | Points Awarded |\n|---|---|---|---|\n\n- **Points Awarded:** 1 point for each correct answer.\n\n**Identification:**\n\n| Question Number | Student Answer | Correct Answer | Points Awarded |\n|---|---|---|---|\n\n- **Points Awarded:** 1 point for each correct answer.\n\n**Word Problem:**\n\n| Question Number | Component | Student Answer | Correct Answer | Points Awarded |\n|---|---|---|---|---|\n|  | 1. Asked:  |   |  |  1  |\n|  | 2. Given: |  |  |  1  |\n|  | 3. Operation: |  |  |  1  |\n|  | 4. Number Sentence: |  |  |  1  |\n|  | 5. Solution/Answer: |  |  |  1  |\n\n- **Points Awarded:** 1 point for each component correctly identified and answered.\n- **Partial credit for \"Asked\":** I'll give 1 point if the student's answer reflects the core concept of the question, even if not perfectly phrased. For example, \"Number of fruits left\" would get credit for \"How many fruits are left?\"\n- **Partial credit for \"Solution/Answer\":** \n    - I'll give 1 point if the answer is close, but missing a unit (e.g., \"84\" instead of \"84 apples\"). \n    - I will NOT give credit if the answer is significantly off (e.g., \"36\" instead of \"84\").\n\n**To get started, please provide:**\n\n* **Images of the student answer sheets:** \n* **An answer key:** This is crucial for accurate grading!\n* **Any additional guidance:**  Are there any specific instructions about how to handle other types of partial credit or errors? \n\nI'm ready to analyze those answer sheets and provide you with the results.  \n")
            },
            content("user") {
                text("You are a math test checker assigned to check grade 6 student answer sheet images and create the table for answers in each number. There are three types of examination you will check (multiple choice, identification, and word problems). If the test paper is Multiple choice, you will map the shaded answers to each number. If it is Identification you will map the answers in the box to each number and if it is a Word Problem (clue: Asked, Given, Operation, Number Sentence, Solution/Answer) you will map each component to the 5-point number (1-5). The pointing system is 1 point for each number except for Word Problem which is 5 points (1 pt for each component). You can consider if the answer is close to the answer for example in a word problem the answer for Asked is the number of fruits left, you can consider if the student's answer is fruit left or in the final answer you can consider it there is no unit but don't check it if the answer is 84 but the student's final is 36. After that identify the student's name and section (clue: \"Name:\" for name, and \"Class/Section:\" for the section) of the student paper and create another table for records and assign the score they got.")
            },
            content("model") {
                text("Okay, I'm ready to check those Grade 6 math answer sheets! I'll carefully follow your instructions about grading and partial credit, and I'll create tables for both the answers and the student records.\n\n**Here's the breakdown:**\n\n**1. Answer Tables:**\n\n**Multiple Choice:**\n\n| Question Number | Shaded Answer | Correct Answer | Points Awarded |\n|---|---|---|---|\n\n**Identification:**\n\n| Question Number | Student Answer | Correct Answer | Points Awarded |\n|---|---|---|---|\n\n**Word Problem:**\n\n| Question Number | Component | Student Answer | Correct Answer | Points Awarded |\n|---|---|---|---|---|\n|  | 1. Asked:  |   |  |  1  |\n|  | 2. Given: |  |  |  1  |\n|  | 3. Operation: |  |  |  1  |\n|  | 4. Number Sentence: |  |  |  1  |\n|  | 5. Solution/Answer: |  |  |  1  |\n\n**2. Student Records Table:**\n\n| Student Name | Class/Section | Total Score |\n|---|---|---|\n\n**Grading Criteria:**\n\n* **Multiple Choice & Identification:** 1 point per correct answer.\n* **Word Problem:** 1 point per correctly answered component (\"Asked,\" \"Given,\" \"Operation,\" \"Number Sentence,\" \"Solution/Answer\"). \n* **Partial Credit for \"Asked\":** 1 point if the student's answer reflects the core concept, even if not perfectly phrased.\n* **Partial Credit for \"Solution/Answer\":** 1 point if the answer is close, but missing a unit. No credit if the answer is significantly off.\n\n**To get started, please provide:**\n\n1. **Images of the student answer sheets:** \n2. **An answer key:**  Essential for accurate grading!\n3. **Any additional guidance:**  Specific instructions about handling partial credit or other errors? \n\n**Once I have all this information, I'll create the tables, analyze the answer sheets, and calculate the scores. I'll then summarize the student records in the \"Student Records Table.\"** \n")
            },
        )

        // Prepare the input content (image and text)
        val inputContent = content {
            image(image) // Include the captured image
            text("This is the student paper, identify first the exam type and then create the table for answers in each number, compare that to the answer key I provided and give a score considering the closeness of student answer to the answer key. After that provide the table for records with the identified name, section and the score this student get.")
            text("Answer Key:\n$answerKeyText") // Include the answer key

//            text("This is a sample image can you describe what things do you see?")
        }

        val chat = generativeModel.startChat(chatHistory)

        // Call from a coroutine to send the request to Gemini and collect the response
        lifecycleScope.launch {
            try {
                val response = chat.sendMessage(inputContent)
                val feedback = response.text?: "No feedback generated."
                onFeedbackGenerated(feedback)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating feedback", e)
                onFeedbackGenerated("Error generating feedback.")
            }
        }
    }

//    private fun recognizeTextFromImage(imageBitmap: Bitmap, onTextRecognized: (String) -> Unit) {
//        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//        val inputImage = InputImage.fromBitmap(imageBitmap, 0)
//
//        recognizer.process(inputImage)
//            .addOnSuccessListener { visionText ->
//                val recognizedText = visionText.text
//                onTextRecognized(recognizedText)
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Error recognizing text: ", e)
//            }
//    }

//    private fun matchAnswers(recognizedAnswers: Map<Int, String>, answerKeys: List<QuestionEntity>): List<Pair<Int, Boolean>> {
//        val results = mutableListOf<Pair<Int, Boolean>>()
//
//        for (question in answerKeys) {
//            val recognizedAnswer = recognizedAnswers[question.questionNumber]
//            val isCorrect = recognizedAnswer == question.answer
//            results.add(Pair(question.questionNumber, isCorrect))
//        }
//
//        return results
//    }
//
//    private fun showResults(results: List<Pair<Int, Boolean>>) {
//        val resultString = results.joinToString("\n") { (question, isCorrect) ->
//            "Q$question: ${if (isCorrect) "Correct" else "Incorrect"}"
//        }
//        Toast.makeText(requireContext(), resultString, Toast.LENGTH_LONG).show()
//    }

//    private fun processCapturedImage(imageBitmap: Bitmap): String {
//        // Step 1: Apply any image preprocessing (e.g., grayscale, resizing, rotation, etc.)
//        val processedImage = preprocessImage(imageBitmap)
//
//        // Step 2: Perform text recognition using ML Kit or other OCR library
//        val recognizedText = recognizeTextFromImage(processedImage)
//
//        // Step 3: Parse the recognized text
////        return parseRecognizedText(recognizedText)
//        return recognizedText
//    }

//    private fun preprocessImage(imageBitmap: Bitmap): Bitmap {
//        // Example: convert to grayscale
//        // You can also apply OpenCV transformations here
//        return imageBitmap // Adjust the image as per requirement
//    }

//    private fun recognizeTextFromImage(imageBitmap: Bitmap): String {
//        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//        val image = InputImage.fromBitmap(imageBitmap, 0) // 0 is the default rotation
//
//        var recognizedText = ""
//        recognizer.process(image)
//            .addOnSuccessListener { visionText ->
//                recognizedText = visionText.text
//            }
//            .addOnFailureListener { e ->
//                // Handle error
//                Log.e("OCR", "Error recognizing text: ", e)
//            }
//
//        return recognizedText // Ensure this is handled correctly in real-time
//    }


    private fun closeCameraAndReturn() {
        // Unbind all use cases to stop the camera
        cameraProvider?.unbindAll()

        // Hide the PreviewView and Back button
        checkBinding.viewFinder.visibility = View.GONE

        // Show the RecyclerView
        checkBinding.recyclerViewCreatedSheets.visibility = View.VISIBLE

        checkBinding.buttonCheck.visibility = View.GONE

        // Hide the toolbar and back arrow
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // Set the toolbar title if needed
        activity.supportActionBar?.title = getString(R.string.check_title)
    }

    // Check if camera permissions are granted
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Handle permissions result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.check_title)
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

    // Constants for permissions
    companion object {
        private const val TAG = "CameraXCheckSheet"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}