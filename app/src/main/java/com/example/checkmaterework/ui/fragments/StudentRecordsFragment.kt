package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.databinding.FragmentStudentRecordsBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.StudentViewModel
import com.example.checkmaterework.models.StudentViewModelFactory
import com.example.checkmaterework.ui.adapters.StudentAdapter

class StudentRecordsFragment(private val classId: Int) : Fragment() {

    private lateinit var studentRecordsBinding: FragmentStudentRecordsBinding
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var studentAdapter: StudentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup ViewModel
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).studentDao()
        studentViewModel = ViewModelProvider(this, StudentViewModelFactory(dao))
            .get(StudentViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        studentRecordsBinding = FragmentStudentRecordsBinding.inflate(inflater, container, false)
        return studentRecordsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        studentAdapter = StudentAdapter(mutableListOf())

        studentRecordsBinding.recyclerViewStudents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = studentAdapter
        }

        // Load students by classId
        studentViewModel.loadStudentsByClass(classId)

        // Observe the list of students
        studentViewModel.studentList.observe(viewLifecycleOwner) { students ->
            studentAdapter.updateStudentList(students)
        }

        // Handle add student button click
        studentRecordsBinding.buttonAddStudent.setOnClickListener {
            showAddStudentDialog()
        }
    }

    private fun showAddStudentDialog() {
        val addStudentFragment = AddStudentFragment { student ->
            studentViewModel.addStudent(student.copy(classId = classId)) // Ensure student is linked to the class
        }
        addStudentFragment.show(parentFragmentManager, addStudentFragment.tag)
    }
}