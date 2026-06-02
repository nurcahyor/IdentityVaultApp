package com.identityvault.app.log

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object HookLogger {
    private const val MODULE_PACKAGE = "com.identityvault.app"

    fun markHookActive(packageName: String) {
        val paths = listOf(
            "/data/user_de/0/$MODULE_PACKAGE/files/module_status.json",
            "/data/user/0/$MODULE_PACKAGE/files/module_status.json",
            "/data/data/$MODULE_PACKAGE/files/module_status.json"
        )
        for (path in paths) {
            runCatching {
                val file = File(path)
                file.parentFile?.mkdirs()
                val packages = linkedSetOf<String>()
                if (file.exists()) {
                    val arr = JSONObject(file.readText()).optJSONArray("hooked_packages") ?: JSONArray()
                    for (i in 0 until arr.length()) packages += arr.optString(i)
                }
                packages += packageName
                file.writeText(
                    JSONObject()
                        .put("hook_active", true)
                        .put("last_hooked_package", packageName)
                        .put("last_hook_at", System.currentTimeMillis())
                        .put("hooked_packages", JSONArray(packages.toList()))
                        .toString()
                )
            }
        }
    }
}
