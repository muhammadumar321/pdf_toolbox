package com.pdftoolbox.app.data.repository

import android.content.Context
import com.pdftoolbox.app.data.models.RecentFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecentFilesRepository(private val context: Context) {

    private val fileName = "recent_files.json"

    suspend fun addRecentFile(uri: String, name: String) = withContext(Dispatchers.IO) {
        val currentList = getRecentFilesInternal().toMutableList()
        // Remove existing if any (to move to top)
        currentList.removeAll { it.uri == uri }
        // Add to top
        currentList.add(0, RecentFile(uri, name, System.currentTimeMillis()))
        saveRecentFiles(currentList)
    }

    suspend fun getRecentFiles(): List<RecentFile> = withContext(Dispatchers.IO) {
        getRecentFilesInternal()
    }

    suspend fun clearRecentFiles() = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun getRecentFilesInternal(): List<RecentFile> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        try {
            val jsonString = FileReader(file).use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<RecentFile>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RecentFile(
                        uri = obj.getString("uri"),
                        name = obj.getString("name"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            return list
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun saveRecentFiles(list: List<RecentFile>) {
        val jsonArray = JSONArray()
        list.forEach { file ->
            val obj = JSONObject()
            obj.put("uri", file.uri)
            obj.put("name", file.name)
            obj.put("timestamp", file.timestamp)
            jsonArray.put(obj)
        }

        val file = File(context.filesDir, fileName)
        try {
            FileWriter(file).use { it.write(jsonArray.toString()) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
