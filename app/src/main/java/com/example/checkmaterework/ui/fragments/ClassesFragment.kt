package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.checkmaterework.databinding.FragmentClassesBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.ClassEntity
import com.example.checkmaterework.models.ClassViewModel
import com.example.checkmaterework.models.ClassViewModelFactory
import com.example.checkmaterework.ui.adapters.ClassesAdapter

class ClassesFragment(private val sheet: AnswerSheetEntity) : Fragment() {
    private lateinit var classesBinding: FragmentClassesBinding
    private lateinit var classViewModel: ClassViewModel
    private lateinit var classesAdapter: ClassesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup ViewModel
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).classDao()
        classViewModel = ViewModelProvider(this, ClassViewModelFactory(dao))
            .get(ClassViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        classesBinding = FragmentClassesBinding.inflate(inflater, container, false)
        return classesBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        classesBinding.recyclerViewClasses.layoutManager = LinearLayoutManager(requireContext())

        // Setup RecyclerView Adapter
        classesAdapter = ClassesAdapter(
            mutableListOf(),
            onViewRecordsClick = { classEntity ->  viewRecords(classEntity)}
        )

        // Set the adapter to the RecyclerView
        classesBinding.recyclerViewClasses.adapter = classesAdapter

        // Observe the data from ViewModel
        classViewModel.classList.observe(viewLifecycleOwner) { classes ->
            classesAdapter.updateClassList(classes)
        }

        classesBinding.buttonAddClass.setOnClickListener {
            showAddClassDialog()
        }
    }

    private fun showAddClassDialog() {
        val addClassesFragment = AddClassesFragment(
            onClassAdded = { newClass ->
                classViewModel.createClass(newClass)
            }
        )
        addClassesFragment.show(parentFragmentManager, addClassesFragment.tag)

    }

    private fun viewRecords(classEntity: ClassEntity) {
        // Handle record viewing logic for the selected class
    }
}