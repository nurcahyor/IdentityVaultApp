package com.identityvault.app.root

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

class MagiskChecker(private val context: Context, private val rootShell: RootShell = RootShell()) {
    private val rootManagers = listOf(
        "com.topjohnwu.magisk",
        "me.weishu.kernelsu",
        "me.bmax.apatch"
    )

    fun check(): String {
        val installed = rootManagers.filter { packageInstalled(it) }
        val adb = rootShell.run("ls -d /data/adb /data/adb/modules 2>/dev/null; su -v 2>/dev/null; getprop init.svc.zygisk 2>/dev/null")
        val localIndicator = File("/sbin/.magisk").exists()
        val details = mutableListOf<String>()
        if (installed.isNotEmpty()) details += "Package: ${installed.joinToString()}"
        if (adb.output.isNotBlank()) details += adb.output
        if (localIndicator) details += "/sbin/.magisk tersedia"
        return if (details.isEmpty()) "Magisk/KSU/APatch tidak terdeteksi" else details.joinToString("\n")
    }

    private fun packageInstalled(name: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(name, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
