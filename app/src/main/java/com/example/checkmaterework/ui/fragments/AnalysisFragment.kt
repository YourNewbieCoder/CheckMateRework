package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.checkmaterework.models.ViewAnalysisItem
import com.example.checkmaterework.ui.adapters.ViewAnalysisAdapter
import com.example.checkmaterework.ui.fragments.StudentRecordsFragment.Companion
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

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

        analysisBinding.buttonExportCSV.setOnClickListener {
            studentRecordViewModel.studentRecordList.observe(viewLifecycleOwner) { records ->
                if (records.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "No records available to export", Toast.LENGTH_SHORT).show()
                } else {
                    exportToCSV(records)
                }
            }
        }

    }

    private fun exportToCSV(records: List<StudentRecordEntity>) {
        // Prepare the CSV file data
        val csvHeader = "Question Number,Correct Students,Incorrect Students\n"
        val csvData = StringBuilder(csvHeader)

        records.forEach { record ->
            val analysisParts = record.itemAnalysis.split(";")
            analysisParts.forEach { part ->
                if (part.isNotBlank()) {
                    val (question, status) = part.trim().split(":").map { it.trim() }
                    val correctCount = if (status.equals("Correct", ignoreCase = true)) 1 else 0
                    val incorrectCount = if (status.equals("Incorrect", ignoreCase = true)) 1 else 0
                    csvData.append("$question,$correctCount,$incorrectCount\n")
                }
            }
        }

        // Save the CSV to a file
        try {
            val fileName = "${className.replace(" ", "_")}_${answerSheetName.replace(" ", "_")}_item_analysis_${System.currentTimeMillis()}.csv"
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CheckMate Rework")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos).use { writer ->
                    writer.write(csvData.toString())
                }
            }

            // Notify the user
            Toast.makeText(requireContext(), "CSV exported successfully to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error exporting CSV: ${e.message}", Toast.LENGTH_LONG).show()
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
            val correctPercent = (counts.first * 100) / (counts.first + counts.second)
            val remarks = when {
                correctPercent >= 80 -> "Excellent"
                correctPercent >= 50 -> "Average"
                else -> "Needs Improvement"
            }
            ViewAnalysisItem(
                question = question,
                correctCount = counts.first,
                incorrectCount = counts.second,
                isMostCorrect = false,
                isLeastCorrect = false,
                remarks = remarks // Add remarks
            )
//            Triple(question, counts.first, counts.second)
        }

//        // Sort items by correct count (descending) and incorrect count (ascending)
//        val sortedByCorrect = itemAnalysisList.sortedWith(
//            compareByDescending<Triple<String, Int, Int>> { it.second }
//                .thenBy { it.third }
//        )
        // Highlight top most/least correct items (existing logic)
        val sortedByCorrect = itemAnalysisList.sortedWith(
            compareByDescending<ViewAnalysisItem> { it.correctCount }.thenBy { it.incorrectCount }
        )

        val topMostCorrect = sortedByCorrect.take(3)
        val topLeastCorrect = sortedByCorrect.sortedBy { it.correctCount }.take(3)

//        // Extract top 3 most correct items with ties
//        val topMostCorrect = mutableListOf<Triple<String, Int, Int>>()
//        for (item in sortedByCorrect) {
//            if (topMostCorrect.size < 3 || item.second == topMostCorrect.last().second) {
//                topMostCorrect.add(item)
//            } else if (topMostCorrect.size >= 3) {
//                break
//            }
//        }
//
//        // Extract top 3 least correct items with ties
//        val sortedByIncorrect = itemAnalysisList.sortedWith(
//            compareByDescending<Triple<String, Int, Int>> { it.third }
//                .thenBy { it.second }
//        )
//        val topLeastCorrect = mutableListOf<Triple<String, Int, Int>>()
//        for (item in sortedByIncorrect) {
//            if (topLeastCorrect.size < 3 || item.third == topLeastCorrect.last().third) {
//                topLeastCorrect.add(item)
//            } else if (topLeastCorrect.size >= 3) {
//                break
//            }
//        }

//        // Highlight items based on their inclusion in top-most and top-least correct
//        val highlightedItems = itemAnalysisList.map {
//            ViewAnalysisItem(
//                question = it.first,
//                correctCount = it.second,
//                incorrectCount = it.third,
//                isMostCorrect = topMostCorrect.contains(it),
//                isLeastCorrect = topLeastCorrect.contains(it)
//            )
//        }

        val highlightedItems = itemAnalysisList.map {
            it.copy(
                isMostCorrect = topMostCorrect.contains(it),
                isLeastCorrect = topLeastCorrect.contains(it)
            )
        }

        // Submit the highlighted list to the adapter
        viewAnalysisAdapter.submitList(highlightedItems)
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