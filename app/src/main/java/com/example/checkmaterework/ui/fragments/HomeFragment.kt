package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentHomeBinding
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.example.checkmaterework.ui.adapters.CreatedSheet
import com.example.checkmaterework.ui.adapters.CreatedSheetsAdapter

class HomeFragment : Fragment() {

    private lateinit var homeBinding: FragmentHomeBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var createdSheetsAdapter: CreatedSheetsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        homeBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return homeBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        answerSheetViewModel = ViewModelProvider(activity)[AnswerSheetViewModel::class.java]
        homeBinding.buttonCreateSheet.setOnClickListener {
            CreateSheetFragment().show(childFragmentManager, "createSheetTag")
        }

        answerSheetViewModel.sheetName.observe(viewLifecycleOwner) {
            homeBinding.textSheetName.text = String.format("Sheet Name: %s", it)
        }

        answerSheetViewModel.numberOfItems.observe(viewLifecycleOwner) {
            homeBinding.textNumberOfItems.text = String.format("Number Of Items: %s", it)
        }

//        recyclerView = view.findViewById(R.id.recyclerViewCreatedSheets)
//        recyclerView.layoutManager = LinearLayoutManager(requireContext())

//        val createdSheets = listOf(
//            CreatedSheet("Quiz 1"),
//            CreatedSheet("Quiz 2"),
//            CreatedSheet("Quiz 3"),
//            CreatedSheet("Quiz 4"),
//            CreatedSheet("Quiz 5"),
//            CreatedSheet("Quiz 6"),
//            CreatedSheet("Quiz 7")
//        )



//        createdSheetsAdapter = CreatedSheetsAdapter(createdSheets)
//        recyclerView.adapter = createdSheetsAdapter
    }
}