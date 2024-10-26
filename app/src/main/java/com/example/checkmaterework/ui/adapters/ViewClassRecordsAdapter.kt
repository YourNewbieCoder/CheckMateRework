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
import com.example.checkmaterework.ui.adapters.ClassesAdapter.ClassesViewHolder

class ViewClassRecordsAdapter(
    private val classList: MutableList<ClassEntity>,
    private val onViewClassRecordsClick: (ClassEntity) -> Unit,
    private val onViewClassItemAnalysisClick: (ClassEntity) -> Unit
): RecyclerView.Adapter<ViewClassRecordsAdapter.ViewClassRecordsViewHolder>() {
    class ViewClassRecordsViewHolder(addedClassesView: View) : RecyclerView.ViewHolder(addedClassesView) {
        val addedClassesName: TextView = addedClassesView.findViewById(R.id.addedClassesName)
        val buttonViewClassRecords: Button = addedClassesView.findViewById(R.id.buttonViewClassRecords)
        val buttonViewClassItemAnalysis: Button = addedClassesView.findViewById(R.id.buttonViewClassItemAnalysis)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewClassRecordsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_view_class_records, parent, false)
        return ViewClassRecordsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return classList.size
    }

    override fun onBindViewHolder(holder: ViewClassRecordsViewHolder, position: Int) {
        val addedClasses = classList[position]
        holder.addedClassesName.text = addedClasses.className

        holder.buttonViewClassRecords.setOnClickListener {
            onViewClassRecordsClick(addedClasses)
        }

        holder.buttonViewClassItemAnalysis.setOnClickListener {
            onViewClassItemAnalysisClick(addedClasses)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateClassList(newClasses: List<ClassEntity>) {
        classList.clear()
        classList.addAll(newClasses)
        notifyDataSetChanged()
    }
}