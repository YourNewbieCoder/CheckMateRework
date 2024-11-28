package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
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
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
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

    private lateinit var imageViewSelected: ImageView
    private lateinit var buttonCheckPaper: Button

    private var selectedSheet: AnswerSheetEntity? = null

    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModels()

        cameraExecutor = Executors.newSingleThreadExecutor()

        generativeModel = GenerativeModel(
            "gemini-1.5-flash",
            BuildConfig.apiKey,
            generationConfig = generationConfig {
                temperature = 0.4f
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

    private val modelResponses = listOf(
        """
    Name: Ayumu Uehara
    Class/Section: 6-Sampaguita

    1. (No answer provided)
    2. A
    3. D
    4. (No answer provided)
    5. B
    6. (No answer provided)
    7. 235.20
    8. 4/20
    9. (No answer provided)
    10. 15/20
    11. (No answer provided)
    12. d908
    13. 2730
    14. 284°
    15. 9/8
    16. Addition
    17. Sur 99
    18. (No answer provided)
    19. (No answer provided)
    20. (No answer provided)
    """,
        """
    **Name:** Chisato Araoshi
    **Class/Section:** 6-Jade

    **1-5.**
    **Asked (A):** How many chairs is taken away or left
    **Given (G):** 19 chairs, 12 chairs
    **Operation (O):** (No answer provided)
    **Number Sentence (N):** (No answer provided)
    **Solution/Answer (A):** 19 - 12 = 7 chairs
    There are 7 chairs left in the auditorium.

    **6-10.**
    **Asked (A):** (No answer provided)
    **Given (G):** (No answer provided)
    **Operation (O):** Division
    **Number Sentence (N):** 81 / 9 = N
    **Solution/Answer (A):** 81 ÷ 9 = 9
    There are 9 tables needed to collect all the fruits baskets.
    """,
        """
    **Name:** Karin Asaka
    **Class/Section:** Ilang-Ilang
    **Date:** July 4, 1989

    1.  (No answer provided)
    2.  D
    3.  (No answer provided)
    4.  B
    5.  (No answer provided)
    6.  150%
    7.  54
    8.  10 ½
    9.  63 ²/₁₀
    10. (No answer provided)
    11. 90²
    12. 4⁶
    13. (No answer provided)
    14. 3 x 3 x 3
    15. 1021
    16. (No answer provided)
    17. 0193
    18. 9321
    19. (No answer provided)
    20. 3451
    """
        // Add more responses as needed
    )

    private val imageResourceIds = listOf(
        R.drawable.uehara_4th_summative_test,
        R.drawable.arashi_1st_seatwork,
        R.drawable.asaka_4th_summative_test
    )

    // Function to decode an image resource into a Bitmap
    fun decodeImageResource(context: Context?, resId: Int): Bitmap? {
        return context?.let { BitmapFactory.decodeResource(it.resources, resId) }
    }

    private fun generateChatHistoryWithResponses(context: Context?): List<Content> {
        val chatHistory = mutableListOf<Content>()

        // Initial user input
        chatHistory.add(content("user") {
            text(
                """
                You are a math test checker assigned to check grade 6 student answer sheet images and map the answers in each number. 
                There are three types of examination you will check (multiple choice, identification, and word problems). 
                If the test paper is Multiple Choice, you will map the shaded answers to each number. 
                If it is Identification, you will map the answers in the box to each number. 
                If it is a Word Problem (clue: Asked, Given, Operation, Number Sentence, Solution/Answer) you will map it like this:
                
                1.
                Asked: HOW MANY KILOGRAMS OF POTATOES REMAIN?
                Given: Potatoes = 850 and 320
                Operation: SUBTRACTION
                Number Sentence: 850 - 320
                Solution: 850 - 320 = 530
                530 kgs. OF POTATOES REMAIN.

                After that, identify the student's name and section (clue: "Name:" for name, and "Class/Section:" for the section) 
                of the student paper and map it like this:
                
                Name: 
                Class/Section:
                """
            )
        })

        // Initial model response
        chatHistory.add(
            content("model") {
                text("Please provide the images of the student answer sheets. I need to see the images to extract the answers, name, and section. I will then format the output as requested.")
            }
        )

        // Loop through images and generate corresponding chat content
        imageResourceIds.forEachIndexed { index, resId ->
            val imageBitmap  = decodeImageResource(context, resId)

            // Add the image content
            chatHistory.add(content("user") {
                imageBitmap?.let { image(it) }
            })

            // Add the corresponding model response
            chatHistory.add(content("model") {
                text(modelResponses[index])
            })
        }

        return chatHistory
    }

    private fun generateFeedbackWithGemini(image: Bitmap, onFeedbackGenerated: (String) -> Unit) {

        val chatHistory = generateChatHistoryWithResponses(context)

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