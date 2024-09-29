package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.LayoutAnswerKeyDetailsBinding
import com.example.checkmaterework.models.AnswerSheetEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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

        val answerKeyContainer = view.findViewById<LinearLayout>(R.id.answerKeyContainer)

        var currentItemNumber = 1 // Start with item number 1

        for ((examType, itemCount) in answerSheet.examTypesList) {
            if (examType == "Multiple Choice") {
                for (i in 1..itemCount) {
                    // Create horizontal LinearLayout for each question
                    val questionLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // Create TextView for numbering
                    val numberTextView = TextView(requireContext()).apply {
                        text = "$currentItemNumber: "
                        textSize = 20f // Adjust text size as needed
                    }
                    questionLayout.addView(numberTextView)

                    // Create ChipGroup for multiple-choice
                    val chipGroup = ChipGroup(requireContext()).apply {
                        isSingleSelection = true // Only one answer can be selected
                    }
                    for (option in listOf("A", "B", "C", "D")) {
                        chipGroup.addView(Chip(requireContext()).apply {
                            text = option
                            isCheckable = true
                        })
                    }
                    questionLayout.addView(chipGroup)

                    // Add the horizontal LinearLayout to the answerKeyContainer
                    answerKeyContainer.addView(questionLayout)

                    currentItemNumber++ // Increment item number
                }
            } else if (examType == "Identification") {
                for (i in 1..itemCount) {
                    // Create horizontal LinearLayout for each question
                    val questionLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // Create TextView for numbering
                    val numberTextView = TextView(requireContext()).apply {
                        text = "$currentItemNumber: "
                        textSize = 20f // Adjust text size as needed
                    }
                    questionLayout.addView(numberTextView)

                    // Create TextInputLayout for identification
                    val textInputLayout = TextInputLayout(requireContext()).apply {
                        hint = "Enter answer for item $currentItemNumber"
                        layoutParams = LinearLayout.LayoutParams(
                            0, // Set width to 0dp
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            weight = 1f // Set weight to 1 to take up remaining space
                        }
                        visibility = View.VISIBLE

                        // Add TextInputEditText to TextInputLayout
                        val textInputEditText = TextInputEditText(requireContext())
//                        textInputEditText.hint = "Enter answer for item $i"
                        hint = "Enter answer for item $currentItemNumber"

                        addView(textInputEditText)
                    }
                    questionLayout.addView(textInputLayout)

                    // Add the horizontal LinearLayout to the answerKeyContainer
                    answerKeyContainer.addView(questionLayout)

                    currentItemNumber ++ // Increment the item number for the next iteration
                }
            }
        }
    }
}