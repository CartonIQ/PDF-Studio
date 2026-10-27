package com.pdfstudio.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: String,
    val actionType: String,
    val timestamp: Long,
    val hasWatermark: Boolean
)
