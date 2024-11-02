package com.example.checkmaterework.ui.fragments

import android.os.Bundle
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

class StudentRecordsFragment(private val selectedClass: ClassEntity) : Fragment(), ToolbarTitleProvider {

    private lateinit var studentRecordsBinding: FragmentStudentRecordsBinding
    private lateinit var studentRecordAdapter: StudentRecordAdapter
    private lateinit var studentRecordViewModel: StudentRecordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        studentRecordViewModel.getRecordsByClassId(selectedClass.classId)

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