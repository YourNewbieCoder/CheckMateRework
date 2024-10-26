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
import com.example.checkmaterework.ui.adapters.CheckSheetsAdapter.CheckSheetViewHolder

class ViewRecordsAdapter(
    private var createdSheets: MutableList<AnswerSheetEntity>,
    private val onViewRecordsClick: (AnswerSheetEntity) -> Unit
//    private val onViewAnalysisClick: (AnswerSheetEntity) -> Unit
): RecyclerView.Adapter<ViewRecordsAdapter.ViewRecordsViewHolder>() {
    class ViewRecordsViewHolder(createdSheetsView: View): RecyclerView.ViewHolder(createdSheetsView) {
        val createdSheetName: TextView = createdSheetsView.findViewById(R.id.createdSheetName)
        val buttonViewRecords: Button = createdSheetsView.findViewById(R.id.buttonViewRecords)
//        val buttonViewItemAnalysis: Button = createdSheetsView.findViewById(R.id.buttonViewItemAnalysis)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewRecordsViewHolder {
        val createdSheetsView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_view_records, parent, false)
        return ViewRecordsViewHolder(createdSheetsView)
    }

    override fun getItemCount(): Int {
        return createdSheets.size
    }

    override fun onBindViewHolder(holder: ViewRecordsViewHolder, position: Int) {
        val createdSheet = createdSheets[position]
        holder.createdSheetName.text = createdSheet.name

        holder.buttonViewRecords.setOnClickListener {
            onViewRecordsClick(createdSheet)
        }

//        holder.buttonViewItemAnalysis.setOnClickListener {
//            onViewAnalysisClick(createdSheet)
//        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateSheetList(newSheets: MutableList<AnswerSheetEntity>) {
        createdSheets.clear()
        createdSheets.addAll(newSheets)
        notifyDataSetChanged()
    }
}