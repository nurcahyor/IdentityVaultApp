package com.identityvault.app.log

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetailedLogEntry(
    val timestamp: Long,
    val level: String,
    val packageName: String,
    val sdkInt: Int,
    val category: String,
    val message: String,
    val detail: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("timestamp", timestamp)
        .put("level", level)
        .put("packageName", packageName)
        .put("sdkInt", sdkInt)
        .put("category", category)
        .put("message", message)
        .put("detail", detail)

    fun toDisplay(): String {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestamp))
        val displaySdk = if (sdkInt > 0) sdkInt else Build.VERSION.SDK_INT
        val packageLine = if (packageName.isBlank()) "local - SDK $displaySdk" else "$packageName - SDK $displaySdk"
        val detailLine = if (detail.isBlank()) "" else "\n$detail"
        return "[$level] $category - $time\n$packageLine\n$message$detailLine"
    }

    companion object {
        fun fromJson(json: JSONObject): DetailedLogEntry = DetailedLogEntry(
            timestamp = json.optLong("timestamp"),
            level = json.optString("level", "INFO"),
            packageName = json.optString("packageName"),
            sdkInt = json.optInt("sdkInt"),
            category = json.optString("category", "APP"),
            message = json.optString("message"),
            detail = json.optString("detail")
        )
    }
}

class DetailedLogRepository(context: Context) {
    private val prefs = context.getSharedPreferences("detailed_logs", Context.MODE_PRIVATE)

    fun add(
        level: String,
        category: String,
        message: String,
        packageName: String = "",
        sdkInt: Int = 0,
        detail: String = ""
    ) {
        val updated = getEntries().toMutableList()
        updated += DetailedLogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            packageName = packageName,
            sdkInt = if (sdkInt > 0) sdkInt else Build.VERSION.SDK_INT,
            category = category,
            message = message,
            detail = detail
        )
        while (updated.size > MAX_LOGS) updated.removeAt(0)
        prefs.edit().putString(KEY_ENTRIES, JSONArray(updated.map { it.toJson() }).toString()).apply()
    }

    fun getEntries(): List<DetailedLogEntry> {
        val arr = JSONArray(prefs.getString(KEY_ENTRIES, "[]") ?: "[]")
        return List(arr.length()) { DetailedLogEntry.fromJson(arr.optJSONObject(it) ?: JSONObject()) }
    }

    fun getDisplayLines(level: String = "ALL"): List<String> {
        return getEntries()
            .filter { level == "ALL" || it.level.equals(level, ignoreCase = true) }
            .map { it.toDisplay() }
    }

    fun summary(): Map<String, Int> {
        val entries = getEntries()
        return mapOf(
            "SUCCESS" to entries.count { it.level == "SUCCESS" },
            "WARNING" to entries.count { it.level == "WARNING" },
            "ERROR" to entries.count { it.level == "ERROR" },
            "SKIPPED" to entries.count { it.level == "SKIPPED" }
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun exportJson(): JSONArray = JSONArray(getEntries().map { it.toJson() })

    fun importJson(arr: JSONArray) {
        val logs = List(arr.length()) { DetailedLogEntry.fromJson(arr.optJSONObject(it) ?: JSONObject()) }
        prefs.edit().putString(KEY_ENTRIES, JSONArray(logs.takeLast(MAX_LOGS).map { it.toJson() }).toString()).apply()
    }

    companion object {
        private const val KEY_ENTRIES = "entries"
        private const val MAX_LOGS = 500
    }
}
