package com.karwa.mdtnavigation.log

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LocalLogger(private val context: Context) {
    var currentDate: String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    val dateTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

    fun logEvent(event: String) {
        val time = dateTime.format(Date())
        try {
            val logFile = File(context.filesDir, currentDate + "_event_logs.txt")
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
            Log.e("ASD","$time\t$event\n")
            FileWriter(logFile, true).buffered().use { writer ->
                writer.append("$time\t$event\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}