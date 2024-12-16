package com.example.checkmaterework.ui.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.checkmaterework.R
import com.example.checkmaterework.models.AnswerSheetEntity
import com.example.checkmaterework.models.ViewAnalysisItem

class ViewAnalysisAdapter(
    private val analysisList: MutableList<ViewAnalysisItem>
): RecyclerView.Adapter<ViewAnalysisAdapter.ViewAnalysisViewHolder>() {
    class ViewAnalysisViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val textQuestionNumber: TextView = itemView.findViewById(R.id.textQuestionNumber)
        val textCorrectStudents: TextView = itemView.findViewById(R.id.textCorrectStudents)
        val textIncorrectStudents: TextView = itemView.findViewById(R.id.textIncorrectStudents)
        val textRemarks: TextView = itemView.findViewById(R.id.textRemarks)
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
//        val (question, correctCount, incorrectCount) = analysisList[position]
        val analysisItem = analysisList[position]
        holder.textQuestionNumber.text = analysisItem.question
        holder.textCorrectStudents.text = analysisItem.correctCount.toString()
        holder.textIncorrectStudents.text = analysisItem.incorrectCount.toString()
        holder.textRemarks.text = analysisItem.remarks

        // Reset the background and text colors to default
        holder.itemView.setBackgroundColor(Color.WHITE)
        holder.textQuestionNumber.setTextColor(Color.BLACK)
        holder.textCorrectStudents.setTextColor(Color.BLACK)
        holder.textIncorrectStudents.setTextColor(Color.BLACK)
        holder.textRemarks.setTextColor(Color.BLACK)

        // Highlight most and least correctly answered items
        if (analysisItem.isMostCorrect) {
            holder.textQuestionNumber.setTextColor(Color.GREEN)
            holder.textCorrectStudents.setTextColor(Color.GREEN)
            holder.textIncorrectStudents.setTextColor(Color.GREEN)
            holder.textRemarks.setTextColor(Color.GREEN)
            holder.itemView.setBackgroundColor(Color.parseColor("#E0F7FA")) // Light greenish background
        }

        if (analysisItem.isLeastCorrect) {
            holder.textQuestionNumber.setTextColor(Color.RED)
            holder.textCorrectStudents.setTextColor(Color.RED)
            holder.textIncorrectStudents.setTextColor(Color.RED)
            holder.textRemarks.setTextColor(Color.RED)
            holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE")) // Light reddish background
        }

//        // Apply styles based on whether the item is most or least correct
//        when {
//            analysisItem.isMostCorrect -> {
//                holder.textQuestionNumber.setTextColor(Color.GREEN)
//            }
//            analysisItem.isLeastCorrect -> {
//                holder.textQuestionNumber.setTextColor(Color.RED)
//            }
//            else -> {
//                holder.textQuestionNumber.setTextColor(Color.BLACK)
//            }
//        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<ViewAnalysisItem>) {
        analysisList.clear()
        analysisList.addAll(newList)
        notifyDataSetChanged()
    }
}