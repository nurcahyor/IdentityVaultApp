package com.identityvault.hook

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import de.robv.android.xposed.XposedBridge
import org.json.JSONArray
import org.json.JSONObject

class HookDebugLogger(private val context: Context, private val packageName: String, private val sdkInt: Int) {
    private val emitted = linkedSetOf<String>()

    fun log(level: String, category: String, message: String, detail: String? = null, onceKey: String? = null) {
        val key = onceKey ?: "$level|$category|$message"
        if (!emitted.add(key)) return
        XposedBridge.log("IdentityVault $level | $category | $packageName | SDK $sdkInt | $message")
        runCatching {
            context.sendBroadcast(
                Intent(HookConstants.MARKER_ACTION)
                    .setComponent(ComponentName(HookConstants.MODULE_PACKAGE, HookConstants.MARKER_RECEIVER))
                    .putExtra("log_level", level)
                    .putExtra("log_category", category)
                    .putExtra("log_package", packageName)
                    .putExtra("log_sdk", sdkInt)
                    .putExtra("log_message", message)
                    .putExtra("log_detail", detail.orEmpty())
            )
        }
    }

    fun marker(session: HookSession) {
        runCatching {
            val summary = JSONObject()
                .put("packageName", packageName)
                .put("sdkInt", sdkInt)
                .put("hooksInstalled", JSONArray(session.hooksInstalled.toList()))
                .put("appliedFields", JSONArray(session.appliedFields.toList()))
                .put("skippedHooks", JSONArray(session.skippedHooks.toList()))
                .put("skippedFields", JSONArray(session.skippedFields.toList()))
                .put("errors", JSONArray(session.errors.toList()))
            context.sendBroadcast(
                Intent(HookConstants.MARKER_ACTION)
                    .setComponent(ComponentName(HookConstants.MODULE_PACKAGE, HookConstants.MARKER_RECEIVER))
                    .putExtra("package", packageName)
                    .putStringArrayListExtra("fields", ArrayList(session.appliedFields))
                    .putExtra("sdk", sdkInt)
                    .putExtra("marker_summary", summary.toString())
            )
        }.onFailure {
            XposedBridge.log("IdentityVault marker failed for $packageName: ${it.message}")
        }
    }
}

fun HookSession.installed(category: String, message: String) {
    hooksInstalled += category
    logger.log("SUCCESS", category, message)
}

fun HookSession.applied(category: String, field: String, message: String) {
    appliedFields += field
    logger.log("SUCCESS", category, message, onceKey = "applied|$field")
    logger.marker(this)
}

fun HookSession.skipped(level: String, category: String, message: String, field: String? = null) {
    skippedHooks += message
    if (!field.isNullOrBlank()) skippedFields += field
    logger.log(level, category, message, onceKey = "skip|$category|$message")
    logger.marker(this)
}

fun HookSession.error(category: String, message: String) {
    errors += message
    logger.log("ERROR", category, message, onceKey = "error|$category|$message")
    logger.marker(this)
}
