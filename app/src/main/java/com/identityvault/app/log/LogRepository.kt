package com.identityvault.app.log

import android.content.Context
import org.json.JSONArray

class LogRepository(context: Context) {
    private val prefs = context.getSharedPreferences("logs", Context.MODE_PRIVATE)

    fun add(message: String) {
        val updated = getLogs().toMutableList()
        updated += "${System.currentTimeMillis()} | $message"
        while (updated.size > 200) updated.removeAt(0)
        prefs.edit().putString("entries", JSONArray(updated).toString()).apply()
    }

    fun getLogs(): List<String> {
        val arr = JSONArray(prefs.getString("entries", "[]"))
        return List(arr.length()) { arr.optString(it) }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun exportJson(): JSONArray = JSONArray(getLogs())

    fun importJson(arr: JSONArray) {
        val logs = List(arr.length()) { arr.optString(it) }
        prefs.edit().putString("entries", JSONArray(logs).toString()).apply()
    }
}
