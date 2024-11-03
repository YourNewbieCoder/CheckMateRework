package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentAnalysisBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.ClassEntity
import com.example.checkmaterework.models.StudentRecordEntity
import com.example.checkmaterework.models.StudentRecordViewModel
import com.example.checkmaterework.models.StudentRecordViewModelFactory
import com.example.checkmaterework.ui.adapters.ViewAnalysisAdapter
import com.example.checkmaterework.ui.fragments.StudentRecordsFragment.Companion

class AnalysisFragment : Fragment(), ToolbarTitleProvider {

    private lateinit var analysisBinding: FragmentAnalysisBinding
    private lateinit var studentRecordViewModel: StudentRecordViewModel
    private lateinit var viewAnalysisAdapter: ViewAnalysisAdapter
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
        analysisBinding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return analysisBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analysisBinding.recyclerViewItemAnalysis.layoutManager = LinearLayoutManager(requireContext())

        // Set up the adapter
        viewAnalysisAdapter = ViewAnalysisAdapter(mutableListOf())
        analysisBinding.recyclerViewItemAnalysis.adapter = viewAnalysisAdapter

        // Retrieve `classId` and `answerSheetId` from arguments
        val classId = arguments?.getInt(ARG_CLASS_ID)
        val answerSheetId = arguments?.getInt(ARG_ANSWER_SHEET_ID)

        if (classId != null && answerSheetId != null && answerSheetId != -1) {
            studentRecordViewModel.getRecordsByClassAndAnswerSheet(classId, answerSheetId)
        }

        studentRecordViewModel.studentRecordList.observe(viewLifecycleOwner) { records ->
            if (records.isNullOrEmpty()) {
                analysisBinding.textViewNoItemAnalysis.visibility = View.VISIBLE
            } else {
                analysisBinding.textViewNoItemAnalysis.visibility = View.GONE
                displayItemAnalysis(records)
            }
        }
    }

    private fun displayItemAnalysis(records: List<StudentRecordEntity>) {
        val questionStats = mutableMapOf<String, Pair<Int, Int>>() // Map of "Qn" -> (correctCount, incorrectCount)

        for (record in records) {
            val analysisParts = record.itemAnalysis.split(";")
            for (part in analysisParts) {
                if (part.isNotBlank()) {
                    val (question, status) = part.trim().split(":").map { it.trim() }
                    val counts = questionStats.getOrDefault(question, 0 to 0)
                    questionStats[question] = if (status.equals("Correct", ignoreCase = true)) {
                        counts.first + 1 to counts.second
                    } else {
                        counts.first to counts.second + 1
                    }
                }
            }
        }

        val itemAnalysisList = questionStats.map { (question, counts) ->
            Triple(question, counts.first, counts.second)
        }

        viewAnalysisAdapter.submitList(itemAnalysisList)

    }

    override fun getFragmentTitle(): String {
        return "$className $answerSheetName Item Analysis"
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
        private const val ARG_ANSWER_SHEET_ID = "answer_sheet_id" // Add this constant

        fun newInstance(selectedClass: ClassEntity, answerSheetName: String?, answerSheetId: Int?): AnalysisFragment {
            val fragment = AnalysisFragment()
            val bundle = Bundle().apply {
                putString(ARG_CLASS_NAME, selectedClass.className)
                putString(ARG_ANSWER_SHEET_NAME, answerSheetName)
                putInt(ARG_CLASS_ID, selectedClass.classId)
                putInt(ARG_ANSWER_SHEET_ID, answerSheetId ?: -1) // Pass answerSheetId
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}