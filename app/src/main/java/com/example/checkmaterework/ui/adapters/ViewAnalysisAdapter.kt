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

class ViewAnalysisAdapter(
    private val analysisList: MutableList<Triple<String, Int, Int>>
): RecyclerView.Adapter<ViewAnalysisAdapter.ViewAnalysisViewHolder>() {
    class ViewAnalysisViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val textQuestionNumber: TextView = itemView.findViewById(R.id.textQuestionNumber)
        val textCorrectStudents: TextView = itemView.findViewById(R.id.textCorrectStudents)
        val textIncorrectStudents: TextView = itemView.findViewById(R.id.textIncorrectStudents)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewAnalysisViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_view_item_analysis, parent, false)
        return ViewAnalysisViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return analysisList.size
    }

    override fun onBindViewHolder(holder: ViewAnalysisViewHolder, position: Int) {
        val (question, correctCount, incorrectCount) = analysisList[position]
        holder.textQuestionNumber.text = question
        holder.textCorrectStudents.text = correctCount.toString()
        holder.textIncorrectStudents.text = incorrectCount.toString()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<Triple<String, Int, Int>>) {
        analysisList.clear()
        analysisList.addAll(newList)
        notifyDataSetChanged()
    }
}