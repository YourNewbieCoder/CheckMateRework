package com.example.checkmaterework.ui.fragments

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class YourImageAnalyzer : ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // Pass image to text recognizer
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image).addOnSuccessListener { visionText ->
                // Iterate over blocks of recognized text
                for (block in visionText.textBlocks) {
                    val boundingBox = block.boundingBox
                    val cornerPoints = block.cornerPoints
                    val text = block.text
                    Log.d("TextBlock", "Detected text block: $text")

                    // Further break down into lines
                    for (line in block.lines) {
                        Log.d("TextLine", "Detected text line: ${line.text}")

                        // Further break down into elements (words or symbols)
                        for (element in line.elements) {
                            Log.d("TextElement", "Detected text element: ${element.text}")
                        }
                    }
                }
            // Once processing is done, close the image
            imageProxy.close()
        }
            .addOnFailureListener { e ->
                Log.e("TextRecognition", "Error during text recognition: ${e.message}")
                imageProxy.close()
            }
        } else {
            // Close the image proxy if mediaImage is null
            imageProxy.close()
        }
    }
}