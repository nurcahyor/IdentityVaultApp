package com.identityvault.app.detector

import android.content.Context
import android.os.Build
import com.identityvault.app.root.RootShell
import com.identityvault.app.status.ModuleStatusRepository
import java.io.File

class EnvironmentDetector(
    private val context: Context,
    private val rootShell: RootShell = RootShell(),
    private val moduleStatusRepository: ModuleStatusRepository = ModuleStatusRepository(context)
) {
    private val rootManagers = listOf("com.topjohnwu.magisk", "me.weishu.kernelsu", "me.bmax.apatch")
    private val busyBoxPaths = listOf("/system/bin/busybox", "/system/xbin/busybox", "/sbin/busybox", "/vendor/bin/busybox")
    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su"
    )

    fun detect(): DetectorResult {
        val checks = listOf(
            rootManagementApps(),
            rootOrXposedAccess(),
            testKeys(),
            busyboxBinary(),
            suBinary(),
            suOnPath(),
            rwPartitions(),
            magiskEnvironment(),
            lsposedEnvironment(),
            selinuxStatus(),
            systemProperties()
        )
        val red = checks.count { it.status == DetectorStatus.DETECTED }
        val warning = checks.count { it.status == DetectorStatus.WARNING }
        val summary = if (red > 0) "Status: Root / modifications detected ($red red checks)" else "Status: No obvious root indicators"
        return DetectorResult(checks, red, warning, summary)
    }

    fun toReport(result: DetectorResult): String {
        return buildString {
            appendLine("Environment / Root Detector")
            appendLine(result.summary)
            appendLine("Warnings: ${result.warningCount}")
            appendLine()
            result.checks.forEach {
                appendLine("[${it.status}] ${it.title}")
                appendLine(it.detail)
                appendLine()
            }
        }
    }

    private fun rootManagementApps(): DetectorCheck {
        val installed = rootManagers.filter { isInstalled(it) }
        return if (installed.isEmpty()) {
            DetectorCheck("root_managers", "Root management apps", "No known root managers installed", DetectorStatus.CLEAN)
        } else {
            DetectorCheck("root_managers", "Root management apps", installed.joinToString(), DetectorStatus.DETECTED)
        }
    }

    private fun rootOrXposedAccess(): DetectorCheck {
        val packages = moduleStatusRepository.getHookedPackages()
        return if (packages.isEmpty()) {
            DetectorCheck("root_xposed_access", "Apps with root / Xposed access", "No module/package marker stored", DetectorStatus.CLEAN)
        } else {
            DetectorCheck("root_xposed_access", "Apps with root / Xposed access", "Packages: ${packages.joinToString()}", DetectorStatus.DETECTED)
        }
    }

    private fun testKeys(): DetectorCheck {
        val tags = Build.TAGS ?: "unknown"
        val detected = tags.contains("test-keys", ignoreCase = true)
        return DetectorCheck("test_keys", "Test keys / build tags", "Build.TAGS = $tags", if (detected) DetectorStatus.DETECTED else DetectorStatus.CLEAN)
    }

    private fun busyboxBinary(): DetectorCheck {
        val found = busyBoxPaths.filter { File(it).exists() }
        return if (found.isEmpty()) {
            DetectorCheck("busybox", "BusyBox binary", "No busybox binary in common locations", DetectorStatus.CLEAN)
        } else {
            DetectorCheck("busybox", "BusyBox binary", found.joinToString("\n"), DetectorStatus.DETECTED)
        }
    }

    private fun suBinary(): DetectorCheck {
        val found = suPaths.filter { File(it).exists() }
        return if (found.isEmpty()) {
            DetectorCheck("su_binary", "su binary", "No su binary in common locations", DetectorStatus.CLEAN)
        } else {
            DetectorCheck("su_binary", "su binary", found.joinToString("\n"), DetectorStatus.DETECTED)
        }
    }

    private fun suOnPath(): DetectorCheck {
        val result = rootShell.runPlain("which su", 3)
        return if (result.exitCode == 0 && result.output.isNotBlank()) {
            DetectorCheck("su_path", "su exists on PATH", result.output, DetectorStatus.DETECTED)
        } else {
            DetectorCheck("su_path", "su exists on PATH", "which su did not return a path", DetectorStatus.CLEAN)
        }
    }

    private fun rwPartitions(): DetectorCheck {
        val mount = rootShell.runPlain("mount", 3)
        val lines = mount.output.lines().filter { line ->
            val normalized = line.lowercase()
            listOf(" /system ", " /vendor ", " /product ", " /odm ").any { normalized.contains(it) } &&
                (normalized.contains(" rw,") || normalized.contains("(rw,"))
        }
        return if (lines.isEmpty()) {
            DetectorCheck("rw_partitions", "RW system / vendor partitions", "No obvious RW mounts for /system, /vendor, /product, /odm", DetectorStatus.CLEAN)
        } else {
            DetectorCheck("rw_partitions", "RW system / vendor partitions", lines.joinToString("\n"), DetectorStatus.DETECTED)
        }
    }

    private fun magiskEnvironment(): DetectorCheck {
        val result = rootShell.run("ls -d /data/adb /data/adb/modules 2>/dev/null; su -v 2>/dev/null; getprop init.svc.zygisk 2>/dev/null", 5)
        return if (result.output.isBlank()) {
            DetectorCheck("magisk_env", "Magisk environment", "No /data/adb, module, su -v, or Zygisk indicators available", DetectorStatus.CLEAN)
        } else {
            DetectorCheck("magisk_env", "Magisk environment", result.output, DetectorStatus.DETECTED)
        }
    }

    private fun lsposedEnvironment(): DetectorCheck {
        val active = moduleStatusRepository.hookActive()
        val packages = moduleStatusRepository.getHookedPackages()
        val detail = if (active) "hook_active=true\nPackages: ${packages.joinToString()}" else "hook_active marker kosong"
        return DetectorCheck("lsposed_env", "LSPosed environment", detail, if (active) DetectorStatus.DETECTED else DetectorStatus.CLEAN)
    }

    private fun selinuxStatus(): DetectorCheck {
        val result = rootShell.runPlain("getenforce", 3)
        val value = result.output.ifBlank { "unknown" }
        val status = when {
            value.equals("Enforcing", ignoreCase = true) -> DetectorStatus.CLEAN
            value.equals("Permissive", ignoreCase = true) -> DetectorStatus.DETECTED
            else -> DetectorStatus.UNKNOWN
        }
        return DetectorCheck("selinux", "SELinux status", value, status)
    }

    private fun systemProperties(): DetectorCheck {
        val props = listOf("ro.debuggable", "ro.secure", "ro.adb.secure", "ro.build.type", "ro.build.tags")
        val output = rootShell.runPlain("getprop", 4).output
        val values = props.associateWith { key ->
            Regex("\\[$key\\]: \\[(.*?)]").find(output)?.groupValues?.get(1) ?: "unknown"
        }
        val detected = values["ro.debuggable"] == "1" || values["ro.secure"] == "0" ||
            values["ro.build.type"] != "user" || values["ro.build.tags"]?.contains("test-keys") == true
        val detail = values.entries.joinToString("\n") { "${it.key}=${it.value}" }
        return DetectorCheck("system_props", "Build/system properties", detail, if (detected) DetectorStatus.DETECTED else DetectorStatus.CLEAN)
    }

    private fun isInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
