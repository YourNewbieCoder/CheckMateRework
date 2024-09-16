package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.databinding.FragmentCheckBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.example.checkmaterework.models.AnswerSheetViewModelFactory
import com.example.checkmaterework.ui.adapters.CheckSheetsAdapter

class CheckFragment : Fragment() {

    private lateinit var checkBinding: FragmentCheckBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel
    private lateinit var checkSheetsAdapter: CheckSheetsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).answerSheetDao()
        answerSheetViewModel = ViewModelProvider(this, AnswerSheetViewModelFactory(dao))
            .get(AnswerSheetViewModel::class.java)
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
    }

    private fun onSheetSelected(sheet: AnswerSheetEntity) {
        // Fade-in animation for smooth transition to the camera preview
        checkBinding.viewFinder.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(300L)
                .setListener(null)
        }

        //Start the camera
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
            val cameraProvider: ProcessCameraProvider = cameraProviderFeature.get()

            //Preview
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = checkBinding.viewFinder.surfaceProvider
            }

            //Select Back Camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Check if camera permissions are granted
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
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