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

class KeyFragment : Fragment(), ToolbarTitleProvider {

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

        // Replace the current fragment and add to back stack
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, editAnswerKeyFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.key_title)
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