package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentStudentRecordsBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.ClassEntity
import com.example.checkmaterework.models.StudentRecordEntity
import com.example.checkmaterework.models.StudentRecordViewModel
import com.example.checkmaterework.models.StudentRecordViewModelFactory
import com.example.checkmaterework.ui.adapters.StudentRecordAdapter

class StudentRecordsFragment() : Fragment(), ToolbarTitleProvider {

    private lateinit var studentRecordsBinding: FragmentStudentRecordsBinding
    private lateinit var studentRecordAdapter: StudentRecordAdapter
    private lateinit var studentRecordViewModel: StudentRecordViewModel
    private lateinit var className: String
    private lateinit var answerSheetName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            className = it.getString(ARG_CLASS_NAME) ?: ""
            answerSheetName = it.getString(ARG_ANSWER_SHEET_NAME) ?: ""
        }

        // Setup ViewModel
        val studentRecordDao  = AnswerSheetDatabase.getDatabase(requireContext()).studentRecordDao()
        val studentDao  = AnswerSheetDatabase.getDatabase(requireContext()).studentDao()
        studentRecordViewModel = ViewModelProvider(this, StudentRecordViewModelFactory(studentRecordDao, studentDao))
            .get(StudentRecordViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        studentRecordsBinding = FragmentStudentRecordsBinding.inflate(inflater, container, false)
        return studentRecordsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        studentRecordAdapter = StudentRecordAdapter(mutableListOf()) { selectedRecord ->
            navigateToStudentAnalysisFragment(selectedRecord)
        }

        studentRecordsBinding.recyclerViewStudentScores.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = studentRecordAdapter
        }

        // Retrieve `classId` and `answerSheetId` from arguments
        val classId = arguments?.getInt(ARG_CLASS_ID)
        val answerSheetId = arguments?.getInt(ARG_ANSWER_SHEET_ID) // Add this line
        Log.d("StudentRecordsFragment", "Received answerSheetId: $answerSheetId")

        if (classId != null && answerSheetId != null) {
            studentRecordViewModel.getRecordsByClassAndAnswerSheet(classId, answerSheetId)
        }

        // Observe student records for the selected class
        studentRecordViewModel.studentRecordList.observe(viewLifecycleOwner) { records ->
            if (records.isEmpty()) {
                studentRecordsBinding.textViewNoStudentRecords.visibility = View.VISIBLE
                studentRecordsBinding.recyclerViewStudentScores.visibility = View.GONE
            } else {
                studentRecordsBinding.textViewNoStudentRecords.visibility = View.GONE
                studentRecordsBinding.recyclerViewStudentScores.visibility = View.VISIBLE

                val studentNames = studentRecordViewModel.studentNamesMap.value ?: emptyMap()
                studentRecordAdapter.updateRecords(records.toMutableList(), studentNames)
            }
        }

        studentRecordViewModel.studentNamesMap.observe(viewLifecycleOwner) { namesMap ->
            val records = studentRecordViewModel.studentRecordList.value ?: emptyList()
            studentRecordAdapter.updateRecords(records.toMutableList(), namesMap)
        }
    }

    private fun navigateToStudentAnalysisFragment(record: StudentRecordEntity) {
        val fragment = StudentAnalysisFragment.newInstance(record.recordId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun getFragmentTitle(): String {
        return "$className $answerSheetName Records"
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

    companion object {
        private const val ARG_CLASS_NAME = "class_name"
        private const val ARG_ANSWER_SHEET_NAME = "answer_sheet_name"
        private const val ARG_CLASS_ID = "class_id"
        private const val ARG_ANSWER_SHEET_ID = "answer_sheet_id" // New argument

        fun newInstance(selectedClass: ClassEntity, answerSheetName: String?, answerSheetId: Int?): StudentRecordsFragment {
            val fragment = StudentRecordsFragment()
            val bundle = Bundle().apply {
                putString(ARG_CLASS_NAME, selectedClass.className)
                putString(ARG_ANSWER_SHEET_NAME, answerSheetName)
                putInt(ARG_CLASS_ID, selectedClass.classId) // Pass the class ID
                putInt(ARG_ANSWER_SHEET_ID, answerSheetId?: -1) // Pass the answer sheet ID
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}