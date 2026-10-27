package com.pdfstudio.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentFileDao {

    @Query("SELECT * FROM recent_files ORDER BY timestamp DESC")
    fun getAllFiles(): LiveData<List<RecentFileEntity>>

    @Query("SELECT * FROM recent_files ORDER BY timestamp DESC")
    fun getAllHistory(): LiveData<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: RecentFileEntity): Long

    @Query("UPDATE recent_files SET hasWatermark = :hasWatermark WHERE id = :id")
    suspend fun updateWatermark(id: Long, hasWatermark: Boolean)

    @Delete
    suspend fun delete(file: RecentFileEntity)

    @Query("DELETE FROM recent_files")
    suspend fun clearAll()
}
