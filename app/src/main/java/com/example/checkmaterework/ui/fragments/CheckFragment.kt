package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.example.checkmaterework.models.AnswerSheetViewModelFactory
import com.example.checkmaterework.models.ImageCaptureViewModel
import com.example.checkmaterework.models.ImageCaptureViewModelFactory
import com.example.checkmaterework.models.Student
import com.example.checkmaterework.ui.adapters.CheckSheetsAdapter
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CheckFragment : Fragment() {

    private lateinit var checkBinding: FragmentCheckBinding

    private lateinit var answerSheetViewModel: AnswerSheetViewModel
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
        // Show the back arrow and toolbar
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(activity.findViewById(R.id.myToolbar))

        // Enable the back button
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity.supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set the toolbar title if needed
        activity.supportActionBar?.title = getString(R.string.check_title)

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
                    val reviewImageFragment = ReviewImageFragment.newInstance(sheet.id, photoFile.absolutePath)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.frameContainer, reviewImageFragment)
                        .addToBackStack(null)
                        .commit()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exc)
                }
            }
        )
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

    // Constants for permissions
    companion object {
        private const val TAG = "CameraXCheckSheet"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}