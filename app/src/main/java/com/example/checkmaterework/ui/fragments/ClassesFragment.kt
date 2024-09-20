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

        // Set up the toolbar as the support action bar
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(activity.findViewById(R.id.myToolbar))

        // Enable the back button
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity.supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set the toolbar title if needed
        activity.supportActionBar?.title = getString(R.string.classes_title)

        // Set click listener for the back button
        activity.findViewById<Toolbar>(R.id.myToolbar).setNavigationOnClickListener {
            activity.onBackPressed() // Handle the back press
        }

        classesBinding.recyclerViewClasses.layoutManager = LinearLayoutManager(requireContext())

        // Setup RecyclerView Adapter
        classesAdapter = ClassesAdapter(
            mutableListOf(),
            onViewStudentRecordsClick = { classEntity ->  viewStudentRecords(classEntity)}
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

    private fun viewStudentRecords(classEntity: ClassEntity) {
        // Create a new instance of StudentRecordsFragment and pass the classId as an argument
        val studentRecordsFragment = StudentRecordsFragment(classEntity.id) // assuming classEntity has an `id` field

        // Navigate to the StudentRecordsFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, studentRecordsFragment)
            .addToBackStack(null)
            .commit()
    }
}