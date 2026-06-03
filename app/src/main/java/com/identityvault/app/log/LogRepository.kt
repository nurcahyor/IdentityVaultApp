package com.identityvault.app.log

import android.content.Context
import org.json.JSONArray

class LogRepository(context: Context) {
    private val detailed = DetailedLogRepository(context)
    private val prefs = context.getSharedPreferences("logs", Context.MODE_PRIVATE)

    fun add(message: String) {
        detailed.add("INFO", "APP", message)
        val updated = getLogs().toMutableList()
        updated += "${System.currentTimeMillis()} | $message"
        while (updated.size > 200) updated.removeAt(0)
        prefs.edit().putString("entries", JSONArray(updated).toString()).apply()
    }

    fun getLogs(): List<String> {
        val detailedLines = detailed.getDisplayLines()
        if (detailedLines.isNotEmpty()) return detailedLines
        val arr = JSONArray(prefs.getString("entries", "[]"))
        return List(arr.length()) { arr.optString(it) }
    }

    fun clear() {
        prefs.edit().clear().apply()
        detailed.clear()
    }

    fun exportJson(): JSONArray = detailed.exportJson()

    fun importJson(arr: JSONArray) {
        detailed.importJson(arr)
    }

    fun getDetailedRepository(): DetailedLogRepository = detailed
}
