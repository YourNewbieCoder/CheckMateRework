package com.example.checkmaterework.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.checkmaterework.R
import com.example.checkmaterework.models.StudentRecordEntity

class StudentRecordAdapter(
    private var records: MutableList<StudentRecordEntity>,
    private var studentNamesMap: Map<Int, String> = emptyMap(),
    private val onRecordClick: (StudentRecordEntity) -> Unit,
):
    RecyclerView.Adapter<StudentRecordAdapter.StudentRecordViewHolder>() {
    class StudentRecordViewHolder(studentRecordView: View): RecyclerView.ViewHolder(studentRecordView) {
        val textStudentName: TextView = studentRecordView.findViewById(R.id.textStudentName)
        val textScore: TextView = studentRecordView.findViewById(R.id.textScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentRecordViewHolder {
        val studentRecordView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_view_student_records, parent, false)
        return StudentRecordViewHolder(studentRecordView)
    }

    override fun getItemCount(): Int {
        return records.size
    }

    override fun onBindViewHolder(holder: StudentRecordViewHolder, position: Int) {
        val record = records[position]
        holder.textStudentName.text = studentNamesMap[record.studentId] ?: "Unknown"
        holder.itemView.setOnClickListener {
            onRecordClick(record)
        }
        holder.textScore.text = record.score.toString()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateRecords(newRecords: MutableList<StudentRecordEntity>, newNamesMap: Map<Int, String>){
        records.clear()
        records.addAll(newRecords)
        studentNamesMap = newNamesMap
        notifyDataSetChanged()
    }
}