package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentEditAnswerKeyBinding
import com.example.checkmaterework.models.AnswerSheetEntity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditAnswerKeyFragment(private val answerSheet: AnswerSheetEntity) : Fragment(), ToolbarTitleProvider {

    private lateinit var editAnswerKeyBinding: FragmentEditAnswerKeyBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var isCameraActive = false

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        editAnswerKeyBinding = FragmentEditAnswerKeyBinding.inflate(inflater, container, false)
        return editAnswerKeyBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up button click for adding key with camera
        editAnswerKeyBinding.buttonAddKeyWithCamera.setOnClickListener {
            checkCameraPermissionAndStart()
        }

        // Here you can set up your view logic, such as loading the data of the answer sheet
        loadAnswerKeyData(answerSheet)
    }

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        isCameraActive = true // Set the camera state to active
        editAnswerKeyBinding.viewFinder.visibility = View.VISIBLE // Show camera preview
        editAnswerKeyBinding.buttonCheck.visibility = View.VISIBLE // Show the "Check Paper" button

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraPreview()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraPreview() {
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(editAnswerKeyBinding.viewFinder.surfaceProvider) }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider?.unbindAll() // Unbind any previous use cases before rebinding
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error binding camera preview: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deactivateCamera() {
        isCameraActive = false // Set the camera state to inactive
        editAnswerKeyBinding.viewFinder.visibility = View.GONE // Hide camera preview
        editAnswerKeyBinding.buttonCheck.visibility = View.GONE // Hide the "Check Paper" button
//        editAnswerKeyBinding.editKeyLayout.visibility = View.VISIBLE // Show the answer key items
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

//        if (canGoBack) {
//            activity.findViewById<Toolbar>(R.id.myToolbar).setNavigationOnClickListener {
//                activity.onBackPressed()
//            }
//        }
        activity.findViewById<Toolbar>(R.id.myToolbar).setNavigationOnClickListener {
            if (isCameraActive) {
                deactivateCamera() // Close the camera and revert UI
            } else {
                activity.onBackPressed() // Default back navigation
            }
        }
    }
}
