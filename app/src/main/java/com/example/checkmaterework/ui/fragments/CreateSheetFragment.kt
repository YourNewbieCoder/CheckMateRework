package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentCreateSheetBinding
import com.example.checkmaterework.models.AnswerSheet
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.example.checkmaterework.models.AnswerSheetViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText

class CreateSheetFragment(
    private val existingSheet: AnswerSheetEntity? = null, // Optional parameter for editing
    private val onNewSheetAdded: (AnswerSheetEntity) -> Unit,
    private val onSheetUpdated: (AnswerSheetEntity) -> Unit // Callback for updating a sheet
) : BottomSheetDialogFragment() {

    private lateinit var createSheetBinding: FragmentCreateSheetBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel
    private val examTypeOptions = listOf("Multiple Choice", "Identification", "Word Problem")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).answerSheetDao()
        answerSheetViewModel = ViewModelProvider(this, AnswerSheetViewModelFactory(dao))
            .get(AnswerSheetViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        createSheetBinding = FragmentCreateSheetBinding.inflate(inflater, container, false)
        return createSheetBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        createSheetBinding.buttonSave.setOnClickListener {
            saveSheetData()
        }

        createSheetBinding.buttonAddExamType.setOnClickListener {
            addExamTypeView()
        }
    }

    private fun setupUI() {
        // Populate fields if editing an existing sheet
        existingSheet?.let { sheet ->
            createSheetBinding.textInputSheetName.setText(sheet.name)
            createSheetBinding.textInputNumberOfItems.setText(sheet.items.toString())

            // Disable editing of sheet name when editing an existing sheet
            createSheetBinding.textInputSheetName.isEnabled = false

            // Populate exam types
            sheet.examTypesList.forEach { addExamTypeView(it.first, it.second) }
        }
    }

    private fun addExamTypeView(examType: String = "", itemsCount: Int = 0) {
        // Inflate the exam type item layout
        val examTypeView = LayoutInflater.from(requireContext()).inflate(
            R.layout.layout_exam_types, createSheetBinding.examTypesContainer, false
        )

        val textInputExamType = examTypeView.findViewById<MaterialAutoCompleteTextView>(R.id.textInputExamType)
        val textInputItemsCount = examTypeView.findViewById<TextInputEditText>(R.id.textInputItemsCount)
        val buttonRemoveExamType = examTypeView.findViewById<MaterialButton>(R.id.buttonRemoveExamType)

        // Set up ArrayAdapter for dropdown selection of exam types
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, examTypeOptions)
        textInputExamType.setAdapter(adapter)

        // Set existing values if provided
        textInputExamType.setText(examType)
        textInputItemsCount.setText(if (itemsCount > 0) itemsCount.toString() else "")

        textInputExamType.setOnClickListener {
            textInputExamType.showDropDown() // Show dropdown when clicked
        }

        buttonRemoveExamType.setOnClickListener {
            createSheetBinding.examTypesContainer.removeView(examTypeView) // Remove this view from the container
        }

        createSheetBinding.examTypesContainer.addView(examTypeView) // Add the view to the container
    }

    private fun saveSheetData() {
        val sheetName = createSheetBinding.textInputSheetName.text.toString()
        val numberOfItems = createSheetBinding.textInputNumberOfItems.text.toString().toIntOrNull() ?: 0

        // Collect data from dynamically added exam types
        val examTypesList =  mutableListOf<Pair<String, Int>>()
        for (i in 0 until createSheetBinding.examTypesContainer.childCount) {
            val examTypeView = createSheetBinding.examTypesContainer.getChildAt(i)
            val examType = examTypeView.findViewById<MaterialAutoCompleteTextView>(R.id.textInputExamType).text.toString()
            val itemsCount = examTypeView.findViewById<TextInputEditText>(R.id.textInputItemsCount).text.toString().toIntOrNull() ?:0

            if (examType.isNotEmpty() && itemsCount > 0) {
                examTypesList.add(Pair(examType, itemsCount))
            }
        }

        // Validate data to ensure the total items specified matches the sum of exam types' items
        val totalItems = examTypesList.sumOf { it.second }
        if (sheetName.isEmpty()) {
            createSheetBinding.textInputSheetName.error = "Please specify the name for the created sheet!"
            return
        }
        if (totalItems != numberOfItems) {
            createSheetBinding.textInputNumberOfItems.error = "Total items don't match the specified count!"
            return
        }

        val newSheet = AnswerSheetEntity(name = sheetName, items = numberOfItems, examTypesList = examTypesList)

        if (existingSheet == null) {
            answerSheetViewModel.createSheet(newSheet) // Creating new sheet
        } else {
            answerSheetViewModel.updateSheet(newSheet) // Updating existing sheet
        }

        // Clear input fields and dismiss the dialog
        createSheetBinding.textInputSheetName.setText("")
        createSheetBinding.textInputNumberOfItems.setText("")
        createSheetBinding.examTypesContainer.removeAllViews()
        dismiss()

    }
}