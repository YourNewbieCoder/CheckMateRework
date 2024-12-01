package com.example.checkmaterework.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.checkmaterework.R
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content

class AnswerSheetHelper {
    // Model responses (you can customize this or make it configurable)
    fun getModelResponses(): List<String> {
        return listOf(
            """
                Name: Ayumu Uehara
                Class/Section: 6-Sampaguita
            
                1. (No answer provided)
                2. A
                3. D
                4. (No answer provided)
                5. B
                6. (No answer provided)
                7. 235.20
                8. 4/20
                9. (No answer provided)
                10. 15/20
                11. (No answer provided)
                12. d908
                13. 2730
                14. 284°
                15. 9/8
                16. Addition
                17. Sur 99
                18. (No answer provided)
                19. (No answer provided)
                20. (No answer provided)
            """,
            """
                Name: Chisato Araoshi
                Class/Section: 6-Jade
            
                1-5.
                Asked (A): How many chairs is taken away or left
                Given (G): 19 chairs, 12 chairs
                Operation (O): (No answer provided)
                Number Sentence (N): (No answer provided)
                Solution/Answer (A): 19 - 12 = 7 chairs
                There are 7 chairs left in the auditorium.
            
                6-10.
                Asked (A): (No answer provided)
                Given (G): (No answer provided)
                Operation (O): Division
                Number Sentence (N): 81 / 9 = N
                Solution/Answer (A): 81 ÷ 9 = 9
                There are 9 tables needed to collect all the fruits baskets.
            """,
            """
                Name: Karin Asaka
                Class/Section: Ilang-Ilang
                Date: July 4, 1989
            
                1.  (No answer provided)
                2.  D
                3.  (No answer provided)
                4.  B
                5.  (No answer provided)
                6.  150%
                7.  54
                8.  10 ½
                9.  63 ²/₁₀
                10. (No answer provided)
                11. 90²
                12. 4⁶
                13. (No answer provided)
                14. 3 x 3 x 3
                15. 1021
                16. (No answer provided)
                17. 0193
                18. 9321
                19. (No answer provided)
                20. 3451
            """
        )
    }

    // Image resource IDs
    fun getImageResourceIds(): List<Int> {
        return listOf(
            R.drawable.uehara_4th_summative_test,
            R.drawable.arashi_1st_seatwork,
            R.drawable.asaka_4th_summative_test
            // Add more image resource IDs as needed
        )
    }

    // Function to decode an image resource into a Bitmap
    fun decodeImageResource(context: Context?, resId: Int): Bitmap? {
        return context?.let { BitmapFactory.decodeResource(it.resources, resId) }
    }

    // Generates the chat history with responses and images
    fun generateChatHistoryWithResponses(context: Context?): List<Content> {
        val chatHistory = mutableListOf<Content>()
        val imageResourceIds = getImageResourceIds()
        val modelResponses = getModelResponses()

        // Initial user input
        chatHistory.add(content("user") {
            text(
                """
                You are a math test checker assigned to check grade 6 student answer sheet images and map the answers in each number. 
                There are three types of examination you will check (multiple choice, identification, and word problems). 
                If the test paper is Multiple Choice, you will map the shaded answers to each number. 
                If it is Identification, you will map the answers in the box to each number. 
                If it is a Word Problem (clue: Asked, Given, Operation, Number Sentence, Solution/Answer) you will map it like this:
                
                1.
                Asked: HOW MANY KILOGRAMS OF POTATOES REMAIN?
                Given: Potatoes = 850 and 320
                Operation: SUBTRACTION
                Number Sentence: 850 - 320
                Solution: 850 - 320 = 530
                530 kgs. OF POTATOES REMAIN.

                After that, identify the student's name and section (clue: "Name:" for name, and "Class/Section:" for the section) 
                of the student paper and map it like this:
                
                Name: 
                Class/Section:
                """
            )
        })

        // Initial model response
        chatHistory.add(
            content("model") {
                text("Please provide the images of the student answer sheets. I need to see the images to extract the answers, name, and section. I will then format the output as requested.")
            }
        )

        // Loop through images and generate corresponding chat content
        imageResourceIds.forEachIndexed { index, resId ->
            val imageBitmap  = decodeImageResource(context, resId)

            // Add the image content
            chatHistory.add(content("user") {
                imageBitmap?.let { image(it) }
            })

            // Add the corresponding model response
            chatHistory.add(content("model") {
                text(modelResponses[index])
            })
        }

        return chatHistory
    }

}