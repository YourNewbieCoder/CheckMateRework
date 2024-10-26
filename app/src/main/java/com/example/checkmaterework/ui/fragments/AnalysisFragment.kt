package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentAnalysisBinding
import com.example.checkmaterework.models.ClassEntity

class AnalysisFragment(private val selectedClass: ClassEntity) : Fragment(), ToolbarTitleProvider {

    private lateinit var analysisBinding: FragmentAnalysisBinding
//    private lateinit var answerSheetViewModel: AnswerSheetViewModel
//    private lateinit var viewAnalysisAdapter: ViewAnalysisAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val dao = AnswerSheetDatabase.getDatabase(requireContext()).answerSheetDao()
//        answerSheetViewModel = ViewModelProvider(this, AnswerSheetViewModelFactory(dao))
//            .get(AnswerSheetViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        analysisBinding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return analysisBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analysisBinding.recyclerViewItemAnalysis.layoutManager = LinearLayoutManager(requireContext())

//        // Set up the adapter
//        viewAnalysisAdapter = ViewAnalysisAdapter(
//            mutableListOf(),
//            onViewAnalysisClick = { sheet -> openCameraToCheckSheet(sheet) }
//        )

//        analysisBinding.recyclerViewCreatedSheets.adapter = viewAnalysisAdapter
//
//        answerSheetViewModel.createdSheetList.observe(viewLifecycleOwner) { sheets ->
//            viewAnalysisAdapter.updateSheetList(sheets)
//        }
    }

//    private fun openCameraToCheckSheet(sheet: AnswerSheetEntity) {
//
//    }

    override fun getFragmentTitle(): String {
        return getString(R.string.students_title)
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