package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentAddStudentBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.StudentEntity
import com.example.checkmaterework.models.StudentViewModel
import com.example.checkmaterework.models.StudentViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddStudentFragment(
    private val onStudentAdded: (StudentEntity) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var addStudentBinding: FragmentAddStudentBinding
    private lateinit var studentViewModel: StudentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup ViewModel
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).studentDao()
        studentViewModel = ViewModelProvider(this, StudentViewModelFactory(dao))
            .get(StudentViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        addStudentBinding = FragmentAddStudentBinding.inflate(inflater, container, false)
        return addStudentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle save button click
        addStudentBinding.buttonSaveStudent.setOnClickListener {
            val lastName = addStudentBinding.textInputLastName.text.toString()
            val firstName = addStudentBinding.textInputFirstName.text.toString()
//            val score = addStudentBinding.textInputScore.text.toString().toIntOrNull() ?: 0

            if (lastName.isNotEmpty() && firstName.isNotEmpty()){
                val newStudent = StudentEntity(studentName = firstName, score = 0, classId = 0)
                onStudentAdded(newStudent)
                dismiss()
            } else {
                // Show validation error
                addStudentBinding.textInputLastName.error = "Last name required"
                addStudentBinding.textInputFirstName.error = "First name required"
            }
        }
    }
}