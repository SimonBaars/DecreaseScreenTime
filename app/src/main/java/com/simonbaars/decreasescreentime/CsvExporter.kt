package com.simonbaars.decreasescreentime

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    private const val HISTORY_PREFS = "screen_time_history"
    private const val FILENAME = "screen_time_history.csv"
    
    fun saveHistoryEntry(context: Context, date: String, screenTimeMs: Long, unlockCount: Int) {
        val prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
        val key = "history_$date"
        prefs.edit()
            .putString(key, "$screenTimeMs,$unlockCount")
            .apply()
    }
    
    fun getAllHistory(context: Context): List<DailyStats> {
        val prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
        val allEntries = prefs.all
        val history = mutableListOf<DailyStats>()
        
        for ((key, value) in allEntries) {
            if (key.startsWith("history_") && value is String) {
                val date = key.removePrefix("history_")
                val parts = value.split(",")
                if (parts.size == 2) {
                    val screenTimeMs = parts[0].toLongOrNull() ?: 0L
                    val unlockCount = parts[1].toIntOrNull() ?: 0
                    history.add(DailyStats(date, screenTimeMs, unlockCount))
                }
            }
        }
        
        return history.sortedBy { it.date }
    }
    
    fun exportToCsv(context: Context): File? {
        try {
            val history = getAllHistory(context)
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(downloadsDir, "screen_time_history_$timestamp.csv")
            
            FileWriter(file).use { writer ->
                writer.append("Date,Screen Time (minutes),Unlock Count\n")
                
                for (stats in history) {
                    val minutes = stats.screenTimeMs / (60 * 1000)
                    writer.append("${stats.date},$minutes,${stats.unlockCount}\n")
                }
            }
            
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
