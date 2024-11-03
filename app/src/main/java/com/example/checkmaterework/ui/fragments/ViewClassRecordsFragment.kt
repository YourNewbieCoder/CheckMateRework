package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentViewClassRecordsBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.ClassEntity
import com.example.checkmaterework.models.ClassViewModel
import com.example.checkmaterework.models.ClassViewModelFactory
import com.example.checkmaterework.ui.adapters.ViewClassRecordsAdapter

class ViewClassRecordsFragment : Fragment(), ToolbarTitleProvider {

    private lateinit var binding: FragmentViewClassRecordsBinding
    private lateinit var classViewModel: ClassViewModel
    private lateinit var viewClassRecordsAdapter: ViewClassRecordsAdapter
    private var answerSheetName: String? = null
    private var answerSheetId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the answer sheet name and ID from arguments
        answerSheetName = arguments?.getString("answerSheetName")
        answerSheetId = arguments?.getInt("answerSheetId")

        // Setup ViewModel
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).classDao()
        classViewModel = ViewModelProvider(this, ClassViewModelFactory(dao))
            .get(ClassViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentViewClassRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewClasses.layoutManager = LinearLayoutManager(requireContext())

        // Setup RecyclerView Adapter
        viewClassRecordsAdapter = ViewClassRecordsAdapter(
            mutableListOf(),
            onViewClassRecordsClick = { classEntity -> displayClassRecords(classEntity)},
            onViewClassItemAnalysisClick = { classEntity -> displayClassItemAnalysis(classEntity)}, // Pass edit logic
        )

        binding.recyclerViewClasses.adapter = viewClassRecordsAdapter

        // Set the adapter to the RecyclerView
        binding.recyclerViewClasses.adapter = viewClassRecordsAdapter

        // Observe the data from ViewModel
        classViewModel.classList.observe(viewLifecycleOwner) { classes ->
            viewClassRecordsAdapter.updateClassList(classes)
        }
    }

    private fun displayClassRecords(classEntity: ClassEntity) {
        val studentRecordsFragment = StudentRecordsFragment.newInstance(classEntity, answerSheetName, answerSheetId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, studentRecordsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun displayClassItemAnalysis(classEntity: ClassEntity) {
        val analysisFragment = AnalysisFragment.newInstance(classEntity, answerSheetName, answerSheetId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, analysisFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.view_records_title, answerSheetName)
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