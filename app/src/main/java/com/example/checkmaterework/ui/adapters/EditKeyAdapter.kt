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

class EditKeyAdapter(
    private var createdSheets: MutableList<AnswerSheetEntity>,
    private val onEditKeyClick: (AnswerSheetEntity) -> Unit // Click listener for "Check Sheet" button
): RecyclerView.Adapter<EditKeyAdapter.EditKeyViewHolder>() {
    class EditKeyViewHolder(createdSheetsView: View): RecyclerView.ViewHolder(createdSheetsView) {
        val createdSheetName: TextView = createdSheetsView.findViewById(R.id.createdSheetName)
        val buttonEditKey: Button = createdSheetsView.findViewById(R.id.buttonEditKey)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditKeyViewHolder {
        val createdSheetsView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_edit_key, parent, false)
        return EditKeyViewHolder(createdSheetsView)
    }

    override fun getItemCount(): Int {
        return createdSheets.size
    }

    override fun onBindViewHolder(holder: EditKeyViewHolder, position: Int) {
        val createdSheet = createdSheets[position]
        holder.createdSheetName.text = createdSheet.name

        holder.buttonEditKey.setOnClickListener {
            onEditKeyClick(createdSheet) // Trigger the check functionality
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateSheetList(newSheets: MutableList<AnswerSheetEntity>) {
        createdSheets.clear()
        createdSheets.addAll(newSheets)
        notifyDataSetChanged()
    }
}