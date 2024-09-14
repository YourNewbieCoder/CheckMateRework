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

class CheckSheetsAdapter(
    private var createdSheets: MutableList<AnswerSheetEntity>,
    private val onCheckClick: (AnswerSheetEntity) -> Unit // Click listener for "Check Sheet" button
): RecyclerView.Adapter<CheckSheetsAdapter.CheckSheetViewHolder>() {
    class CheckSheetViewHolder(createdSheetsView: View): RecyclerView.ViewHolder(createdSheetsView) {
        val createdSheetName: TextView = createdSheetsView.findViewById(R.id.createdSheetName)
        val buttonCheckSheet: Button = createdSheetsView.findViewById(R.id.buttonCheckSheet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckSheetViewHolder {
        val createdSheetsView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_check_sheets, parent, false)
        return CheckSheetViewHolder(createdSheetsView)
    }

    override fun getItemCount(): Int {
        return createdSheets.size
    }

    override fun onBindViewHolder(holder: CheckSheetViewHolder, position: Int) {
        val createdSheet = createdSheets[position]
        holder.createdSheetName.text = createdSheet.name

        holder.buttonCheckSheet.setOnClickListener {
            onCheckClick(createdSheet) // Trigger the check functionality
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateSheetList(newSheets: MutableList<AnswerSheetEntity>) {
        createdSheets.clear()
        createdSheets.addAll(newSheets)
        notifyDataSetChanged()
    }
}