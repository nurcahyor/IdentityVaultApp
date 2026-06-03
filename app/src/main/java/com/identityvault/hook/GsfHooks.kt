package com.identityvault.hook

import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import de.robv.android.xposed.XC_MethodHook
import java.util.concurrent.ConcurrentHashMap

object GsfHooks {
    private const val CATEGORY = "GSF"
    private data class Stats(var queryDetected: Int = 0, var androidIdApplied: Int = 0, var nonAndroidIdQuery: Int = 0)
    private val statsByPackage = ConcurrentHashMap<String, Stats>()

    fun install(session: HookSession, profile: HookProfile) {
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param, session, profile)
        }
        session.logger.log("INFO", CATEGORY, "GSF hook installed; waiting for target app to query android_id.")
        HookCompat.safeHookMethod(
            session,
            CATEGORY,
            ContentResolver::class.java,
            "query",
            Uri::class.java,
            Array<String>::class.java,
            String::class.java,
            Array<String>::class.java,
            String::class.java,
            hook = hook
        )
        HookCompat.safeHookMethod(
            session,
            CATEGORY,
            ContentResolver::class.java,
            "query",
            Uri::class.java,
            Array<String>::class.java,
            String::class.java,
            Array<String>::class.java,
            String::class.java,
            CancellationSignal::class.java,
            hook = hook
        )
        HookCompat.safeHookMethod(
            session,
            CATEGORY,
            ContentResolver::class.java,
            "query",
            Uri::class.java,
            Array<String>::class.java,
            Bundle::class.java,
            CancellationSignal::class.java,
            hook = hook
        )
    }

    private fun apply(param: XC_MethodHook.MethodHookParam, session: HookSession, profile: HookProfile) {
        val uri = param.args.firstOrNull() as? Uri ?: return
        if (uri.authority != "com.google.android.gsf.gservices") return
        val stats = statsByPackage.getOrPut(session.packageName) { Stats() }
        stats.queryDetected++
        val queryInfo = queryInfo(param.args)
        session.logger.log("INFO", CATEGORY, "Detected GSF query $queryInfo", onceKey = "gsf-query|${session.packageName}|${stats.queryDetected}")
        val key = gsfKey(param.args)
        if (key == null || !key.equals("android_id", ignoreCase = true)) {
            stats.nonAndroidIdQuery++
            session.skipped("SKIPPED", CATEGORY, "GSF query detected but key is not android_id: ${key ?: "unknown"}", "Google Services Framework ID")
            return
        }
        val value = validField(
            session,
            CATEGORY,
            "Google Services Framework ID",
            profile.googleServicesFrameworkId,
            HookValidation::hex16
        ) ?: return
        param.result = MatrixCursor(arrayOf("name", "value")).apply {
            addRow(arrayOf("android_id", value))
        }
        stats.androidIdApplied++
        session.applied(CATEGORY, "Google Services Framework ID", "Applied GSF ID: $value")
    }

    private fun gsfKey(args: Array<Any?>): String? {
        (args.firstOrNull() as? Uri)?.lastPathSegment
            ?.takeIf { it.equals("android_id", true) || it.contains("gsf", true) }
            ?.let { return it }
        args.filterIsInstance<Array<String>>()
            .flatMap { it.toList() }
            .firstOrNull { it.equals("android_id", ignoreCase = true) || it.contains("gsf", ignoreCase = true) }
            ?.let { return it }
        args.filterIsInstance<Bundle>().forEach { bundle ->
            bundle.keySet().forEach { key ->
                val value = bundle.get(key)?.toString()
                if (value != null && (value.equals("android_id", true) || value.contains("gsf", true))) return value
                if (key.equals("android_id", true) || key.contains("gsf", true)) return key
            }
        }
        return null
    }

    private fun queryInfo(args: Array<Any?>): String {
        val uri = args.getOrNull(0) as? Uri
        val selection = args.getOrNull(2)
        val selectionArgs = args.filterIsInstance<Array<String>>().joinToString { it.joinToString(prefix = "[", postfix = "]") }
        val bundle = args.filterIsInstance<Bundle>().firstOrNull()?.keySet()?.joinToString(prefix = "{", postfix = "}") { key ->
            "$key=${args.filterIsInstance<Bundle>().first().get(key)}"
        }.orEmpty()
        return "uri=$uri selection=$selection args=$selectionArgs bundle=$bundle"
    }

    fun summary(session: HookSession): String {
        val stats = statsByPackage[session.packageName] ?: return "GSF: installed, waiting for target query"
        return when {
            stats.androidIdApplied > 0 -> "GSF: applied ${stats.androidIdApplied} time"
            stats.nonAndroidIdQuery > 0 -> "GSF: query detected but android_id not requested"
            stats.queryDetected > 0 -> "GSF: query detected, waiting for android_id"
            else -> "GSF: installed, waiting for target query"
        }
    }
}
