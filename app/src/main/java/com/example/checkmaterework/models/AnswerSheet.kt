package com.example.checkmaterework.models

class AnswerSheet(
    val name: String,
    val items: Int,
    val examTypesList: List<Pair<String, Int>>
) {
}