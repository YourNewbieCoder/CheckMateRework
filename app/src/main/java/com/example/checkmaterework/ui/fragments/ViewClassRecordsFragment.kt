package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentViewClassRecordsBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.ClassEntity
import com.example.checkmaterework.models.ClassViewModel
import com.example.checkmaterework.models.ClassViewModelFactory
import com.example.checkmaterework.ui.adapters.ViewClassRecordsAdapter

class ViewClassRecordsFragment(sheet: AnswerSheetEntity) : Fragment() {

    private lateinit var binding: FragmentViewClassRecordsBinding
    private lateinit var classViewModel: ClassViewModel
    private lateinit var viewClassRecordsAdapter: ViewClassRecordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup ViewModel
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).classDao()
        classViewModel = ViewModelProvider(this, ClassViewModelFactory(dao))
            .get(ClassViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentViewClassRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewClasses.layoutManager = LinearLayoutManager(requireContext())

        // Setup RecyclerView Adapter
        viewClassRecordsAdapter = ViewClassRecordsAdapter(
            mutableListOf(),
            onViewClassRecordsClick = { classEntity -> displayClassRecords(classEntity)},
            onViewClassItemAnalysisClick = { classEntity -> displayClassItemAnalysis(classEntity)}, // Pass edit logic
        )

        binding.recyclerViewClasses.adapter = viewClassRecordsAdapter

        // Set the adapter to the RecyclerView
        binding.recyclerViewClasses.adapter = viewClassRecordsAdapter

        // Observe the data from ViewModel
        classViewModel.classList.observe(viewLifecycleOwner) { classes ->
            viewClassRecordsAdapter.updateClassList(classes)
        }
    }

    private fun displayClassRecords(classEntity: ClassEntity) {
        val studentRecordsFragment = StudentRecordsFragment(classEntity)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, studentRecordsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun displayClassItemAnalysis(classEntity: ClassEntity) {
        val analysisFragment = AnalysisFragment(classEntity)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, analysisFragment)
            .addToBackStack(null)
            .commit()
    }

}