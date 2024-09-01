package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.checkmaterework.databinding.FragmentViewSheetDetailsBinding
import com.example.checkmaterework.models.AnswerSheet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ViewSheetDetailsFragment(private val answerSheet: AnswerSheet) : BottomSheetDialogFragment() {

    private lateinit var viewSheetDetailsBinding: FragmentViewSheetDetailsBinding
    private val STORAGE_PERMISSION_CODE = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewSheetDetailsBinding = FragmentViewSheetDetailsBinding.inflate(inflater, container, false)
        return viewSheetDetailsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set Answer Sheet details
        viewSheetDetailsBinding.textViewSheetName.text = answerSheet.name
        viewSheetDetailsBinding.textViewNumberOfItems.text = "Total Items: ${answerSheet.items}"

        // Display included exam types
        val examTypeDetails = answerSheet.examTypesList.joinToString("\n\n") {
            "${it.first}: ${it.second} items"
        }
        viewSheetDetailsBinding.textViewExamTypeDetails.text = examTypeDetails

        // Handle download button click
        viewSheetDetailsBinding.buttonDownload.setOnClickListener {
            checkStoragePermission()
        }
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
            ) {
            // Request permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        } else {
            downloadSheetAsPDF()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadSheetAsPDF()
            } else {
                Toast.makeText(context, "Permission denied to write to storage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadSheetAsPDF() {
        // Create a new PDF document
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = document.startPage(pageInfo)

        // Draw content on the PDF
        val canvas = page.canvas
        val paint = android.graphics.Paint()
        var yPosition = 50

        paint.textSize = 16f
        canvas.drawText(answerSheet.name, 10f, yPosition.toFloat(), paint)
        yPosition += 30
        canvas.drawText("Total Items: ${answerSheet.items}", 10f, yPosition.toFloat(), paint)
        yPosition += 30

        // Draw the "Included Types of Exam" header
        canvas.drawText("Included Types of Exam:", 10f, yPosition.toFloat(), paint)
        yPosition += 30

        // Iterate through the exam types list and draw each one on the canvas
        answerSheet.examTypesList.forEach { examType ->
            canvas.drawText("${examType.first}: ${examType.second} items", 10f, yPosition.toFloat(), paint)
            yPosition += 30  // Increment yPosition to avoid overlapping text
        }


        document.finishPage(page)

        // Save the PDF to a file
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val filePath = File(directory, "${answerSheet.name}_AnswerSheet.pdf")

        try {
            FileOutputStream(filePath).use { out ->
                document.writeTo(out)
                Toast.makeText(context, "PDF downloaded to ${filePath.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            Toast.makeText(context, "Error downloading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            document.close()
        }
    }
}