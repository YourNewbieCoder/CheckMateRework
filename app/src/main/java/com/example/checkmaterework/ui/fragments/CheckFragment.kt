package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import com.example.checkmaterework.network.AnswerSheetHelper
import com.example.checkmaterework.ui.adapters.CheckSheetsAdapter
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.launch
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

    private lateinit var imageViewSelected: ImageView
    private lateinit var buttonCheckPaper: Button

    private var selectedSheet: AnswerSheetEntity? = null

    private lateinit var generativeModel: GenerativeModel

    private lateinit var answerSheetHelper: AnswerSheetHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModels()

        cameraExecutor = Executors.newSingleThreadExecutor()

        generativeModel = GenerativeModel(
            "gemini-2.0-flash-exp",
            BuildConfig.apiKey,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 8192
                responseMimeType = "text/plain"
            },
        )

        answerSheetHelper = AnswerSheetHelper()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        checkBinding = FragmentCheckBinding.inflate(inflater, container, false)
        return checkBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        // Initialize the ImageView and "Check Paper" button
        imageViewSelected = view.findViewById(R.id.imageViewSelected)
        buttonCheckPaper = view.findViewById(R.id.buttonCheck)

        // Initially hide the image view and button
        imageViewSelected.visibility = View.GONE
        buttonCheckPaper.visibility = View.GONE

        buttonCheckPaper.setOnClickListener { checkPaper() }

        // Request camera permissions
        requestCameraPermission()
    }

    private fun initializeViewModels() {
        val database = AnswerSheetDatabase.getDatabase(requireContext())
        answerSheetViewModel = ViewModelProvider(this, AnswerSheetViewModelFactory(database.answerSheetDao()))
            .get(AnswerSheetViewModel::class.java)

        answerKeyViewModel = ViewModelProvider(this, AnswerKeyViewModelFactory(database.answerKeyDao()))
            .get(AnswerKeyViewModel::class.java)

        imageCaptureViewModel = ViewModelProvider(this, ImageCaptureViewModelFactory(database.imageCaptureDao()))
            .get(ImageCaptureViewModel::class.java)
    }

    private fun setupRecyclerView() {
        checkBinding.recyclerViewCreatedSheets.layoutManager = LinearLayoutManager(requireContext())

        // Set up the adapter
        checkSheetsAdapter = CheckSheetsAdapter(mutableListOf(),
            onCheckClick = { sheet -> showImageSourceOptionsDialog(sheet) }
        )
        checkBinding.recyclerViewCreatedSheets.adapter = checkSheetsAdapter

        answerSheetViewModel.createdSheetList.observe(viewLifecycleOwner) { sheets ->
            checkSheetsAdapter.updateSheetList(sheets)
        }
    }

    private fun showImageSourceOptionsDialog(sheet: AnswerSheetEntity) {
        selectedSheet = sheet
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose an Option").setItems(options) { _, which ->
            when (which) {
                0 -> checkCameraPermissionAndStart(sheet)
                1 -> openImagePicker(sheet)
            }
        }
        builder.show()
    }

    private fun checkCameraPermissionAndStart(sheet: AnswerSheetEntity) {
        if (allPermissionsGranted()) onSheetSelected(sheet)
        else requestCameraPermission()
    }

    private fun openImagePicker(sheet: AnswerSheetEntity) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
            val selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                processGalleryImage(selectedImageUri)
            } else {
                Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processGalleryImage(imageUri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                showImagePreview(bitmap)
            } else {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from gallery", e)
            Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onSheetSelected(sheet: AnswerSheetEntity) {
        // Load the answer keys from the database for the selected sheet
        answerKeyViewModel.loadAnswerKeysForSheet(sheet.id)

        // Observe the savedAnswerKeys LiveData to see if the correct data is retrieved
        answerKeyViewModel.savedAnswerKeys.observe(viewLifecycleOwner) { questions ->
            if (questions.isNotEmpty()) {
//                Log.d("CheckFragment", "Answer key retrieved: ${questions.joinToString("\n")}")
                Toast.makeText(requireContext(), "Answer key retrieved successfully", Toast.LENGTH_SHORT).show()
            } else {
//                Log.d("CheckFragment", "No answer key found for the selected sheet.")
                Toast.makeText(requireContext(), "No answer key found for the selected sheet.", Toast.LENGTH_SHORT).show()
            }
        }

        showCameraPreview(sheet)
    }

    private fun showCameraPreview(sheet: AnswerSheetEntity) {
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
        checkBinding.buttonScan.apply {
            visibility = View.VISIBLE
            setOnClickListener { capturePhoto(sheet) }
        }

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFeature = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFeature.addListener({
            //Camera Provider
            cameraProvider = cameraProviderFeature.get()

            //Preview
            val preview = Preview.Builder().build().apply {
                surfaceProvider = checkBinding.viewFinder.surfaceProvider
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

    private fun capturePhoto(sheet: AnswerSheetEntity?) {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        // Convert ImageProxy to Bitmap and handle rotation
                        val rotationDegrees = image.imageInfo.rotationDegrees
                        val bitmap = image.toBitmap().rotate(rotationDegrees)

                        // Display the rotated Bitmap
                        showImagePreview(bitmap)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing captured image", e)
                        Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show()
                    } finally {
                        // Close the ImageProxy to free resources
                        image.close()
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exc)
                    Toast.makeText(requireContext(), "Failed to capture image: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun Bitmap.rotate(degrees: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

//    private fun capturePhoto(sheet: AnswerSheetEntity?) {
//        val imageCapture = imageCapture ?: return
//        val photoFile = File(requireContext().filesDir, "image_${System.currentTimeMillis()}.jpg")
//
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//        imageCapture.takePicture(
//            outputOptions, ContextCompat.getMainExecutor(requireContext()),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                    // Decode the captured image as a Bitmap
//                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
//                    capturedPhotoFile = photoFile // Store the photo file
//
//                    if (bitmap != null) {
//                        // Display the captured image
//                        showImagePreview(bitmap)
//                    } else {
//                        Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Image capture failed", exc)
//                }
//            }
//        )
//    }

    private fun showImagePreview(bitmap: Bitmap) {
        checkBinding.imageViewSelected.apply {
            setImageBitmap(bitmap)
            visibility = View.VISIBLE
        }
        checkBinding.buttonCheck.visibility = View.VISIBLE

        // Hide the camera preview and button
        checkBinding.viewFinder.visibility = View.GONE
        checkBinding.buttonScan.visibility = View.GONE

        // Hide the RecyclerView
        checkBinding.recyclerViewCreatedSheets.visibility = View.GONE
        checkBinding.textCheckSheet.visibility = View.GONE
    }

    private fun checkPaper() {
        val bitmap = (imageViewSelected.drawable as? BitmapDrawable)?.bitmap
        if (bitmap != null) {
            val sheet = selectedSheet

            if (sheet != null) {
                processCapturedImage(bitmap, sheet)
            } else {
                Toast.makeText(requireContext(), "No answer sheet selected", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "No image to process", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processCapturedImage(bitmap: Bitmap, sheet: AnswerSheetEntity?) {
        generateFeedbackWithGemini(bitmap) { feedback ->
            // Send the generated feedback, including the answer key and image, to the ReviewImageFragment
            val reviewImageFragment = ReviewImageFragment.newInstance(
                sheet!!.id,
                feedback
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, reviewImageFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun generateFeedbackWithGemini(image: Bitmap, onFeedbackGenerated: (String) -> Unit) {

        val chatHistory = answerSheetHelper.generateChatHistoryWithResponses(context)

        // Prepare the input content (image and text)
        val inputContent = content {
            image(image) // Include the captured image
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