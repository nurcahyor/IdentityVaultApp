package com.identityvault.app.status

import android.content.Context
import com.identityvault.app.data.LsposedStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ModuleStatusRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("module_status", Context.MODE_PRIVATE)
    private val statusFile: File = File(context.filesDir, "module_status.json")

    fun setHookActive(packageName: String? = null) {
        val packages = getHookedPackages().toMutableSet()
        if (!packageName.isNullOrBlank()) packages += packageName
        prefs.edit()
            .putBoolean("hook_active", true)
            .putStringSet("hooked_packages", packages)
            .putLong("last_hook_at", System.currentTimeMillis())
            .apply()
        writeStatusFile(true, packages.toList())
    }

    fun hookActive(): Boolean = prefs.getBoolean("hook_active", false) || statusFileJson().optBoolean("hook_active", false)

    fun getHookedPackages(): List<String> {
        val prefPackages = prefs.getStringSet("hooked_packages", emptySet()).orEmpty()
        val filePackages = statusFileJson().optJSONArray("hooked_packages") ?: JSONArray()
        val combined = linkedSetOf<String>()
        combined += prefPackages
        for (i in 0 until filePackages.length()) combined += filePackages.optString(i)
        return combined.filter { it.isNotBlank() }.sorted()
    }

    fun clearMarker() {
        prefs.edit().clear().apply()
        if (statusFile.exists()) statusFile.delete()
    }

    fun status(lsposedInstalled: Boolean): LsposedStatus {
        val active = hookActive()
        val packages = getHookedPackages()
        val detail = when {
            active -> "Module aktif. Package pernah ter-hook: ${packages.size}"
            lsposedInstalled -> "Module belum aktif di LSPosed. Buka LSPosed, aktifkan module ini, pilih scope aplikasi target, lalu reboot."
            else -> "LSPosed tidak tersedia atau belum terdeteksi. Aplikasi tetap aman dijalankan."
        }
        return LsposedStatus(lsposedInstalled, active, packages, detail)
    }

    fun exportJson(): JSONObject = JSONObject()
        .put("hook_active", hookActive())
        .put("hooked_packages", JSONArray(getHookedPackages()))

    fun importJson(json: JSONObject) {
        val packages = mutableSetOf<String>()
        val arr = json.optJSONArray("hooked_packages") ?: JSONArray()
        for (i in 0 until arr.length()) packages += arr.optString(i)
        prefs.edit()
            .putBoolean("hook_active", json.optBoolean("hook_active", false))
            .putStringSet("hooked_packages", packages)
            .apply()
        writeStatusFile(json.optBoolean("hook_active", false), packages.toList())
    }

    private fun writeStatusFile(active: Boolean, packages: List<String>) {
        runCatching {
            statusFile.writeText(JSONObject().put("hook_active", active).put("hooked_packages", JSONArray(packages)).toString())
        }
    }

    private fun statusFileJson(): JSONObject {
        return runCatching {
            if (statusFile.exists()) JSONObject(statusFile.readText()) else JSONObject()
        }.getOrDefault(JSONObject())
    }
}
