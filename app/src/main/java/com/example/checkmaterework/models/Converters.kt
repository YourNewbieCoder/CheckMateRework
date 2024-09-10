package com.example.checkmaterework.models

import androidx.room.TypeConverter

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class Converters {
    @TypeConverter
    fun fromExamTypesList(value: List<Pair<String, Int>>): String {
        val gson = Gson()
        val type = object : TypeToken<List<Pair<String, Int>>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toExamTypesList(value: String): List<Pair<String, Int>> {
        val gson = Gson()
        val type = object : TypeToken<List<Pair<String, Int>>>() {}.type
        return gson.fromJson(value, type)
    }
}
