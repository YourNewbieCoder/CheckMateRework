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

class AnalysisFragment(private val selectedClass: ClassEntity) : Fragment(), ToolbarTitleProvider {

    private lateinit var analysisBinding: FragmentAnalysisBinding
    private lateinit var studentRecordViewModel: StudentRecordViewModel
    private lateinit var viewAnalysisAdapter: ViewAnalysisAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        studentRecordViewModel.getRecordsByClassId(selectedClass.classId)

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
        return getString(R.string.analysis_title)
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