package com.identityvault.app.slots

import org.json.JSONObject

data class IdentityGroup(
    val id: String,
    val name: String,
    val collapsed: Boolean,
    val showApps: Boolean,
    val showSystemApps: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val apps: List<IdentityGroupApp>
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("collapsed", collapsed)
        .put("showApps", showApps)
        .put("showSystemApps", showSystemApps)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("apps", org.json.JSONArray(apps.map { it.toJson() }))

    companion object {
        fun fromJson(json: JSONObject): IdentityGroup {
            val arr = json.optJSONArray("apps") ?: org.json.JSONArray()
            return IdentityGroup(
                id = json.optString("id"),
                name = json.optString("name", "Identity Group"),
                collapsed = json.optBoolean("collapsed", false),
                showApps = json.optBoolean("showApps", true),
                showSystemApps = json.optBoolean("showSystemApps", false),
                createdAt = json.optLong("createdAt"),
                updatedAt = json.optLong("updatedAt"),
                apps = List(arr.length()) { IdentityGroupApp.fromJson(arr.optJSONObject(it) ?: JSONObject()) }
            )
        }
    }
}

data class IdentityGroupApp(
    val packageName: String,
    val appLabel: String,
    val isSystemApp: Boolean,
    val lastExportAt: Long,
    val lastImportAt: Long,
    val status: String
) {
    fun toJson(): JSONObject = JSONObject()
        .put("packageName", packageName)
        .put("appLabel", appLabel)
        .put("isSystemApp", isSystemApp)
        .put("lastExportAt", lastExportAt)
        .put("lastImportAt", lastImportAt)
        .put("status", status)

    companion object {
        const val STATUS_EMPTY = "No identity"
        const val STATUS_EXPORTED = "Exported"
        const val STATUS_ASSIGNED = "Assigned"

        fun fromJson(json: JSONObject): IdentityGroupApp = IdentityGroupApp(
            packageName = json.optString("packageName"),
            appLabel = json.optString("appLabel"),
            isSystemApp = json.optBoolean("isSystemApp"),
            lastExportAt = json.optLong("lastExportAt"),
            lastImportAt = json.optLong("lastImportAt"),
            status = json.optString("status", STATUS_EMPTY)
        )
    }
}

data class IdentitySlot(
    val id: String,
    val name: String,
    val packageName: String,
    val appLabel: String,
    val isSystemApp: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastBackupAt: Long,
    val backupSnapshot: JSONObject?,
    val enabledFieldsSnapshot: JSONObject?,
    val assignedIdentityId: String?,
    val status: String
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("packageName", packageName)
        .put("appLabel", appLabel)
        .put("isSystemApp", isSystemApp)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("lastBackupAt", lastBackupAt)
        .put("backupSnapshot", backupSnapshot)
        .put("enabledFieldsSnapshot", enabledFieldsSnapshot)
        .put("assignedIdentityId", assignedIdentityId)
        .put("status", status)

    companion object {
        const val STATUS_EMPTY = "Empty"
        const val STATUS_BACKUP_AVAILABLE = "Backup Available"
        const val STATUS_ASSIGNED = "Assigned"

        fun fromJson(json: JSONObject): IdentitySlot = IdentitySlot(
            id = json.optString("id"),
            name = json.optString("name"),
            packageName = json.optString("packageName"),
            appLabel = json.optString("appLabel"),
            isSystemApp = json.optBoolean("isSystemApp"),
            createdAt = json.optLong("createdAt"),
            updatedAt = json.optLong("updatedAt"),
            lastBackupAt = json.optLong("lastBackupAt"),
            backupSnapshot = json.optJSONObject("backupSnapshot"),
            enabledFieldsSnapshot = json.optJSONObject("enabledFieldsSnapshot"),
            assignedIdentityId = json.optString("assignedIdentityId").takeIf { it.isNotBlank() },
            status = json.optString("status", STATUS_EMPTY)
        )
    }
}

data class BackupStep(val title: String, val status: String, val detail: String = "")

data class SlotBackupReport(
    val title: String,
    val summary: String,
    val steps: List<BackupStep>,
    val reportText: String,
    val hasWarnings: Boolean
)
