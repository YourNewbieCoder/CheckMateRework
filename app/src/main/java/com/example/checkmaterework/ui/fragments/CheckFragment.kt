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
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                Log.d("CheckFragment", "Recognized text: $recognizedText")
                val reviewImageFragment = ReviewImageFragment.newInstance(sheet.id, photoFile.absolutePath, recognizedText)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frameContainer, reviewImageFragment)
                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed", e)
            }

//        // Perform text recognition
//        recognizeTextFromImage(imageBitmap) { recognizedText ->
//            // Parse the recognized text
//            val parsedAnswers = parseRecognizedText(recognizedText)
//
////            // Match answers with the stored answer key
////            answerKeyViewModel.savedAnswerKeys.observe(viewLifecycleOwner) { answerKeys ->
////                val results = matchAnswers(parsedAnswers, answerKeys)
////
////                // Show results in UI or proceed to the next step (e.g., saving, review)
////                showResults(results)
////            }
//
//            // Compare the answers with the correct answer key
//            compareAnswers(sheet.id, parsedAnswers)
//        }
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