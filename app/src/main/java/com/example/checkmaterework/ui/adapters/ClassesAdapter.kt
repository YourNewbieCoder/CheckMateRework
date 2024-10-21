package com.example.checkmaterework.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.checkmaterework.R
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.ClassEntity

class ClassesAdapter(
    private val classList: MutableList<ClassEntity>,
    private val onItemClick: (ClassEntity) -> Unit, // Click listener for viewing details
    private val onEditClick: (ClassEntity) -> Unit, // Click listener for editing
    private val onDeleteClick: (ClassEntity) -> Unit // Click listener for deleting
//    private val onViewStudentRecordsClick: (ClassEntity) -> Unit
) :
    RecyclerView.Adapter<ClassesAdapter.ClassesViewHolder>(){
    class ClassesViewHolder(addedClassesView: View) : RecyclerView.ViewHolder(addedClassesView) {
        val addedClassesName: TextView = addedClassesView.findViewById(R.id.addedClassesName)
        val buttonEdit: Button = addedClassesView.findViewById(R.id.buttonEdit)
        val buttonDelete: Button = addedClassesView.findViewById(R.id.buttonDelete)
//        val buttonViewStudentRecords: Button = addedClassesView.findViewById(R.id.buttonViewStudentRecords)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_added_classes, parent, false)
        return ClassesViewHolder(view)
    }

    override fun getItemCount(): Int {
        return classList.size
    }

    override fun onBindViewHolder(holder: ClassesViewHolder, position: Int) {
        val addedClasses = classList[position]
        holder.addedClassesName.text = addedClasses.className

        holder.itemView.setOnClickListener {
            onItemClick(addedClasses) // View details
        }

        holder.buttonEdit.setOnClickListener {
            onEditClick(addedClasses) // Edit functionality
        }

        holder.buttonDelete.setOnClickListener {
            onDeleteClick(addedClasses) // Delete functionality
        }

//        holder.buttonViewStudentRecords.setOnClickListener {
//            onViewStudentRecordsClick(addedClasses)
//        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateClassList(newClasses: List<ClassEntity>) {
        classList.clear()
        classList.addAll(newClasses)
        notifyDataSetChanged()
    }
}