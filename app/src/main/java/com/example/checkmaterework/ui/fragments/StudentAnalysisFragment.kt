package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentStudentAnalysisBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.StudentRecordViewModel
import com.example.checkmaterework.models.StudentRecordViewModelFactory

class StudentAnalysisFragment : Fragment() {

    private lateinit var binding: FragmentStudentAnalysisBinding
    private lateinit var studentRecordViewModel: StudentRecordViewModel
    private var recordId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recordId = it.getInt(ARG_RECORD_ID)
        }

        // Setup ViewModel
        val studentRecordDao  = AnswerSheetDatabase.getDatabase(requireContext()).studentRecordDao()
        val studentDao  = AnswerSheetDatabase.getDatabase(requireContext()).studentDao()
        studentRecordViewModel = ViewModelProvider(this, StudentRecordViewModelFactory(studentRecordDao, studentDao))
            .get(StudentRecordViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch the record by ID
        studentRecordViewModel.getRecordById(recordId)

        // Observe the LiveData for the record
        studentRecordViewModel.studentRecord.observe(viewLifecycleOwner) { record ->
            if (record != null) {
                binding.textViewItemAnalysis.text = record.itemAnalysis
            } else {
                binding.textViewItemAnalysis.text = getString(R.string.no_analysis_found)
            }
        }
    }

    companion object {
        private const val ARG_RECORD_ID = "record_id"

        @JvmStatic
        fun newInstance(record: Int) = StudentAnalysisFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_RECORD_ID, record)
            }
        }
    }
}