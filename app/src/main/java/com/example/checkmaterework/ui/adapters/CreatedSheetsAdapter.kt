package com.example.checkmaterework.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.checkmaterework.R
import com.example.checkmaterework.models.AnswerSheet

class CreatedSheetsAdapter(
    private var createdSheets: MutableList<AnswerSheet>,
    private val onItemClick: (AnswerSheet) -> Unit, // Click listener for viewing details
    private val onEditClick: (AnswerSheet) -> Unit, // Click listener for editing
    private val onDeleteClick: (AnswerSheet) -> Unit // Click listener for deleting
): RecyclerView.Adapter<CreatedSheetsAdapter.CreatedSheetViewHolder>() {

    class CreatedSheetViewHolder(createdSheetView: View): RecyclerView.ViewHolder(createdSheetView) {
        val createdSheetName: TextView = createdSheetView.findViewById(R.id.createdSheetName)
        val buttonEdit: Button = createdSheetView.findViewById(R.id.buttonEdit)
        val buttonDelete: Button = createdSheetView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreatedSheetViewHolder {
        val createdSheetView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_created_sheets, parent, false)
        return CreatedSheetViewHolder(createdSheetView)
    }

    override fun getItemCount(): Int {
        return createdSheets.size
    }

    override fun onBindViewHolder(holder: CreatedSheetViewHolder, position: Int) {
        val createdSheets = createdSheets[position]
        holder.createdSheetName.text = createdSheets.name

        holder.itemView.setOnClickListener {
            onItemClick(createdSheets) // View details
        }

        holder.buttonEdit.setOnClickListener {
            onEditClick(createdSheets) // Edit functionality
        }

        holder.buttonDelete.setOnClickListener {
            onDeleteClick(createdSheets) // Delete functionality
        }
    }

    fun updateSheetList(newSheets: MutableList<AnswerSheet>) {
        createdSheets.clear()
        createdSheets.addAll(newSheets)
        notifyDataSetChanged()
    }
}