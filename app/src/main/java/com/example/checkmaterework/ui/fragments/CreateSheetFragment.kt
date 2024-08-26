package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.checkmaterework.databinding.FragmentCreateSheetBinding
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CreateSheetFragment : BottomSheetDialogFragment() {

    private lateinit var createSheetBinding: FragmentCreateSheetBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        createSheetBinding = FragmentCreateSheetBinding.inflate(inflater, container, false)
        return createSheetBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        answerSheetViewModel = ViewModelProvider(activity)[AnswerSheetViewModel::class.java]
        createSheetBinding.buttonSave.setOnClickListener {
            saveSheetData()
        }
    }

    private fun saveSheetData() {
        answerSheetViewModel.sheetName.value = createSheetBinding.textInputSheetName.text.toString()
        answerSheetViewModel.numberOfItems.value = createSheetBinding.textInputNumberOfItems.text.toString()
        createSheetBinding.textInputSheetName.setText("")
        createSheetBinding.textInputNumberOfItems.setText("")
        dismiss()
    }
}