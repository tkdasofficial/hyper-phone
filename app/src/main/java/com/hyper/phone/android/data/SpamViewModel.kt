package com.hyper.phone.android.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class SpamViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        SpamDatabase::class.java, "spam-database"
    ).build()
    
    val settingsManager = SettingsManager(application)
    
    private val spamDao = db.spamDao()

    val spamList = spamDao.getAllSpam()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSpam(number: String, type: String = "exact") {
        viewModelScope.launch {
            spamDao.insert(SpamNumber(number, type))
        }
    }

    fun removeSpam(spamNumber: SpamNumber) {
        viewModelScope.launch {
            spamDao.delete(spamNumber)
        }
    }

    fun exportToCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val list = spamList.value
                    val csvHeader = "number,type\n"
                    outputStream.write(csvHeader.toByteArray())
                    list.forEach { spam ->
                        val line = "\${spam.number},\${spam.type}\n"
                        outputStream.write(line.toByteArray())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line = reader.readLine() // skip header
                    if (line != null && line.contains("number,type")) {
                        line = reader.readLine()
                    }
                    while (line != null) {
                        val parts = line.split(",")
                        if (parts.size >= 2) {
                            spamDao.insert(SpamNumber(parts[0], parts[1]))
                        }
                        line = reader.readLine()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
