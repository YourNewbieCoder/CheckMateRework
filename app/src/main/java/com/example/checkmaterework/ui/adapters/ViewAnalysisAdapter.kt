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

//    private var mostCorrectIndex: Int = -1
//    private var leastCorrectIndex: Int = -1

    private val mostCorrectIndices = mutableSetOf<Int>()
    private val leastCorrectIndices = mutableSetOf<Int>()


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

        // Apply styles for most and least correctly answered items
        when {
            mostCorrectIndices.contains(position) -> {
                holder.textQuestionNumber.setTextColor(Color.GREEN)
                holder.textCorrectStudents.setTextColor(Color.GREEN)
                holder.textIncorrectStudents.setTextColor(Color.GREEN)
                holder.itemView.setBackgroundColor(Color.parseColor("#E0F7FA"))
            }
            leastCorrectIndices.contains(position) -> {
                holder.textQuestionNumber.setTextColor(Color.RED)
                holder.textCorrectStudents.setTextColor(Color.RED)
                holder.textIncorrectStudents.setTextColor(Color.RED)
                holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE"))
            }
            else -> {
                holder.textQuestionNumber.setTextColor(Color.BLACK)
                holder.textCorrectStudents.setTextColor(Color.BLACK)
                holder.textIncorrectStudents.setTextColor(Color.BLACK)
                holder.itemView.setBackgroundColor(Color.WHITE)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<ViewAnalysisItem>) {
        analysisList.clear()
        analysisList.addAll(newList)

        // Determine indices for highlighting
        mostCorrectIndices.clear()
        leastCorrectIndices.clear()
        newList.forEachIndexed { index, item ->
            if (item.isMostCorrect) {
                mostCorrectIndices.add(index)
            }
            if (item.isLeastCorrect) {
                leastCorrectIndices.add(index)
            }
        }

//        // Determine the most and least correctly answered questions
//        var maxCorrect = Int.MIN_VALUE
//        var minCorrect = Int.MAX_VALUE
//        mostCorrectIndex = -1
//        leastCorrectIndex = -1

//        for ((index, item) in analysisList.withIndex()) {
//            val correctCount = item.second
//            if (correctCount > maxCorrect) {
//                maxCorrect = correctCount
//                mostCorrectIndex = index
//            }
//            if (correctCount < minCorrect) {
//                minCorrect = correctCount
//                leastCorrectIndex = index
//            }
//        }

        notifyDataSetChanged()
    }
}