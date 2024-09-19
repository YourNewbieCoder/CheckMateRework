package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentRecordsBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.example.checkmaterework.models.AnswerSheetViewModelFactory
import com.example.checkmaterework.models.ClassEntity
import com.example.checkmaterework.ui.adapters.ViewRecordsAdapter

class RecordsFragment : Fragment() {

    private lateinit var recordsBinding: FragmentRecordsBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel
    private lateinit var viewRecordsAdapter: ViewRecordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).answerSheetDao()
        answerSheetViewModel = ViewModelProvider(this, AnswerSheetViewModelFactory(dao))
            .get(AnswerSheetViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        recordsBinding = FragmentRecordsBinding.inflate(inflater, container, false)
        return recordsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recordsBinding.recyclerViewCreatedSheets.layoutManager = LinearLayoutManager(requireContext())

        // Set up the adapter
        viewRecordsAdapter = ViewRecordsAdapter(
            mutableListOf(),
            onViewRecordsClick = { sheet -> openClassesFragment(sheet) }
        )

        recordsBinding.recyclerViewCreatedSheets.adapter = viewRecordsAdapter

        answerSheetViewModel.createdSheetList.observe(viewLifecycleOwner) { sheets ->
            viewRecordsAdapter.updateSheetList(sheets)
        }
    }

    // Function to open ClassesFragment with the selected class entity
    private fun openClassesFragment(sheet: AnswerSheetEntity) {
        val classesFragment = ClassesFragment(sheet)

        // Replace the current fragment and add to back stack
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, classesFragment)
            .addToBackStack(null)
            .commit()
    }
}