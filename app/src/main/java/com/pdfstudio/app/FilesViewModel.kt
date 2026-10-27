package com.pdfstudio.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.pdfstudio.app.data.AppDatabase
import com.pdfstudio.app.data.RecentFileDao
import com.pdfstudio.app.data.RecentFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: RecentFileDao = AppDatabase.getDatabase(application).recentFileDao()

    val allFiles: LiveData<List<RecentFileEntity>> = dao.getAllFiles()
    val allHistory: LiveData<List<RecentFileEntity>> = dao.getAllHistory()

    fun insertFile(file: RecentFileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(file)
        }
    }

    fun updateWatermark(id: Long, hasWatermark: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateWatermark(id, hasWatermark)
        }
    }

    fun deleteFile(file: RecentFileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(file)
        }
    }
}
