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
    private val onViewRecordsClick: (ClassEntity) -> Unit
) :
RecyclerView.Adapter<ClassesAdapter.ClassesViewHolder>(){
    class ClassesViewHolder(addedClassesView: View) : RecyclerView.ViewHolder(addedClassesView) {
        val addedClassesName: TextView = addedClassesView.findViewById(R.id.addedClassesName)
        val buttonViewRecords: Button = addedClassesView.findViewById(R.id.buttonViewRecords)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_view_classes, parent, false)
        return ClassesViewHolder(view)
    }

    override fun getItemCount(): Int {
        return classList.size
    }

    override fun onBindViewHolder(holder: ClassesViewHolder, position: Int) {
        val addedClasses = classList[position]
        holder.addedClassesName.text = addedClasses.className


    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateClassList(newClasses: List<ClassEntity>) {
        classList.clear()
        classList.addAll(newClasses)
        notifyDataSetChanged()
    }
}