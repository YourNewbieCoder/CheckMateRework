package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.checkmaterework.databinding.FragmentHomeBinding
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.example.checkmaterework.ui.adapters.CreatedSheetsAdapter

class HomeFragment : Fragment() {

    private lateinit var homeBinding: FragmentHomeBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel
    private lateinit var createdSheetsAdapter: CreatedSheetsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        homeBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return homeBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        answerSheetViewModel = ViewModelProvider(activity).get(AnswerSheetViewModel::class.java)

        homeBinding.recyclerViewCreatedSheets.layoutManager = LinearLayoutManager(requireContext())
        createdSheetsAdapter = CreatedSheetsAdapter(mutableListOf())
        homeBinding.recyclerViewCreatedSheets.adapter = createdSheetsAdapter

        answerSheetViewModel.createdSheetList.observe(viewLifecycleOwner) { sheets ->
            createdSheetsAdapter.updateSheetList(sheets)
        }

        homeBinding.buttonCreateSheet.setOnClickListener {
            showCreateSheetFragment()
        }
    }

    private fun showCreateSheetFragment() {
        val createSheetFragment = CreateSheetFragment { sheet ->
            answerSheetViewModel.createSheet(sheet)
        }
        createSheetFragment.show(parentFragmentManager, createSheetFragment.tag)
    }
}