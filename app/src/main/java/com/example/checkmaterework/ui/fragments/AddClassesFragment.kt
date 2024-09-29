package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentAddClassesBinding
import com.example.checkmaterework.models.AnswerSheetDatabase
import com.example.checkmaterework.models.ClassEntity
import com.example.checkmaterework.models.ClassViewModel
import com.example.checkmaterework.models.ClassViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddClassesFragment(
    private val onClassAdded: (ClassEntity) -> Unit // Callback to add the class
) : BottomSheetDialogFragment() {

    private lateinit var addClassesBinding: FragmentAddClassesBinding
    private lateinit var classViewModel: ClassViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup ViewModel
        val dao = AnswerSheetDatabase.getDatabase(requireContext()).classDao()
        classViewModel = ViewModelProvider(this, ClassViewModelFactory(dao))
            .get(ClassViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        addClassesBinding = FragmentAddClassesBinding.inflate(inflater, container, false)
        return addClassesBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the save button click listener
        addClassesBinding.buttonSaveClass.setOnClickListener {
            val className = addClassesBinding.textInputClassName.text.toString()

            if (className.isNotEmpty()) {
                val newClass = ClassEntity(className = className)
                onClassAdded(newClass) // Pass the class name back to the parent fragment
                dismiss()
            } else {
                addClassesBinding.textInputClassName.error = "Please enter a class name"
            }
        }
    }
}