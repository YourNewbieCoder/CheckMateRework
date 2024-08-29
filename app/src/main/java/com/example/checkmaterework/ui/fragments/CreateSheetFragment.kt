package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.checkmaterework.databinding.FragmentCreateSheetBinding
import com.example.checkmaterework.models.AnswerSheet
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CreateSheetFragment(private val onNewSheetAdded: (AnswerSheet) -> Unit) : BottomSheetDialogFragment() {

    private lateinit var createSheetBinding: FragmentCreateSheetBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        createSheetBinding = FragmentCreateSheetBinding.inflate(inflater, container, false)
        return createSheetBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        answerSheetViewModel = ViewModelProvider(activity).get(AnswerSheetViewModel::class.java)

        createSheetBinding.buttonSave.setOnClickListener {
            saveSheetData()
        }
    }

    private fun saveSheetData() {
        val sheetName = createSheetBinding.textInputSheetName.text.toString()
        val numberOfItems = createSheetBinding.textInputNumberOfItems.text.toString().toIntOrNull() ?: 0

        if (sheetName.isNotEmpty()) {
            // Create a new AnswerSheet object
            val newSheet = AnswerSheet(sheetName, numberOfItems)

//            // Update the ViewModel with the new sheet
//            answerSheetViewModel.createSheet(newSheet)

            // Call the callback to inform the parent fragment
            onNewSheetAdded(newSheet)

            // Clear input fields and dismiss the dialog
            createSheetBinding.textInputSheetName.setText("")
            createSheetBinding.textInputNumberOfItems.setText("")
            dismiss()
        }
    }
}