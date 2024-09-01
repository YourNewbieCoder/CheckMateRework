package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
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

        // Set up PDF page dimensions
        val pageWidth = 595 // A4 width in points (8.27 inches)
        val pageHeight = 842 // A4 height in points (11.69 inches)
        val margin = 40 // Margin for content within the page
        val lineSpacing = 20 // Line spacing between answer fields

        // Create the first page for the PDF
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()

        // Draw Header Information
        paint.textSize = 24f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(answerSheet.name, (pageWidth / 2).toFloat(), 60f, paint) // Title centered

        // Draw fields for Name, Teacher, Class/Section, Date
        paint.textSize = 14f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Name: _______________________________", margin.toFloat(), 100f, paint)
        canvas.drawText("Teacher: ____________________________", pageWidth / 2 + 10f, 100f, paint)

        canvas.drawText("Class/Section: _______________________", margin.toFloat(), 130f, paint)
        canvas.drawText("Date: _______________________________", pageWidth / 2 + 10f, 130f, paint)

        // Draw Answer Fields Dynamically
        var yPosition = 180f // Starting position for answers below the header and fields
        var itemNumber = 1

        for (i in 1..answerSheet.items) {
            // Draw item number and an answer line
            canvas.drawText("$itemNumber.", margin.toFloat(), yPosition, paint)
            canvas.drawLine(70f, yPosition - 10f, pageWidth - margin.toFloat(), yPosition - 10f, paint)

            // Update yPosition for the next answer field
            yPosition += lineSpacing

            // Check if we need to create a new page
            if (yPosition + lineSpacing > pageHeight - margin) {
                document.finishPage(page) // Finish the current page

                // Start a new page
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas // Update the canvas for the new page

                // Redraw the header for the new page
                paint.textSize = 24f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(answerSheet.name, (pageWidth / 2).toFloat(), 60f, paint)

                paint.textSize = 14f
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Name: __________________________", margin.toFloat(), 100f, paint)
                canvas.drawText("Teacher: _______________________", pageWidth / 2 + 10f, 100f, paint)

                canvas.drawText("Class/Section: __________________", margin.toFloat(), 130f, paint)
                canvas.drawText("Date: __________________________", pageWidth / 2 + 10f, 130f, paint)

                yPosition = 180f // Reset yPosition for the new page
            }

            itemNumber ++
        }

        // Finish the last page
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