package com.example.checkmaterework.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.checkmaterework.R
import com.example.checkmaterework.models.StudentEntity

class StudentAdapter(
    private var studentList: MutableList<StudentEntity>
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder> () {
    class StudentViewHolder(studentsView: View) : RecyclerView.ViewHolder(studentsView) {
        val textLastName: TextView = studentsView.findViewById(R.id.textLastName)
        val textFirstName: TextView = studentsView.findViewById(R.id.textFirstName)
        val textScore: TextView = studentsView.findViewById(R.id.textScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val studentsView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_view_students, parent, false)
        return StudentViewHolder(studentsView)
    }

    override fun getItemCount(): Int {
        return studentList.size
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
       val student = studentList[position]
        holder.textFirstName.text = student.studentName
        holder.textScore.text = student.score.toString()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateStudentList(newStudents: MutableList<StudentEntity>) {
        studentList.clear()
        studentList.addAll(newStudents)
        notifyDataSetChanged()
    }
}