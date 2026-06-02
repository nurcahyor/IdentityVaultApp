package com.identityvault.app.root

import android.content.Context
import com.identityvault.app.data.RootStatus
import java.io.File

class RootChecker(private val context: Context, private val rootShell: RootShell = RootShell()) {
    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su"
    )

    fun check(): RootStatus {
        val pathSu = suPaths.firstOrNull { File(it).exists() }
        val which = rootShell.runPlain("which su")
        val hasIndicator = pathSu != null || which.exitCode == 0
        val granted = rootShell.hasRoot()
        val detail = when {
            granted -> "Root granted (${rootShell.run("id").output})"
            pathSu != null -> "su ditemukan di $pathSu, izin root belum granted"
            which.exitCode == 0 -> "su pada PATH: ${which.output}"
            else -> "Tidak ada indikator root jelas"
        }
        return RootStatus(available = hasIndicator || granted, granted = granted, detail = detail)
    }

    fun requestRoot(): RootStatus {
        rootShell.run("id", 10)
        return check()
    }
}
