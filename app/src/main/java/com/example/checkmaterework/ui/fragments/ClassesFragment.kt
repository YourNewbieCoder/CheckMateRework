package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
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
import com.example.checkmaterework.ui.adapters.CreatedSheetsAdapter

class ClassesFragment() : Fragment(), ToolbarTitleProvider {
    private lateinit var classesBinding: FragmentClassesBinding
    private lateinit var classViewModel: ClassViewModel
    private lateinit var classesAdapter: ClassesAdapter
//    private lateinit var spinnerClasses: Spinner
//    private lateinit var classesAdapter: ArrayAdapter<String>

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
//        classesAdapter = ClassesAdapter(
//            mutableListOf(),
//            onViewStudentRecordsClick = { classEntity ->  viewStudentRecords(classEntity)}
//        )

        classesAdapter = ClassesAdapter(
            mutableListOf(),
            onItemClick = { classEntity -> showViewStudentsFragment(classEntity)},
            onEditClick = { classEntity -> showEditClassDetailsFragment(classEntity)}, // Pass edit logic
            onDeleteClick = { classEntity -> deleteClass(classEntity)} // Pass delete logic
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

//        // Setup Spinner
//        spinnerClasses = classesBinding.spinnerClasses
//        classesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
//        classesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinnerClasses.adapter = classesAdapter

//        // Observe the data from ViewModel
//        classViewModel.classList.observe(viewLifecycleOwner) { classes ->
//            val classNames = classes.map { it.className }.toMutableList()
//            classNames.add(0, "All Classes") // Add the "All Classes" option as the first entry
//
//            classesAdapter.clear()
//            classesAdapter.addAll(classNames)
//            classesAdapter.notifyDataSetChanged()
//        }

        // Handle selection in Spinner
//        spinnerClasses.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
//                if (position == 0) {
//                    // Show all student records when "All Classes" is selected
//                    viewAllStudentRecords()
//                } else {
//                    // Get the selected class and view student records
//                    val selectedClass = classViewModel.classList.value?.get(position - 1) // Adjust index due to "All Classes"
//                    selectedClass?.let { viewStudentRecords(it) }
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//                // Show all student records when nothing is selected
//                viewAllStudentRecords()
//            }
//        }
    }

    private fun showAddClassDialog() {
        val addClassesFragment = AddClassesFragment(
            onClassAdded = { newClass ->
                classViewModel.addClass(newClass)
            },
            onClassUpdated = { updatedClass ->
                classViewModel.updateClass(updatedClass)
            }
        )
        addClassesFragment.show(parentFragmentManager, addClassesFragment.tag)

    }

    private fun showViewStudentsFragment(classEntity: ClassEntity) {
        val studentFragment = StudentFragment.newInstance(classEntity.classId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, studentFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showEditClassDetailsFragment(classEntity: ClassEntity) {
        val editClassFragment = AddClassesFragment(
            existingClass = classEntity,
            onClassAdded = { newClass ->
                classViewModel.addClass(newClass)
            },
            onClassUpdated = { updatedClass ->
                classViewModel.updateClass(updatedClass)
            }
        )
        editClassFragment.show(parentFragmentManager, editClassFragment.tag)

    }

//    private fun showEditSheetDetailsFragment(sheet: AnswerSheetEntity) {
//        val editSheetFragment = CreateSheetFragment(
//            existingSheet = sheet, // Pass the existing sheet for editing
//            onNewSheetAdded = { newSheet ->
//                answerSheetViewModel.createSheet(newSheet)
//            },
//            onSheetUpdated = { updatedSheet ->
//                answerSheetViewModel.updateSheet(updatedSheet) // Update ViewModel with edited sheet
//            }
//        )
//        editSheetFragment.show(parentFragmentManager, editSheetFragment.tag)
//    }

    private fun deleteClass(classEntity: ClassEntity) {
        classViewModel.deleteClass(classEntity)
    }

//    private fun viewStudentRecords(classEntity: ClassEntity) {
//        // Create a new instance of StudentRecordsFragment and pass the classId as an argument
//        val studentRecordsFragment = StudentRecordsFragment(classEntity.id) // assuming classEntity has an `id` field
//
////        // Navigate to the StudentRecordsFragment
////        parentFragmentManager.beginTransaction()
////            .replace(R.id.frameContainer, studentRecordsFragment)
////            .addToBackStack(null)
////            .commit()
//
//        studentRecordsFragment.show(parentFragmentManager, studentRecordsFragment.tag)
//    }
//
//    private fun viewAllStudentRecords() {
//        val studentRecordsFragment = StudentRecordsFragment(null)
////        parentFragmentManager.beginTransaction()
////            .replace(R.id.frameContainer, studentRecordsFragment)
////            .addToBackStack(null)
////            .commit()
//        studentRecordsFragment.show(parentFragmentManager, studentRecordsFragment.tag)
//
//    }

    override fun getFragmentTitle(): String {
        return getString(R.string.classes_title)
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