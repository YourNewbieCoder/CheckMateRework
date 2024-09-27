package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.checkmaterework.databinding.LayoutAnswerKeyDetailsBinding
import com.example.checkmaterework.models.AnswerSheetEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ViewSheetDetailsKeyFragment (private val answerSheet: AnswerSheetEntity) : BottomSheetDialogFragment() {

    private lateinit var viewSheetDetailsKeyBinding: LayoutAnswerKeyDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewSheetDetailsKeyBinding =
            LayoutAnswerKeyDetailsBinding.inflate(inflater, container, false)
        return viewSheetDetailsKeyBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set Answer Sheet details
        viewSheetDetailsKeyBinding.textViewSheetNameKey.text = answerSheet.name
        viewSheetDetailsKeyBinding.textViewNumberOfItemsKey.text =
            "Total Items: ${answerSheet.items}"

        // Display included exam types
        val examTypeDetails = answerSheet.examTypesList.joinToString("\n\n") {
            "${it.first}: ${it.second} items"
        }
        viewSheetDetailsKeyBinding.textViewExamTypeKeyDetails.text = examTypeDetails

    }
}