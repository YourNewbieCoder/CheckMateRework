package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentEditAnswerKeyBinding
import com.example.checkmaterework.models.AnswerSheetEntity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditAnswerKeyFragment(private val answerSheet: AnswerSheetEntity) : Fragment(), ToolbarTitleProvider {

    private lateinit var editAnswerKeyBinding: FragmentEditAnswerKeyBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        editAnswerKeyBinding = FragmentEditAnswerKeyBinding.inflate(inflater, container, false)
        return editAnswerKeyBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Here you can set up your view logic, such as loading the data of the answer sheet
        loadAnswerKeyData(answerSheet)
    }

    private fun loadAnswerKeyData(answerSheet: AnswerSheetEntity) {
        editAnswerKeyBinding.textViewSheetNameKey.text = answerSheet.name // Set the name of the answer sheet

        val answerKeyContainer = editAnswerKeyBinding.answerKeyContainer // Get the container for answer key items

        var currentItemNumber = 1

        for ((examType, itemCount) in answerSheet.examTypesList) {
            if (examType == "Multiple Choice") {
                for (i in 1..itemCount) {
                    // Create horizontal LinearLayout for each question
//                    val questionLayout = LinearLayout(requireContext()).apply {
//                        orientation = LinearLayout.HORIZONTAL
//                        layoutParams = LinearLayout.LayoutParams(
//                            LinearLayout.LayoutParams.MATCH_PARENT,
//                            LinearLayout.LayoutParams.WRAP_CONTENT
//                        )
//                    }

                    // Create TextView for numbering
                    val numberTextView = TextView(requireContext()).apply {
                        text = "$currentItemNumber: "
                        textSize = 20f // Adjust text size as needed
                    }
                    answerKeyContainer.addView(numberTextView)

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
                    answerKeyContainer.addView(chipGroup)

//                    // Add the horizontal LinearLayout to the answerKeyContainer
//                    answerKeyContainer.addView(questionLayout)

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
                        text = "$currentItemNumber : "
                        textSize = 20f // Adjust text size as needed
                    }
                    questionLayout.addView(numberTextView)

                    // Create TextInputLayout for identification
                    val identificationLayout = createTextInputLayout("Answer for $currentItemNumber")

//                    val textInputLayout = TextInputLayout(requireContext()).apply {
//                        hint = "Answer for $currentItemNumber"
//                        layoutParams = LinearLayout.LayoutParams(
//                            0, // Set width to 0dp
//                            LinearLayout.LayoutParams.WRAP_CONTENT
//                        ).apply {
//                            weight = 1f // Set weight to 1 to take up remaining space
//                        }
//                        visibility = View.VISIBLE
//
//                        // Add TextInputEditText to TextInputLayout
//                        val textInputEditText = TextInputEditText(requireContext())
//                        hint = "Answer for $currentItemNumber"
//
//                        addView(textInputEditText)
//                    }
                    questionLayout.addView(identificationLayout)

                    // Add the horizontal LinearLayout to the answerKeyContainer
                    answerKeyContainer.addView(questionLayout)

                    currentItemNumber ++ // Increment the item number for the next iteration
                }
            } else if (examType == "Word Problem") {
                // Calculate the number of word problems
                val wordProblemsCount = itemCount / 5

                for (i in 1..wordProblemsCount) {
                    val questionLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL // Change to vertical orientation
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val wordProblemStart = currentItemNumber
                    val wordProblemEnd = currentItemNumber + 4

                    val numberTextView = TextView(requireContext()).apply {
                        text = "$wordProblemStart - $wordProblemEnd : "
                        textSize = 20f
                    }
                    questionLayout.addView(numberTextView)

                    // Create layouts for Asked, Given, Operation, Number Sentence, Solution/Answer
                    val askedLayout = createTextInputLayout("Asked")
                    val givenLayout =createTextInputLayout("Given")
                    val operationLayout = createTextInputLayout("Operation")
                    val numberSentenceLayout = createTextInputLayout("Number Sentence")
                    val solutionLayout = createTextInputLayout("Solution/Answer")

                    // Add layouts to questionLayout
                    questionLayout.addView(askedLayout)
                    questionLayout.addView(givenLayout)
                    questionLayout.addView(operationLayout)
                    questionLayout.addView(numberSentenceLayout)
                    questionLayout.addView(solutionLayout)

                    answerKeyContainer.addView(questionLayout)

                    currentItemNumber += 5 // Increment item number for the next iteration
                }
            }
        }
    }

    private fun createTextInputLayout(hint: String): TextInputLayout {
        return TextInputLayout(requireContext()).apply {
            this.hint = hint
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.VISIBLE

            val textInputEditText = TextInputEditText(requireContext())
            this.hint = hint
            addView(textInputEditText)
        }

    }

    override fun getFragmentTitle(): String {
        return getString(R.string.edit_key_title)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    private fun setupToolbar() {
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(activity.findViewById(R.id.myToolbar))

        val canGoBack = parentFragmentManager.backStackEntryCount > 0
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(canGoBack)
        activity.supportActionBar?.setDisplayShowHomeEnabled(canGoBack)

        activity.supportActionBar?.title = getFragmentTitle()

        if (canGoBack) {
            activity.findViewById<Toolbar>(R.id.myToolbar).setNavigationOnClickListener {
                activity.onBackPressed()
            }
        }
    }
}
