package com.example.checkmaterework.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ImageCaptureDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageCapture(imageCapture: ImageCaptureEntity)

    @Update
    suspend fun updateImageCapture(imageCapture: ImageCaptureEntity)

    @Delete
    suspend fun deleteImageCapture(imageCapture: ImageCaptureEntity)

    @Query("SELECT * FROM image_captures")
    suspend fun getAllImageCaptures(): List<ImageCaptureEntity>

    @Query("SELECT * FROM image_captures WHERE id = :id")
    suspend fun getImageCaptureById(id: Int): ImageCaptureEntity?

    @Query("SELECT * FROM image_captures WHERE sheetId = :sheetId")
    suspend fun getImageCapturesBySheetId(sheetId: Int): List<ImageCaptureEntity>
}