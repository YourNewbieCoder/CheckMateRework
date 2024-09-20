package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentKeyBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.AnswerSheetViewModel
import com.example.checkmaterework.models.AnswerSheetViewModelFactory
import com.example.checkmaterework.ui.adapters.EditKeyAdapter

class KeyFragment : Fragment() {

    private lateinit var keyBinding: FragmentKeyBinding
    private lateinit var answerSheetViewModel: AnswerSheetViewModel
    private lateinit var editKeyAdapter: EditKeyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).answerSheetDao()
        answerSheetViewModel = ViewModelProvider(this, AnswerSheetViewModelFactory(dao))
            .get(AnswerSheetViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        keyBinding = FragmentKeyBinding.inflate(inflater, container, false)
        return keyBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        keyBinding.recyclerViewCreatedSheets.layoutManager = LinearLayoutManager(requireContext())

        // Set up the adapter
        editKeyAdapter = EditKeyAdapter(
            mutableListOf(),
            onEditKeyClick = { sheet -> showEditAnswerKeyFragment(sheet) }
        )

        keyBinding.recyclerViewCreatedSheets.adapter = editKeyAdapter

        answerSheetViewModel.createdSheetList.observe(viewLifecycleOwner) { sheets ->
            editKeyAdapter.updateSheetList(sheets)
        }
    }

    private fun showEditAnswerKeyFragment(sheet: AnswerSheetEntity) {
        val editAnswerKeyFragment = EditAnswerKeyFragment(sheet)

        // Set up the toolbar as the support action bar
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(activity.findViewById(R.id.myToolbar))

        // Enable the back button
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity.supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set the toolbar title if needed
        activity.supportActionBar?.title = getString(R.string.edit_key_title)

        // Set click listener for the back button
        activity.findViewById<Toolbar>(R.id.myToolbar).setNavigationOnClickListener {
            handleBackButtonClick() // Custom function to handle back button logic
        }

        // Replace the current fragment and add to back stack
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, editAnswerKeyFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleBackButtonClick() {
        // Hide the toolbar and back arrow
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // Set the toolbar title if needed
        activity.supportActionBar?.title = getString(R.string.key_title)

        // Pop the fragment from the back stack (or navigate back to the previous fragment)
        parentFragmentManager.popBackStack() // This will remove the current fragment from the back stack
    }
}