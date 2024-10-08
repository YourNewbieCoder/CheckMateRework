package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentEditAnswerKeyBinding
import com.example.checkmaterework.models.AnswerSheetEntity
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

    // ViewModel for handling text recognition
    private lateinit var textRecognitionViewModel: TextRecognitionViewModel


    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the ViewModel
        textRecognitionViewModel = ViewModelProvider(this)[TextRecognitionViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        editAnswerKeyBinding = FragmentEditAnswerKeyBinding.inflate(inflater, container, false)
        return editAnswerKeyBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up button click for adding key with camera
        editAnswerKeyBinding.buttonAddKeyWithCamera.setOnClickListener {
//            checkCameraPermissionAndStart()
            showImageSourceOptionsDialog()
        }

        // Observe the recognized text from the ViewModel and update the UI
        textRecognitionViewModel.recognizedText.observe(viewLifecycleOwner) { recognizedText ->
        }

        // Set up button click for scanning the key
        editAnswerKeyBinding.buttonScan.setOnClickListener {
            deactivateCamera()

            val recognizedText = textRecognitionViewModel.recognizedText.value ?: "No text recognized"
            // Navigate to ScannedKeyFragment
            val fragment = ScannedKeyFragment.newInstance(recognizedText)
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Here you can set up your view logic, such as loading the data of the answer sheet
        loadAnswerKeyData(answerSheet)
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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
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
            .build()
            .also { it.surfaceProvider = editAnswerKeyBinding.viewFinder.surfaceProvider }

        // Image analysis use case
        val imageAnalyzer = ImageAnalysis.Builder()
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), YourImageAnalyzer(textRecognitionViewModel))
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider?.unbindAll() // Unbind any previous use cases before rebinding
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error binding camera preview: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deactivateCamera() {
        isCameraActive = false // Set the camera state to inactive
        editAnswerKeyBinding.viewFinder.visibility = View.GONE // Hide camera preview
        editAnswerKeyBinding.buttonScan.visibility = View.GONE // Hide the "Check Paper" button

        cameraProvider?.unbindAll()

        // Reset the toolbar title to the original title
        setupToolbarTitle(getFragmentTitle()) // Assuming this method sets the toolbar title
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

    private fun loadAnswerKeyData(answerSheet: AnswerSheetEntity) {
        editAnswerKeyBinding.textViewSheetNameKey.text = answerSheet.name // Set the name of the answer sheet

        val answerKeyContainer = editAnswerKeyBinding.answerKeyContainer // Get the container for answer key items

        var currentItemNumber = 1

        for ((examType, itemCount) in answerSheet.examTypesList) {
            when (examType) {
                "Multiple Choice" -> {
                    for (i in 1..itemCount) {
                        addMultipleChoiceView(answerKeyContainer, currentItemNumber)
                        currentItemNumber++
                    }
                }
                "Identification" -> {
                    for (i in 1..itemCount) {
                        addIdentificationView(answerKeyContainer, currentItemNumber)
                        currentItemNumber++

                    }
                }
                "Word Problem" -> {
                    val wordProblemsCount = itemCount / 5
                    for (i in 1..wordProblemsCount) {
                        addWordProblemView(answerKeyContainer, currentItemNumber)
                        currentItemNumber += 5
                    }
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
        val identificationLayout = createTextInputLayout("Answer for $currentItemNumber")
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

        val askedLayout = createTextInputLayout("Asked")
        val givenLayout =createTextInputLayout("Given")
        val operationLayout = createTextInputLayout("Operation")
        val numberSentenceLayout = createTextInputLayout("Number Sentence")
        val solutionLayout = createTextInputLayout("Solution/Answer")

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

            // Show the "Proceed" button
            editAnswerKeyBinding.buttonProceedWithImage.visibility = View.VISIBLE

            // Set click listener on the "Proceed" button
            editAnswerKeyBinding.buttonProceedWithImage.setOnClickListener {
                recognizeTextFromBitmap(bitmap) // Call text recognition here
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(requireContext(), "File not found: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun processImageFromURI(uri: Uri) {
//        try {
//            val inputStream = requireContext().contentResolver.openInputStream(uri)
//            val bitmap = BitmapFactory.decodeStream(inputStream)
//
//            // Now pass the bitmap to your text recognition process
//            recognizeTextFromBitmap(bitmap)
//        } catch (e: FileNotFoundException) {
//            Toast.makeText(requireContext(), "File not found: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//
//    }

    private fun recognizeTextFromBitmap(bitmap: Bitmap?) {
        val image = bitmap?.let { InputImage.fromBitmap(it, 0) }

        // Use ML Kit's text recognition with the image
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        if (image != null) {
            recognizer.process(image).addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                textRecognitionViewModel.setRecognizedText(recognizedText)

                // Navigate to ScannedKeyFragment after successful recognition
                val fragment = ScannedKeyFragment.newInstance(recognizedText)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frameContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.edit_key_title)
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

    companion object {
        private const val REQUEST_CODE_IMAGE_PICKER = 1001  // Unique request code
    }
}
