package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentStudentRecordsBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.StudentViewModel
import com.example.checkmaterework.models.StudentViewModelFactory
import com.example.checkmaterework.ui.adapters.StudentAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class StudentRecordsFragment(private val classId: Int?) : BottomSheetDialogFragment() {

    private lateinit var studentRecordsBinding: FragmentStudentRecordsBinding
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var studentAdapter: StudentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        // Setup ViewModel
//        val dao = AnswerSheetDatabase.getDatabase(requireContext()).studentDao()
//        studentViewModel = ViewModelProvider(this, StudentViewModelFactory(dao))
//            .get(StudentViewModel::class.java)

        // Setup ViewModel
        val studentDao = AnswerSheetDatabase.getDatabase(requireContext()).studentDao()
        val classDao = AnswerSheetDatabase.getDatabase(requireContext()).classDao() // Add this line
        studentViewModel = ViewModelProvider(this, StudentViewModelFactory(studentDao, classDao))
            .get(StudentViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        studentRecordsBinding = FragmentStudentRecordsBinding.inflate(inflater, container, false)
        return studentRecordsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
//        studentAdapter = StudentAdapter(mutableListOf())

        studentRecordsBinding.recyclerViewStudents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = studentAdapter
        }

//        // Load students based on classId
//        if (classId != null) {
//            studentViewModel.loadStudentsByClass(classId) // Filter by class
//        } else {
//            studentViewModel.loadAllStudents() // Load all students if no classId is provided
//        }

        // Observe the list of students
        studentViewModel.studentList.observe(viewLifecycleOwner) { students ->
            if (students.isEmpty()) {
                studentRecordsBinding.textViewNoStudents.visibility = View.VISIBLE
                studentRecordsBinding.recyclerViewStudents.visibility = View.GONE
            } else {
                studentRecordsBinding.textViewNoStudents.visibility = View.GONE
                studentRecordsBinding.recyclerViewStudents.visibility = View.VISIBLE
                studentAdapter.updateStudentList(students)
            }
        }

//        // Handle add student button click
//        studentRecordsBinding.buttonAddStudent.setOnClickListener {
//            showAddStudentDialog()
//        }
    }

//    private fun showAddStudentDialog() {
//        val addStudentFragment = AddStudentFragment { student ->
//            studentViewModel.addStudent(student.copy(classId = classId ?: 0)) // Ensure student is linked to the class
//        }
//        addStudentFragment.show(parentFragmentManager, addStudentFragment.tag)
//    }

//    override fun getFragmentTitle(): String {
//        return getString(R.string.students_title)
//    }

//    override fun onResume() {
//        super.onResume()
//        setupToolbar()
//    }

//    private fun setupToolbar() {
//        val activity = requireActivity() as AppCompatActivity
//        activity.setSupportActionBar(activity.findViewById(R.id.myToolbar))
//
//        val canGoBack = parentFragmentManager.backStackEntryCount > 0
//        activity.supportActionBar?.setDisplayHomeAsUpEnabled(canGoBack)
//        activity.supportActionBar?.setDisplayShowHomeEnabled(canGoBack)
//
//        activity.supportActionBar?.title = getFragmentTitle()
//
//        if (canGoBack) {
//            activity.findViewById<Toolbar>(R.id.myToolbar).setNavigationOnClickListener {
//                activity.onBackPressed()
//            }
//        }
//    }
}