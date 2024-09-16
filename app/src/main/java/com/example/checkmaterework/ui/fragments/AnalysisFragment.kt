package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentAnalysisBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.example.checkmaterework.models.AnswerSheetViewModelFactory
import com.example.checkmaterework.ui.adapters.ViewAnalysisAdapter
import com.example.checkmaterework.ui.adapters.ViewRecordsAdapter

class AnalysisFragment : Fragment() {

    private lateinit var analysisBinding: FragmentAnalysisBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel
    private lateinit var viewAnalysisAdapter: ViewAnalysisAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).answerSheetDao()
        answerSheetViewModel = ViewModelProvider(this, AnswerSheetViewModelFactory(dao))
            .get(AnswerSheetViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        analysisBinding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return analysisBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analysisBinding.recyclerViewCreatedSheets.layoutManager = LinearLayoutManager(requireContext())

        // Set up the adapter
        viewAnalysisAdapter = ViewAnalysisAdapter(
            mutableListOf(),
            onViewAnalysisClick = { sheet -> openCameraToCheckSheet(sheet) }
        )

        analysisBinding.recyclerViewCreatedSheets.adapter = viewAnalysisAdapter

        answerSheetViewModel.createdSheetList.observe(viewLifecycleOwner) { sheets ->
            viewAnalysisAdapter.updateSheetList(sheets)
        }
    }

    private fun openCameraToCheckSheet(sheet: AnswerSheetEntity) {

    }
}