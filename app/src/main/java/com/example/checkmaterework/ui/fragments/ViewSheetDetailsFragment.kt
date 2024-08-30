package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentViewSheetDetailsBinding
import com.example.checkmaterework.models.AnswerSheet
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ViewSheetDetailsFragment(private val answerSheet: AnswerSheet) : BottomSheetDialogFragment() {

    private lateinit var viewSheetDetailsBinding: FragmentViewSheetDetailsBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewSheetDetailsBinding = FragmentViewSheetDetailsBinding.inflate(inflater, container, false)
        return viewSheetDetailsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val activity = requireActivity()
//        answerSheetViewModel = ViewModelProvider(activity).get(AnswerSheetViewModel::class.java)

        // Set Answer Sheet details
        viewSheetDetailsBinding.textViewSheetName.text = answerSheet.name
        viewSheetDetailsBinding.textViewNumberOfItems.text = "Total Items: ${answerSheet.items}"

        // Display included exam types
        val examTypeDetails = answerSheet.examTypesList.joinToString("\n") {
            "${it.first}: ${it.second} items"
        }
        viewSheetDetailsBinding.textViewExamTypeDetails.text = examTypeDetails

    }
}