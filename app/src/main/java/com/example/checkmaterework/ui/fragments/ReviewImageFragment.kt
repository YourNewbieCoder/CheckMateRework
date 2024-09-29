package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide

import com.example.checkmaterework.databinding.FragmentReviewImageBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.ImageCaptureEntity
import com.example.checkmaterework.models.ImageCaptureViewModel
import com.example.checkmaterework.models.ImageCaptureViewModelFactory
import com.example.checkmaterework.models.ReviewImageViewModel
import com.example.checkmaterework.models.ReviewImageViewModelFactory
import com.example.checkmaterework.models.StudentEntity
import kotlinx.coroutines.launch

class ReviewImageFragment : Fragment() {

    private lateinit var reviewBinding: FragmentReviewImageBinding
    private lateinit var imageCaptureViewModel: ImageCaptureViewModel
    private lateinit var reviewImageViewModel: ReviewImageViewModel
    private var sheetId: Int = 0
    private lateinit var imagePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            sheetId = it.getInt(ARG_SHEET_ID)
            imagePath = it.getString(ARG_IMAGE_PATH) ?: ""
        }

        val dao = AnswerSheetDatabase.getDatabase(requireContext()).imageCaptureDao()
        val studentDao = AnswerSheetDatabase.getDatabase(requireContext()).studentDao()
        val classDao = AnswerSheetDatabase.getDatabase(requireContext()).classDao()
        val answerSheetDao = AnswerSheetDatabase.getDatabase(requireContext()).answerSheetDao()

        reviewImageViewModel = ViewModelProvider(this, ReviewImageViewModelFactory(dao, studentDao, classDao, answerSheetDao))
            .get(ReviewImageViewModel::class.java)
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
                Log.d("Student Info", "Last Name: ${it.lastName}, First Name: ${it.firstName}")
            }
        }

        // Load captured image using Glide or similar library
        Glide.with(this).load(imagePath).into(reviewBinding.imageViewCaptured)

        // Set save button listener
        reviewBinding.buttonSave.setOnClickListener {
            saveImageCapture()
        }
    }

    private fun saveImageCapture() {
        val lastName = reviewBinding.textInputLastName.text.toString()
        val firstName = reviewBinding.textInputFirstName.text.toString()
        val score = reviewBinding.textInputScore.text.toString().toIntOrNull() ?: 0
        val sectionName = reviewBinding.textInputSection.text.toString()

        // Step 1: Create or retrieve the StudentEntity
        lifecycleScope.launch {
            val classEntity = reviewImageViewModel.getClassByName(sectionName)
            val classId = classEntity?.id ?: 0

            val student = StudentEntity(lastName = lastName, firstName = firstName, score = score, classId = classId)
            val studentId = reviewImageViewModel.insertStudent(student)

            // Step 2: Create and save ImageCaptureEntity
            val imageCapture = ImageCaptureEntity(
                sheetId = sheetId,
                studentId = studentId.toInt(),
                imagePath = imagePath,
                sectionId = classId,
                score = score
            )
            reviewImageViewModel.insertImageCapture(imageCapture)

            // Navigate back or show a success message
            requireActivity().onBackPressed()
        }

//        val imageCapture = ImageCaptureEntity(sheetId = sheetId, imagePath = imagePath, lastName = lastName, firstName = firstName, score = score, section = section)
//
//        imageCaptureViewModel.insertImageCapture(imageCapture)
//        // Navigate back or show a success message
//        requireActivity().onBackPressed()
    }

    companion object {
        private const val ARG_SHEET_ID = "sheet_id"
        private const val ARG_IMAGE_PATH = "image_path"

        fun newInstance(sheetId: Int, imagePath: String): ReviewImageFragment {
            val fragment = ReviewImageFragment()
            val args = Bundle()
            args.putInt(ARG_SHEET_ID, sheetId)
            args.putString(ARG_IMAGE_PATH, imagePath)
            fragment.arguments = args
            return fragment
        }
    }
}
