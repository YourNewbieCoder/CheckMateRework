package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.databinding.FragmentHomeBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.example.checkmaterework.models.AnswerSheetViewModelFactory
import com.example.checkmaterework.ui.adapters.CreatedSheetsAdapter

class HomeFragment : Fragment() {

    private lateinit var homeBinding: FragmentHomeBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel
    private lateinit var createdSheetsAdapter: CreatedSheetsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).answerSheetDao()
        answerSheetViewModel = ViewModelProvider(this, AnswerSheetViewModelFactory(dao))
            .get(AnswerSheetViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        homeBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return homeBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeBinding.recyclerViewCreatedSheets.layoutManager = LinearLayoutManager(requireContext())

        // Pass the click listener to the adapter
        createdSheetsAdapter = CreatedSheetsAdapter(
            mutableListOf(),
            onItemClick = { sheet -> showViewSheetDetailsFragment(sheet)},
            onEditClick = { sheet -> showEditSheetDetailsFragment(sheet)}, // Pass edit logic
            onDeleteClick = { sheet -> deleteSheet(sheet)} // Pass delete logic
            )

        homeBinding.recyclerViewCreatedSheets.adapter = createdSheetsAdapter

        answerSheetViewModel.createdSheetList.observe(viewLifecycleOwner) { sheets ->
            createdSheetsAdapter.updateSheetList(sheets)
        }

        homeBinding.buttonCreateSheet.setOnClickListener {
            showCreateSheetFragment()
        }
    }

    private fun showCreateSheetFragment() {
        val createSheetFragment = CreateSheetFragment(
            onNewSheetAdded = { sheet ->
                answerSheetViewModel.createSheet(sheet)
            },
            onSheetUpdated = { updatedSheet ->
                answerSheetViewModel.updateSheet(updatedSheet)
            }
        )
        createSheetFragment.show(parentFragmentManager, createSheetFragment.tag)
    }

    private fun showViewSheetDetailsFragment(sheet: AnswerSheetEntity) {
        val viewSheetDetailsFragment = ViewSheetDetailsFragment(sheet)
        viewSheetDetailsFragment.show(parentFragmentManager, viewSheetDetailsFragment.tag)
    }

    private fun showEditSheetDetailsFragment(sheet: AnswerSheetEntity) {
        val editSheetFragment = CreateSheetFragment(
            existingSheet = sheet, // Pass the existing sheet for editing
            onNewSheetAdded = { newSheet ->
                answerSheetViewModel.createSheet(newSheet)
            },
            onSheetUpdated = { updatedSheet ->
                answerSheetViewModel.updateSheet(updatedSheet) // Update ViewModel with edited sheet
            }
        )
        editSheetFragment.show(parentFragmentManager, editSheetFragment.tag)
    }

    private fun deleteSheet(sheet: AnswerSheetEntity) {
        answerSheetViewModel.deleteSheet(sheet) // Call ViewModel method to delete sheet
    }
}