package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentStudentBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.StudentEntity
import com.example.checkmaterework.models.StudentViewModel
import com.example.checkmaterework.models.StudentViewModelFactory
import com.example.checkmaterework.ui.adapters.StudentAdapter

class StudentFragment : Fragment(), ToolbarTitleProvider {

    private lateinit var studentBinding: FragmentStudentBinding
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var studentAdapter: StudentAdapter
    private var classId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            classId = it.getInt(ARG_CLASS_ID, 0)
        }

//        // Initialize ViewModel with DAO
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
        studentBinding = FragmentStudentBinding.inflate(inflater, container, false)
        return studentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        studentBinding.recyclerViewStudents.layoutManager = LinearLayoutManager(requireContext())

        studentAdapter = StudentAdapter(
            mutableListOf(),
            onDeleteClick = { studentEntity -> deleteStudent(studentEntity)}
        )
        studentBinding.recyclerViewStudents.adapter = studentAdapter

        // Fetch students for the class on initial load
        studentViewModel.getStudentsForClass(classId)

        // Observe the data from ViewModel
        studentViewModel.studentList.observe(viewLifecycleOwner) { students ->
            studentAdapter.updateStudentList(students.toMutableList())
            Log.d("StudentFragment", "Number of students: ${students.size}")
        }

        studentBinding.buttonAddStudent.setOnClickListener {
            showAddStudentDialog()
        }

        Log.d("StudentFragment", "Received classId: $classId")

    }

    private fun showAddStudentDialog() {
        val addStudentFragment = AddStudentFragment(
            classId = classId,
            onStudentAdded = { newStudent ->
                studentViewModel.addStudent(newStudent)
            }
        )
        addStudentFragment.show(parentFragmentManager, addStudentFragment.tag)
    }

    private fun deleteStudent(studentEntity: StudentEntity) {
        studentViewModel.deleteStudent(studentEntity)
    }

    companion object {
        private const val ARG_CLASS_ID = "class_id"

        @JvmStatic
        fun newInstance(classId: Int) = StudentFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_CLASS_ID, classId)
            }
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.students_title)
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
}