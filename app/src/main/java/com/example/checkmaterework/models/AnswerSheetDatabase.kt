package com.example.checkmaterework.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized

@Database(entities = [
    AnswerSheetEntity::class,
    ClassEntity::class,
    StudentEntity::class,
    ImageCaptureEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AnswerSheetDatabase: RoomDatabase() {

    abstract fun answerSheetDao(): AnswerSheetDAO
    abstract fun classDao(): ClassDAO // Add the DAO for ClassEntity
    abstract fun studentDao(): StudentDAO
    abstract fun imageCaptureDao(): ImageCaptureDAO

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
                )
                    .fallbackToDestructiveMigration() // This line will reset the database when there is a schema change.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}