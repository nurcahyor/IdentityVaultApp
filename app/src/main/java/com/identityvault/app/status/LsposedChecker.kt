package com.identityvault.app.status

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.identityvault.app.data.LsposedStatus

class LsposedChecker(private val context: Context, private val repository: ModuleStatusRepository) {
    private val knownManagers = listOf(
        "org.lsposed.manager",
        "io.github.lsposed.manager",
        "org.meowcat.edxposed.manager",
        "de.robv.android.xposed.installer"
    )

    fun check(): LsposedStatus {
        val installed = knownManagers.any { packageInstalled(it) } || findManagerByLabel()
        return repository.status(installed)
    }

    fun openManager(): Boolean {
        val pm = context.packageManager
        knownManagers.forEach { pkg ->
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    context.startActivity(intent)
                    true
                } catch (_: ActivityNotFoundException) {
                    false
                }
            }
        }
        return false
    }

    private fun packageInstalled(name: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(name, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun findManagerByLabel(): Boolean {
        return try {
            val apps = context.packageManager.getInstalledApplications(0)
            apps.any {
                val label = context.packageManager.getApplicationLabel(it).toString().lowercase()
                label.contains("lsposed") || label.contains("xposed")
            }
        } catch (_: Exception) {
            false
        }
    }
}
