package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.checkmaterework.databinding.FragmentViewSheetDetailsBinding
import com.example.checkmaterework.models.AnswerSheet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ViewSheetDetailsFragment(private val answerSheet: AnswerSheet) : BottomSheetDialogFragment() {

    private lateinit var viewSheetDetailsBinding: FragmentViewSheetDetailsBinding

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            downloadSheetAsPDF()
        } else {
            Toast.makeText(context, "Permission denied to write to storage", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // No need for permission for Android 10+
                downloadSheetAsPDF()
            }
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                downloadSheetAsPDF()
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
        val lineSpacing = 40 // Line spacing between answer fields
        val boxSpacing = 60 // Spacing for Identification boxes
        val largeBoxHeight = boxSpacing * 3 // Height for the Solution/Answer box in Word Problems
        val halfPageWidth = pageWidth / 2 // Half width for Identification answer boxes

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

        // Loop through the exam types and draw fields accordingly
        for (examType in answerSheet.examTypesList) {
            val (type, itemCount) = examType

            // Draw section header
            paint.textSize = 18f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("$type Questions:", margin.toFloat(), yPosition, paint)
            yPosition += 30f

            // Draw fields based on the type
            when (type) {
                "Multiple Choice" -> {
                    // Draw Multiple Choice Fields
                    paint.textSize = 14f
                    paint.style = Paint.Style.STROKE

                    for (i in 1..itemCount) {
                        // Draw item number
                        canvas.drawText("$itemNumber.", margin.toFloat(), yPosition, paint)

                        // Draw circles for A, B, C, D
                        val startX = margin + 30f
                        val bubbleRadius = 10f
                        val gapBetweenBubbles = 50f

                        for (j in 0 until 4) {
                            val bubblesCenterX = startX + j *gapBetweenBubbles
                            canvas.drawCircle(bubblesCenterX, yPosition - 8f, bubbleRadius, paint)

                            // Draw letter inside the circle (adjusted to be centered)
                            paint.style = Paint.Style.FILL // Switch to fill for text
                            paint.textAlign = Paint.Align.CENTER
                            canvas.drawText(('A' + j).toString(), bubblesCenterX, yPosition - 4f, paint)
                            paint.style = Paint.Style.STROKE // Switch back to stroke for circles
                        }

                        yPosition += lineSpacing

                        // Check if we need to create a new page
                        if (yPosition + lineSpacing > pageHeight - margin) {
                            document.finishPage(page) // Finish the current page
                            page = createNewPage(document, pageWidth, pageHeight) // Create a new page
                            canvas = page.canvas
                            yPosition = 180f // Reset yPosition for the new page
                        }

                        itemNumber ++

                    }
                }

                "Identification" -> {
                    // Draw Identification Fields
                    paint.textSize = 14f
                    paint.style = Paint.Style.STROKE

                    for (i in 1..itemCount) {
                        // Draw item number
                        canvas.drawText("$itemNumber.", margin.toFloat(), yPosition, paint)

                        // Draw box for answer (adjusted width to half the page)
                        canvas.drawRect(margin + 30f, yPosition - 20f,
                            margin + 30f + halfPageWidth, yPosition + 20f, paint)

                        yPosition += boxSpacing

                        // Check if we need to create a new page
                        if (yPosition + boxSpacing > pageHeight - margin) {
                            document.finishPage(page) // Finish the current page
                            page = createNewPage(document, pageWidth, pageHeight) // Create a new page
                            canvas = page.canvas
                            yPosition = 180f // Reset yPosition for the new page
                        }

                        itemNumber ++

                    }
                }

                "Word Problem" -> {
                    // Draw Word Problem Fields
                    paint.textSize = 14f
                    paint.style = Paint.Style.STROKE

                    // Calculate the number of word problems
                    val numberOfWordProblems = itemCount / 5

                    for (i in 1..numberOfWordProblems) {
                        // Draw item number range (e.g., 1-5, 6-10)
                        val startRange = itemNumber
                        val endRange = startRange + 4
                        canvas.drawText("$startRange - $endRange.", margin.toFloat(), yPosition, paint)
                        yPosition += 30f // Adjust yPosition to avoid overlap with the next label

                        // Labels and boxes for each Word Problem part
                        val labels = listOf("A", "G", "O", "N", "A")
                        val labelFullText = listOf("Asked", "Given", "Operation", "Number Sentence", "Solution/Answer")
                        val boxHeights = listOf(boxSpacing, boxSpacing, boxSpacing, boxSpacing, largeBoxHeight)

                        for (j in labels.indices) {
                            val label = labels[j]
                            val fullText = labelFullText[j]
                            val boxHeight = boxHeights[j]

                            // Draw label above the box to avoid overlap
                            paint.textAlign = Paint.Align.LEFT
                            paint.style = Paint.Style.FILL
                            canvas.drawText("$fullText ($label):", margin + 30f, yPosition, paint)
                            paint.style = Paint.Style.STROKE

                            // Draw box for answer (half page width)
                            val boxTopY = yPosition + 10f
                            val boxBottomY = boxTopY + (boxHeight - 20f)
                            canvas.drawRect(
                                (margin + 30f), boxTopY,
                                (margin + 30f + halfPageWidth), boxBottomY, paint
                            )

                            yPosition += boxHeight // Move down by box height
                        }

                        // Check if we need to create a new page
                        if (yPosition + largeBoxHeight > pageWidth - margin) {
                            document.finishPage(page) // Finish the current page
                            page = createNewPage(document, pageWidth, pageHeight) // Create a new page
                            canvas = page.canvas
                            yPosition = 180f // Reset yPosition for the new page
                        }

                        itemNumber += 5 // Increment item number by 5 for next word problem
                    }
                }
            }
        }

        // Finish the last page
        document.finishPage(page)

        // Save the PDF to a file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            savePDFUsingMediaStore(document, answerSheet.name)
        } else {
            savePDFUsingFile(document, answerSheet.name)
        }
        document.close()
    }

    private fun createNewPage(document: PdfDocument, pageWidth: Int, pageHeight: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
        return document.startPage(pageInfo)
    }

    private fun savePDFUsingMediaStore(document: PdfDocument, sheetName: String) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${sheetName}_AnswerSheet.pdf")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            }

            val resolver = requireContext().contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    document.writeTo(outputStream)
                }
            }
            Toast.makeText(requireContext(), "PDF saved to Documents", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Error saving PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun savePDFUsingFile(document: PdfDocument, sheetName: String) {
        try {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val filePath = File(directory, "${sheetName}_AnswerSheet.pdf")

            FileOutputStream(filePath).use { outputStream ->
                document.writeTo(outputStream)
            }
//            Toast.makeText(context, "PDF downloaded to ${filePath.absolutePath}", Toast.LENGTH_LONG).show()
            Toast.makeText(requireContext(), "PDF saved to Downloads" ,Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
//            Toast.makeText(context, "Error downloading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            Toast.makeText(requireContext(), "Error saving PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}