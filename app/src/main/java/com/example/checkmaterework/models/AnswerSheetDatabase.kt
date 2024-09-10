package com.example.checkmaterework.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized

@Database(entities = [AnswerSheetEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AnswerSheetDatabase: RoomDatabase() {
    abstract fun answerSheetDao(): AnswerSheetDAO

    companion object {
        @Volatile
        private var INSTANCE: AnswerSheetDatabase? = null

        @OptIn(InternalCoroutinesApi::class)
        fun getDatabase(context: Context): AnswerSheetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnswerSheetDatabase::class.java,
                    "answer_sheet_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}