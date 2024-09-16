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
    private var createdSheets: MutableList<AnswerSheetEntity>,
    private val onViewAnalysisClick: (AnswerSheetEntity) -> Unit // Click listener for "Check Sheet" button
): RecyclerView.Adapter<ViewAnalysisAdapter.ViewAnalysisViewHolder>() {
    class ViewAnalysisViewHolder(createdSheetsView: View): RecyclerView.ViewHolder(createdSheetsView) {
        val createdSheetName: TextView = createdSheetsView.findViewById(R.id.createdSheetName)
        val buttonViewAnalysis: Button = createdSheetsView.findViewById(R.id.buttonViewAnalysis)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewAnalysisViewHolder {
        val createdSheetsView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_view_analysis, parent, false)
        return ViewAnalysisViewHolder(createdSheetsView)
    }

    override fun getItemCount(): Int {
        return createdSheets.size
    }

    override fun onBindViewHolder(holder: ViewAnalysisViewHolder, position: Int) {
        val createdSheet = createdSheets[position]
        holder.createdSheetName.text = createdSheet.name

        holder.buttonViewAnalysis.setOnClickListener {
            onViewAnalysisClick(createdSheet) // Trigger the check functionality
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateSheetList(newSheets: MutableList<AnswerSheetEntity>) {
        createdSheets.clear()
        createdSheets.addAll(newSheets)
        notifyDataSetChanged()
    }
}