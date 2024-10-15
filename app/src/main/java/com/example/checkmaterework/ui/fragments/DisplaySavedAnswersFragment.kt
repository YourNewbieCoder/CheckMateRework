package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentDisplaySavedAnswersBinding
import com.example.checkmaterework.models.AnswerKeyViewModel
import com.example.checkmaterework.models.AnswerKeyViewModelFactory
import com.example.checkmaterework.models.AnswerSheetDatabase

class DisplaySavedAnswersFragment : Fragment() {

    private lateinit var binding: FragmentDisplaySavedAnswersBinding
    private lateinit var answerKeyViewModel: AnswerKeyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ViewModel with AnswerKeyViewModelFactory
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).answerKeyDao()
        answerKeyViewModel = ViewModelProvider(this, AnswerKeyViewModelFactory(dao))
            .get(AnswerKeyViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDisplaySavedAnswersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the passed Answer Sheet ID
        val answerSheetId = arguments?.getInt(ARG_SHEET_ID) ?: return

        // Log the AnswerSheet ID for debugging
        Log.d("DisplaySavedAnswers", "AnswerSheet ID: $answerSheetId")

        // Load the saved answer keys for the given sheet ID
        answerKeyViewModel.loadAnswerKeysForSheet(answerSheetId)

//        answerKeyViewModel.getAnswersForSheet(answerSheetId)

        // Observe and display the saved answers
        answerKeyViewModel.savedAnswerKeys.observe(viewLifecycleOwner) { questions ->
//            Log.d("DisplaySavedAnswersFragment", "Questions loaded: $questions") // Log the questions being observed
            if (questions.isNullOrEmpty()) {
                Log.d("DisplaySavedAnswersFragment", "No questions loaded")
                binding.textViewSavedAnswers.text = getString(R.string.no_answers_found) // Define this in strings.xml
            } else {
                Log.d("DisplaySavedAnswersFragment", "Questions loaded: $questions")
                val savedAnswersText = questions.joinToString("\n") { "Q${it.questionNumber}: ${it.answer}" }
                binding.textViewSavedAnswers.text = savedAnswersText
            }
        }
    }

    companion object {
        private const val ARG_SHEET_ID = "sheet_id"

        fun newInstance(answerSheetId: Int): DisplaySavedAnswersFragment {
            val fragment = DisplaySavedAnswersFragment()
            val args = Bundle()
            args.putInt(ARG_SHEET_ID, answerSheetId)
            fragment.arguments = args
            return fragment
        }
    }
}