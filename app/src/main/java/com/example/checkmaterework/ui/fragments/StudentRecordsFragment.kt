package com.example.checkmaterework.ui.fragments

import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.FileWriter

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

        studentRecordsBinding.buttonExportCSV.setOnClickListener {
            val records = studentRecordViewModel.studentRecordList.value ?: emptyList()
            val studentNamesMap = studentRecordViewModel.studentNamesMap.value ?: emptyMap()

            if (records.isEmpty()) {
                Toast.makeText(requireContext(), "No records to export", Toast.LENGTH_SHORT).show()
            } else {
                exportRecordsToCSV(records, studentNamesMap)
            }
        }
    }

    private fun exportRecordsToCSV(
        records: List<StudentRecordEntity>,
        studentNamesMap: Map<Int, String>
    ) {
        // Sanitize the class name and answer sheet name to make them file-system safe
        val sanitizedClassName = className.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val sanitizedAnswerSheetName = answerSheetName.replace("[^a-zA-Z0-9]".toRegex(), "_")

        // Construct the file name
        val fileName = "${sanitizedClassName}_${sanitizedAnswerSheetName}_Records.csv"

        // Get the file path
//        val fileName = "StudentRecords.csv"
        val fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val csvFile = File(fileDir, fileName)

        try {
            // Write data to the CSV file
            FileWriter(csvFile).use { writer ->
                writer.append("Student Name,Score\n") // CSV header

                records.forEach { record ->
                    val studentName = studentNamesMap[record.studentId] ?: "Unknown"
                    writer.append("$studentName,${record.score}\n")
                }
            }

            // Notify the Media Scanner to make the file visible
            MediaScannerConnection.scanFile(
                requireContext(),
                arrayOf(csvFile.absolutePath),
                null
            ) { path, uri ->
                Log.d("ExportCSV", "File is visible at: $path")
            }

            Toast.makeText(requireContext(), "CSV exported to Downloads: ${csvFile.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }

//        try {
//            // Write the data to CSV
//            FileWriter(csvFile).use { writer ->
//                writer.append("Student Name,Score\n") // CSV header
//
//                records.forEach { record ->
//                    val studentName = studentNamesMap[record.studentId] ?: "Unknown"
//                    writer.append("$studentName,${record.score}\n")
//                }
//            }
//
//            // Notify user of success
//            Toast.makeText(requireContext(), "CSV exported to ${csvFile.absolutePath}", Toast.LENGTH_SHORT).show()
//
////            // Optionally share the file
////            shareCSVFile(csvFile)
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(requireContext(), "Failed to export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
    }

//    private fun shareCSVFile(file: File) {
//        val uri = FileProvider.getUriForFile(
//            requireContext(),
//            "${requireContext().packageName}.provider", // Use your app's provider authority
//            file
//        )
//
//        val intent = Intent(Intent.ACTION_SEND).apply {
//            type = "text/csv"
//            putExtra(Intent.EXTRA_STREAM, uri)
//        }
//
//        startActivity(Intent.createChooser(intent, "Share CSV"))
//    }

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