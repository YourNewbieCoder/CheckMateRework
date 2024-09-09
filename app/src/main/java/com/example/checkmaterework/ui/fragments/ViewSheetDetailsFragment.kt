package com.example.checkmaterework.ui.fragments

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Canvas
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
        val lineSpacing = 30 // Line spacing between answer fields
        val halfPageWidth = (pageWidth - (2 * margin)) / 2 // Half width for Identification answer boxes
        val columnWidth = (pageWidth - (2 * margin)) / 3 // Dividing space for three possible columns

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

        // Initialize variables
        var currentPage = 0
        var currentColumn = 0

        // Calculate total items for overall column management
        var totalItems = 0
        answerSheet.examTypesList.forEach { (_, itemCount) -> totalItems += itemCount }

        // Determine the overall columns needed for all types combined
        val overallColumnsNeeded = when {
            totalItems <= 20 -> 1
            totalItems <= 40 -> 2
            else -> 3
        }
//
//        // Initialize global tracking for column and positions
//        var currentColumn = 0
//        var currentPage = 0
//        var yPosition = 180f
//        var itemNumber = 1

        // Loop through the exam types and draw fields accordingly
        for (examType in answerSheet.examTypesList) {
            val (type, itemCount) = examType
            // Determine the number of columns needed for each type
//            val columnsNeeded = when (type) {
//                "Multiple Choice", "Identification" -> {
//                    when {
//                        itemCount <= 20 -> 1
//                        itemCount <= 40 -> 2
//                        else -> 3
//                    }
//                }
//                "Word Problem" -> {
//                    // Each column can hold up to 2 problems (5 points each, 10 points per column)
//                    when {
//                        itemCount <= 10  -> 1
//                        itemCount <= 20 -> 2
//                        else -> 3
//                    }
//                }
//                else -> 1
//            }

            // Calculate items per column for each type
            val maxItemsPerColumn = when (type) {
                "Multiple Choice", "Identification" -> 20
                "Word Problem" -> 2
                else -> 20
            }

            // Draw fields based on the type
            when (type) {
                "Multiple Choice" -> {
                    // Draw Multiple Choice Fields
                    paint.textSize = 12f
                    paint.style = Paint.Style.STROKE

                    for (i in 1..itemCount) {
                        // Calculate the x-position based on the column
                        val xPosition = margin + (currentColumn * (columnWidth + 20f))

//                        // Reset yPosition for each new column
//                        if ((itemNumber - 1) % maxItemsPerColumn == 0 && itemNumber > 1) {
//                            yPosition = 180f
//                        }

                        // Draw item number
                        canvas.drawText("$itemNumber.", xPosition, yPosition, paint)

                        // Draw circles for A, B, C, D
                        val startX = xPosition + 30f
                        val bubbleRadius = 8f
                        val gapBetweenBubbles = 25f

                        for (j in 0 until 4) {
                            val bubblesCenterX = startX + j * gapBetweenBubbles
                            canvas.drawCircle(bubblesCenterX, yPosition - 8f, bubbleRadius, paint)

                            // Draw letter inside the circle (adjusted to be centered)
                            paint.style = Paint.Style.FILL // Switch to fill for text
                            paint.textAlign = Paint.Align.CENTER
                            canvas.drawText(('A' + j).toString(), bubblesCenterX, yPosition - 4f, paint)
                            paint.style = Paint.Style.STROKE // Switch back to stroke for circles
                        }

                        yPosition += lineSpacing

                        // Check if we need to create a new page or move to a new column
                        if (yPosition + lineSpacing > pageHeight - margin) {
                            if (currentColumn == overallColumnsNeeded - 1) {
                                // If the last column is filled, create a new page and reset to the first column
                                document.finishPage(page) // Finish the current page
                                page = createNewPage(document, pageWidth, pageHeight) // Create a new page
                                canvas = page.canvas
                                yPosition = 180f // Reset yPosition for the new page
                                currentColumn = 0 // Reset column index for new page
                                currentPage++ // Increment current page
                            } else {
                                // Move to the next column on the same page
                                currentColumn++
                                yPosition = 180f // Reset yPosition for the new column
                            }
                        }

                        itemNumber ++

                    }
                }

                "Identification" -> {
                    // Draw Identification Fields
                    paint.textSize = 12f
                    paint.style = Paint.Style.STROKE

                    // Adjusted box height to fit more items
                    val identificationBoxHeight = 21f  // Reduced box height to fit more items
                    val verticalSpacing = 10f // Reduced spacing between items
                    val identificationBoxWidth = columnWidth - 40f  // Adjusted width to fit three columns

                    for (i in 1..itemCount) {
                        // Calculate the x-position based on the column
                        val xPosition = margin + (currentColumn * (columnWidth + 20f))

//                        // Reset yPosition for each new column
//                        if((itemNumber - 1) % maxItemsPerColumn == 0 && itemNumber > 1) {
//                            yPosition = 180f
//                        }

                        // Draw item number
                        canvas.drawText("$itemNumber.", xPosition, yPosition, paint)

                        // Draw box for answer
                        canvas.drawRect(
                            xPosition + 20f,
                            yPosition - identificationBoxHeight / 2,
                            xPosition + 20f + identificationBoxWidth,
                            yPosition + identificationBoxHeight / 2, paint)

                        yPosition += identificationBoxHeight + verticalSpacing

                        // Check if we need to create a new page
                        if (yPosition + identificationBoxHeight > pageHeight - margin) {
                            if (currentColumn == overallColumnsNeeded - 1) {
                                // If the last column is filled, create a new page and reset to the first column
                                document.finishPage(page) // Finish the current page
                                page = createNewPage(document, pageWidth, pageHeight) // Create a new page
                                canvas = page.canvas
                                yPosition = 180f // Reset yPosition for the new page
                                currentColumn = 0 // Reset column index for new page
                                currentPage++ // Increment current page
                            } else {
                                // Move to the next column on the same page
                                currentColumn++
                                yPosition = 180f // Reset yPosition for the new column
                            }
                        }

                        itemNumber ++

                    }
                }

                "Word Problem" -> {
                    // Draw Word Problem Fields
                    paint.textSize = 12f
                    paint.style = Paint.Style.STROKE

                    // Adjusted box sizes to fit more on a page
                    val wordProblemBoxHeight = 25f  // Similar to Identification Box Height
                    val solutionBoxHeight = wordProblemBoxHeight * 3
                    val verticalSpacing = 20f       // Adjusted spacing to fit more items
                    val wordProblemBoxWidth = columnWidth - 40f  // Adjusted width to fit in columns

                    // Calculate the number of word problems
                    val numberOfWordProblems = itemCount / 5

                    for (i in 1..numberOfWordProblems) {
                        // Calculate the x-position based on the column
                        val xPosition = margin + (currentColumn * (columnWidth + 20f))

//                        // Reset yPosition for each new column
//                        if ((i - 1) % maxItemsPerColumn == 0 && i > 1) {
//                            yPosition = 180f
//                        }

                        // Draw item number range (e.g., 1-5, 6-10)
//                        val startRange = itemNumber
//                        val endRange = startRange + 4
//                        canvas.drawText("$startRange - $endRange.", xPosition, yPosition, paint)
                        canvas.drawText("$itemNumber.", xPosition, yPosition, paint)
                        yPosition += 20f // Adjust yPosition to avoid overlap with the next label

                        // Labels and boxes for each Word Problem part
                        val labels = listOf("A", "G", "O", "N", "A")
                        val labelFullText = listOf("Asked", "Given", "Operation", "Number Sentence", "Solution/Answer")
                        val boxHeights = listOf(wordProblemBoxHeight, wordProblemBoxHeight, wordProblemBoxHeight, wordProblemBoxHeight, solutionBoxHeight)

                        for (j in labels.indices) {
                            val label = labels[j]
                            val fullText = labelFullText[j]
                            val boxHeight = boxHeights[j]

                            // Draw label above the box to avoid overlap
                            paint.textAlign = Paint.Align.LEFT
                            paint.style = Paint.Style.FILL
                            canvas.drawText("$fullText ($label):", xPosition + 20f, yPosition, paint)
                            paint.style = Paint.Style.STROKE

                            // Draw box for answer
                            val boxTopY = yPosition + 10f
                            val boxBottomY = boxTopY + boxHeight
                            canvas.drawRect(
                                (xPosition + 20f), boxTopY,
                                (xPosition + 20f + wordProblemBoxWidth), boxBottomY, paint
                            )

                            yPosition += boxHeight + 20f // Move down by box height + adjusted spacing
                        }

                        yPosition += verticalSpacing // Add additional spacing for the next problem

                        // Check if we need to create a new page or move to a new column
                        if (yPosition + solutionBoxHeight > pageHeight - margin) {
                            if (currentColumn == overallColumnsNeeded - 1) {
                                // If the last column is filled, create a new page and reset to the first column
                                document.finishPage(page) // Finish the current page
                                page = createNewPage(document, pageWidth, pageHeight) // Create a new page
                                canvas = page.canvas
                                yPosition = 180f // Reset yPosition for the new page
                                currentColumn = 0 // Reset column index for new page
                                currentPage++ // Increment current page
                            } else {
                                // Move to the next column on the same page
                                currentColumn++
                                yPosition = 180f // Reset yPosition for the new column
                            }
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