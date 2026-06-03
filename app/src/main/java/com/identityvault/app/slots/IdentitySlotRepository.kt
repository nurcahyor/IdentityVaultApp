package com.identityvault.app.slots

import android.content.Context
import com.identityvault.app.data.IdentityFieldState
import com.identityvault.app.data.IdentityProfile
import com.identityvault.app.identity.IdentityRepository
import com.identityvault.app.log.DetailedLogRepository
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class IdentitySlotRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("identity_slots", Context.MODE_PRIVATE)
    private val identityRepository = IdentityRepository(appContext)
    private val logger = DetailedLogRepository(appContext)

    fun getGroups(): List<IdentityGroup> {
        val raw = prefs.getString(KEY_GROUPS, null)
        val groups = if (raw.isNullOrBlank()) emptyList() else JSONArray(raw).let { arr ->
            List(arr.length()) { IdentityGroup.fromJson(arr.optJSONObject(it) ?: JSONObject()) }
        }
        return groups.ifEmpty {
            val now = System.currentTimeMillis()
            listOf(
                IdentityGroup(
                    id = UUID.randomUUID().toString(),
                    name = "Identity Group",
                    collapsed = false,
                    showApps = true,
                    showSystemApps = false,
                    createdAt = now,
                    updatedAt = now,
                    apps = emptyList()
                )
            ).also { saveGroups(it) }
        }
    }

    fun saveGroups(groups: List<IdentityGroup>) {
        prefs.edit().putString(KEY_GROUPS, JSONArray(groups.map { it.toJson() }).toString()).apply()
    }

    fun addGroup(name: String = "New Group"): IdentityGroup {
        val now = System.currentTimeMillis()
        val group = IdentityGroup(
            id = UUID.randomUUID().toString(),
            name = name,
            collapsed = false,
            showApps = true,
            showSystemApps = false,
            createdAt = now,
            updatedAt = now,
            apps = emptyList()
        )
        saveGroups(getGroups() + group)
        return group
    }

    fun updateGroup(group: IdentityGroup) {
        saveGroups(getGroups().map { if (it.id == group.id) group.copy(updatedAt = System.currentTimeMillis()) else it })
    }

    fun deleteGroup(groupId: String) {
        val next = getGroups().filterNot { it.id == groupId }
        saveGroups(next.ifEmpty { listOf(emptyGroup("Identity Group")) })
    }

    fun clearGroup(groupId: String) {
        updateGroup(getGroup(groupId)?.copy(apps = emptyList()) ?: return)
    }

    fun addAppsToGroup(groupId: String, apps: List<IdentityGroupApp>) {
        val group = getGroup(groupId) ?: return
        val existing = group.apps.associateBy { it.packageName }.toMutableMap()
        apps.forEach { app ->
            if (app.packageName.isNotBlank()) existing.putIfAbsent(app.packageName, app)
        }
        updateGroup(group.copy(
            showSystemApps = group.showSystemApps || apps.any { it.isSystemApp },
            apps = existing.values.sortedBy { it.appLabel.lowercase() }
        ))
    }

    fun setGroupApps(groupId: String, apps: List<IdentityGroupApp>) {
        val group = getGroup(groupId) ?: return
        updateGroup(group.copy(
            showSystemApps = group.showSystemApps || apps.any { it.isSystemApp },
            apps = apps.distinctBy { it.packageName }.sortedBy { it.appLabel.lowercase() }
        ))
    }

    fun removeApp(groupId: String, packageName: String) {
        val group = getGroup(groupId) ?: return
        updateGroup(group.copy(apps = group.apps.filterNot { it.packageName == packageName }))
    }

    fun activeGroupId(): String = prefs.getString(KEY_ACTIVE_GROUP_ID, "").orEmpty()

    fun isGroupActive(groupId: String): Boolean = activeGroupId() == groupId

    fun deactivateGroup(groupId: String) {
        if (isGroupActive(groupId)) {
            prefs.edit().remove(KEY_ACTIVE_GROUP_ID).apply()
            logger.add("INFO", "IDENTITY_SLOT", "Identity slot group deactivated", detail = "groupId=$groupId")
        }
    }

    fun activateGroup(groupId: String): SlotBackupReport {
        val group = getGroup(groupId) ?: return failedReport("Activation Failed", "Group not found")
        val activeProfile = identityRepository.getProfile()
        val assigned = assignedProfiles()
        group.apps.forEach { app ->
            if (app.packageName.isNotBlank() && assigned.optJSONObject(app.packageName) == null) {
                assigned.put(app.packageName, activeProfile.toJson())
            }
        }
        prefs.edit()
            .putString(KEY_ASSIGNED_PROFILES, assigned.toString())
            .putString(KEY_ACTIVE_GROUP_ID, groupId)
            .apply()
        logger.add("SUCCESS", "IDENTITY_SLOT", "Identity slot group activated", detail = "group=${group.name}, apps=${group.apps.size}")
        val steps = listOf(
            BackupStep("Active group", "OK", group.name),
            BackupStep("Assigned apps", "OK", group.apps.joinToString { it.packageName }),
            BackupStep("Build Prop", verifyBuild(activeProfile)),
            BackupStep("SystemProperties", verifySystemProperties(activeProfile))
        )
        return SlotBackupReport(
            title = "Identity Slot Active",
            summary = "Identity slot active for ${group.name}.",
            steps = steps,
            reportText = steps.joinToString("\n") { "${it.title}: ${it.status} ${it.detail}" },
            hasWarnings = steps.any { it.status != "OK" }
        )
    }

    fun assignCurrentProfileToApp(packageName: String): SlotBackupReport {
        val profile = identityRepository.getProfile()
        saveAssignedProfile(packageName, profile)
        return SlotBackupReport(
            title = "Identity Assigned",
            summary = "Active IdentityVault profile assigned to $packageName.",
            steps = listOf(
                BackupStep("Reading active profile", "OK"),
                BackupStep("Assigning package identity", "OK"),
                BackupStep("Verifying Build Prop", verifyBuild(profile)),
                BackupStep("Verifying SystemProperties", verifySystemProperties(profile))
            ),
            reportText = report(profile, listOf(BackupStep("Assigning package identity", "OK"))),
            hasWarnings = false
        )
    }

    fun exportGroupJson(groupId: String): JSONObject {
        val group = getGroup(groupId) ?: error("Group not found")
        val now = System.currentTimeMillis()
        val apps = JSONArray()
        group.apps.forEach { app ->
            val profile = assignedProfileForPackage(app.packageName) ?: identityRepository.getProfile()
            apps.put(exportAppObject(group, app, profile, now))
        }
        markGroupExported(groupId, now)
        logger.add("SUCCESS", "IDENTITY_SLOT", "Exported identity group=${group.name}", detail = "apps=${group.apps.size}")
        return JSONObject()
            .put("manifest", manifest("identityvault-group-export", now))
            .put("group", JSONObject().put("id", group.id).put("name", group.name))
            .put("apps", apps)
    }

    fun exportAppJson(groupId: String, packageName: String): JSONObject {
        val group = getGroup(groupId) ?: error("Group not found")
        val app = group.apps.firstOrNull { it.packageName == packageName } ?: error("App not found")
        val now = System.currentTimeMillis()
        val profile = assignedProfileForPackage(packageName) ?: identityRepository.getProfile()
        markApp(groupId, packageName, lastExportAt = now, status = IdentityGroupApp.STATUS_EXPORTED)
        logger.add("SUCCESS", "IDENTITY_SLOT", "Exported identity app=${app.appLabel}", packageName = packageName)
        return JSONObject()
            .put("manifest", manifest("identityvault-app-export", now))
            .put("apps", JSONArray().put(exportAppObject(group, app, profile, now)))
    }

    fun exportReport(json: JSONObject): SlotBackupReport {
        val apps = json.optJSONArray("apps") ?: JSONArray()
        val steps = mutableListOf<BackupStep>()
        for (index in 0 until apps.length()) {
            val item = apps.optJSONObject(index) ?: continue
            val label = item.optString("appLabel").ifBlank { item.optString("packageName") }
            val identity = item.optJSONObject("identity")
            if (identity == null) {
                steps += BackupStep(label, "FAILED", "identity missing")
                continue
            }
            val profile = IdentityProfile.fromJson(identity)
            steps += BackupStep(label, "OK", item.optString("packageName"))
            steps += identitySteps(profile, "  ")
        }
        return SlotBackupReport(
            title = "Backup Completed",
            summary = "IdentityVault export verified. These IdentityVault fields are included in the backup.",
            steps = steps,
            reportText = buildString {
                appendLine("IdentityVault Backup Verification")
                steps.forEach { appendLine("${it.title}: ${it.status}${if (it.detail.isBlank()) "" else " - ${it.detail}"}") }
            },
            hasWarnings = steps.any { it.status != "OK" && it.status != "SKIPPED disabled" && it.status != "SKIPPED empty" }
        )
    }

    fun previewImport(raw: String): ImportPreview {
        return try {
            val json = JSONObject(raw)
            val manifest = json.optJSONObject("manifest") ?: return ImportPreview(false, "IdentityVault manifest not found", emptyList(), json)
            if (manifest.optString("appId") != APP_ID) return ImportPreview(false, "File is not an IdentityVault export", emptyList(), json)
            if (manifest.optInt("version") !in 1..1) return ImportPreview(false, "Unsupported IdentityVault export version", emptyList(), json)
            val type = manifest.optString("type")
            if (type !in setOf("identityvault-group-export", "identityvault-app-export")) {
                return ImportPreview(false, "Unsupported IdentityVault export type: $type", emptyList(), json)
            }
            val apps = json.optJSONArray("apps") ?: return ImportPreview(false, "Export contains no app identities", emptyList(), json)
            val items = List(apps.length()) { index ->
                val item = apps.optJSONObject(index) ?: JSONObject()
                ImportAppPreview(
                    packageName = item.optString("packageName"),
                    appLabel = item.optString("appLabel"),
                    hasIdentity = item.optJSONObject("identity") != null
                )
            }.filter { it.packageName.isNotBlank() }
            ImportPreview(items.isNotEmpty(), if (items.isEmpty()) "No package identity found" else "Ready to restore ${items.size} app identity item(s)", items, json)
        } catch (e: Exception) {
            ImportPreview(false, "Invalid IdentityVault export: ${e.message}", emptyList(), null)
        }
    }

    fun importGroup(groupId: String, raw: String): SlotBackupReport {
        val preview = previewImport(raw)
        if (!preview.valid || preview.json == null) return failedReport("Restore Failed", preview.message)
        val group = getGroup(groupId) ?: return failedReport("Restore Failed", "Group not found")
        val groupPackages = group.apps.map { it.packageName }.toSet()
        val arr = preview.json.optJSONArray("apps") ?: JSONArray()
        val steps = mutableListOf<BackupStep>()
        val now = System.currentTimeMillis()
        for (index in 0 until arr.length()) {
            val item = arr.optJSONObject(index) ?: continue
            val packageName = item.optString("packageName")
            if (packageName !in groupPackages) {
                steps += BackupStep(packageName, "SKIPPED", "package not in group")
                continue
            }
            val identity = item.optJSONObject("identity")
            if (identity == null) {
                steps += BackupStep(packageName, "FAILED", "identity missing")
                continue
            }
            val profile = IdentityProfile.fromJson(identity)
            saveAssignedProfile(packageName, profile)
            markApp(groupId, packageName, lastImportAt = now, status = IdentityGroupApp.STATUS_ASSIGNED)
            steps += BackupStep(packageName, "OK", "identity restored")
            steps += identitySteps(profile, "  ")
            logger.add("SUCCESS", "IDENTITY_SLOT", "Restored identity for package=$packageName from group=${group.name}", packageName = packageName)
        }
        return importReport("Restore Group Completed", "Identity restore finished for ${group.name}. Force stop targets, then reopen.", steps)
    }

    fun importApp(groupId: String, packageName: String, raw: String): SlotBackupReport {
        val preview = previewImport(raw)
        if (!preview.valid || preview.json == null) return failedReport("Restore Failed", preview.message)
        val arr = preview.json.optJSONArray("apps") ?: JSONArray()
        val match = (0 until arr.length())
            .mapNotNull { arr.optJSONObject(it) }
            .firstOrNull { it.optString("packageName") == packageName }
            ?: return failedReport("Restore Failed", "Package mismatch. Export does not contain $packageName.")
        val identity = match.optJSONObject("identity")
            ?: return failedReport("Restore Failed", "Identity data missing for $packageName.")
        val profile = IdentityProfile.fromJson(identity)
        saveAssignedProfile(packageName, profile)
        markApp(groupId, packageName, lastImportAt = System.currentTimeMillis(), status = IdentityGroupApp.STATUS_ASSIGNED)
        logger.add("SUCCESS", "IDENTITY_SLOT", "Restored identity only for package=$packageName", packageName = packageName)
        return importReport(
            "Restore App Completed",
            "Identity restored only for $packageName. Force stop target, then reopen.",
            listOf(
                BackupStep("Validating IdentityVault export", "OK"),
                BackupStep("Matching package $packageName", "OK"),
                BackupStep("Restoring full IdentityProfile", "OK"),
                BackupStep("Verifying Build Prop", verifyBuild(profile)),
                BackupStep("Verifying SystemProperties", verifySystemProperties(profile))
            ) + identitySteps(profile, "")
        )
    }

    fun backupSlot(slotId: String): SlotBackupReport {
        val slots = getSlots()
        val index = slots.indexOfFirst { it.id == slotId }
        if (index < 0) return failedReport("Backup Failed", "Slot not found")
        val slot = slots[index]
        logger.add("INFO", "IDENTITY_SLOT", "Backup started for slot=${slot.name} package=${slot.packageName}")
        val profile = identityRepository.getProfile()
        val steps = mutableListOf<BackupStep>()
        fun step(title: String, status: String, detail: String = "") {
            logger.add("INFO", "IDENTITY_SLOT", "Backup progress: $title", packageName = slot.packageName)
            steps += BackupStep(title, status, detail)
        }
        step("Reading active profile", "OK")
        val enabled = enabledFields(profile)
        step("Collecting enabled fields", "OK")
        step("Capturing Build Prop", verifyBuild(profile))
        step("Capturing SystemProperties", verifySystemProperties(profile))
        step("Capturing Android ID", fieldStatus(profile.androidId))
        step("Capturing Telephony", verifyTelephony(profile))
        step("Capturing WiFi", verifyWifi(profile))
        step("Capturing Bluetooth", verifyBluetooth(profile))
        step("Capturing GSF ID", fieldStatus(profile.gsfId))
        step("Capturing Advertising ID", fieldStatus(profile.advertisingId))
        step("Capturing MediaDrm", fieldStatus(profile.mediaDrmId))
        step("Capturing Google Account", fieldStatus(profile.googleAccountEmail))
        val snapshot = profile.toJson()
        val now = System.currentTimeMillis()
        val updated = slot.copy(updatedAt = now, lastBackupAt = now, backupSnapshot = snapshot, enabledFieldsSnapshot = enabled, status = IdentitySlot.STATUS_BACKUP_AVAILABLE)
        saveSlots(slots.toMutableList().also { it[index] = updated })
        step("Saving slot backup", "OK")
        val verifyStatus = if (getSlots().firstOrNull { it.id == slotId }?.backupSnapshot?.toString() == snapshot.toString()) "OK" else "WARNING"
        step("Verifying saved backup", verifyStatus)
        return backupReport(profile, steps)
    }

    fun restoreSlot(slotId: String): SlotBackupReport {
        val slot = getSlots().firstOrNull { it.id == slotId }
            ?: return failedReport("Restore Failed", "Slot not found")
        val snapshot = slot.backupSnapshot ?: return failedReport("Restore Failed", "No backup available for this slot.")
        val profile = IdentityProfile.fromJson(snapshot)
        identityRepository.saveProfile(profile)
        if (slot.packageName.isNotBlank()) saveAssignedProfile(slot.packageName, profile)
        return importReport(
            "Restore Completed",
            "Identity restored from this slot only. Force stop target app, then reopen.",
            listOf(BackupStep("Reading slot backup", "OK"), BackupStep("Restoring full IdentityProfile", "OK"), BackupStep("Assigning identity to package", if (slot.packageName.isBlank()) "SKIPPED empty" else "OK"))
        )
    }

    fun assignedProfileForPackage(packageName: String): IdentityProfile? {
        val activeGroup = getGroup(activeGroupId()) ?: return null
        if (activeGroup.apps.none { it.packageName == packageName }) return null
        val assigned = assignedProfiles().optJSONObject(packageName)
        if (assigned != null) return IdentityProfile.fromJson(assigned)
        val slot = getSlots().firstOrNull {
            it.packageName == packageName && it.status == IdentitySlot.STATUS_ASSIGNED && it.backupSnapshot != null
        } ?: return null
        return IdentityProfile.fromJson(slot.backupSnapshot ?: return null)
    }

    private fun getGroup(groupId: String): IdentityGroup? = getGroups().firstOrNull { it.id == groupId }

    private fun emptyGroup(name: String): IdentityGroup {
        val now = System.currentTimeMillis()
        return IdentityGroup(UUID.randomUUID().toString(), name, false, true, false, now, now, emptyList())
    }

    private fun manifest(type: String, now: Long): JSONObject = JSONObject()
        .put("appId", APP_ID)
        .put("type", type)
        .put("version", 1)
        .put("createdAt", now)

    private fun exportAppObject(group: IdentityGroup, app: IdentityGroupApp, profile: IdentityProfile, now: Long): JSONObject = JSONObject()
        .put("groupId", group.id)
        .put("groupName", group.name)
        .put("packageName", app.packageName)
        .put("appLabel", app.appLabel)
        .put("isSystemApp", app.isSystemApp)
        .put("exportedAt", now)
        .put("enabledFieldsSnapshot", enabledFields(profile))
        .put("identity", profile.toJson())

    private fun markGroupExported(groupId: String, now: Long) {
        val group = getGroup(groupId) ?: return
        updateGroup(group.copy(apps = group.apps.map { it.copy(lastExportAt = now, status = IdentityGroupApp.STATUS_EXPORTED) }))
    }

    private fun markApp(groupId: String, packageName: String, lastExportAt: Long? = null, lastImportAt: Long? = null, status: String? = null) {
        val group = getGroup(groupId) ?: return
        updateGroup(group.copy(apps = group.apps.map {
            if (it.packageName == packageName) it.copy(
                lastExportAt = lastExportAt ?: it.lastExportAt,
                lastImportAt = lastImportAt ?: it.lastImportAt,
                status = status ?: it.status
            ) else it
        }))
    }

    private fun saveAssignedProfile(packageName: String, profile: IdentityProfile) {
        if (packageName.isBlank()) return
        val json = assignedProfiles().put(packageName, profile.toJson())
        prefs.edit().putString(KEY_ASSIGNED_PROFILES, json.toString()).apply()
    }

    private fun assignedProfiles(): JSONObject = JSONObject(prefs.getString(KEY_ASSIGNED_PROFILES, "{}") ?: "{}")

    private fun getSlots(): List<IdentitySlot> {
        val arr = JSONArray(prefs.getString(KEY_SLOTS, "[]") ?: "[]")
        return List(arr.length()) { IdentitySlot.fromJson(arr.optJSONObject(it) ?: JSONObject()) }
    }

    private fun saveSlots(slots: List<IdentitySlot>) {
        prefs.edit().putString(KEY_SLOTS, JSONArray(slots.map { it.toJson() }).toString()).apply()
    }

    private fun enabledFields(profile: IdentityProfile): JSONObject = JSONObject()
        .put("imei1", profile.imei.enabled)
        .put("imei2", profile.imei2.enabled)
        .put("androidId", profile.androidId.enabled)
        .put("advertisingId", profile.advertisingId.enabled)
        .put("gsfId", profile.gsfId.enabled)
        .put("mediaDrmId", profile.mediaDrmId.enabled)
        .put("wifi", profile.macAddress.enabled || profile.macBssid.enabled || profile.macSsid.enabled)
        .put("bluetooth", profile.bluetoothMac.enabled || profile.bluetoothName.enabled)
        .put("buildProp", profile.buildPropEnabled)

    private fun fieldStatus(field: IdentityFieldState): String = when {
        !field.enabled -> "SKIPPED disabled"
        field.value.isBlank() -> "SKIPPED empty"
        else -> "OK"
    }

    private fun verifyBuild(profile: IdentityProfile): String = with(profile.buildProp) {
        if (!profile.buildPropEnabled) "SKIPPED disabled"
        else if (fingerprint.isBlank() || model.isBlank() || displayId.isBlank() || versionRelease.isBlank() || versionSdk.isBlank()) "WARNING"
        else "OK"
    }

    private fun verifySystemProperties(profile: IdentityProfile): String = with(profile.buildProp) {
        if (fingerprint.isBlank() || brand.isBlank() || model.isBlank() || device.isBlank() || name.isBlank()) "WARNING" else "OK"
    }

    private fun verifyTelephony(profile: IdentityProfile): String {
        val required = listOf(profile.imei, profile.subscriberId, profile.simSerialId, profile.mobileNo, profile.simOperator)
        return if (required.any { it.enabled && it.value.isBlank() }) "WARNING" else "OK"
    }

    private fun verifyWifi(profile: IdentityProfile): String =
        if (listOf(profile.macAddress, profile.macBssid, profile.macSsid).any { it.enabled && it.value.isBlank() }) "WARNING" else "OK"

    private fun verifyBluetooth(profile: IdentityProfile): String =
        if (listOf(profile.bluetoothMac, profile.bluetoothName).any { it.enabled && it.value.isBlank() }) "WARNING" else "OK"

    private fun identitySteps(profile: IdentityProfile, prefix: String): List<BackupStep> = listOf(
        BackupStep("${prefix}Build Prop", verifyBuild(profile), "model=${profile.buildProp.model}, fingerprint=${profile.buildProp.fingerprint}"),
        BackupStep("${prefix}SystemProperties", verifySystemProperties(profile), "ro.product.model=${profile.buildProp.model}, ro.build.fingerprint=${profile.buildProp.fingerprint}"),
        BackupStep("${prefix}IMEI 1", fieldStatus(profile.imei), profile.imei.value),
        BackupStep("${prefix}IMEI 2", fieldStatus(profile.imei2), profile.imei2.value),
        BackupStep("${prefix}Android ID", fieldStatus(profile.androidId), profile.androidId.value),
        BackupStep("${prefix}GSF ID", fieldStatus(profile.gsfId), profile.gsfId.value),
        BackupStep("${prefix}Advertising ID", fieldStatus(profile.advertisingId), profile.advertisingId.value),
        BackupStep("${prefix}Limit Ad Tracking", fieldStatus(profile.limitAdTrackingEnabled), profile.limitAdTrackingEnabled.value),
        BackupStep("${prefix}MediaDrm ID", fieldStatus(profile.mediaDrmId), profile.mediaDrmId.value),
        BackupStep("${prefix}Serial", fieldStatus(profile.serial), profile.serial.value),
        BackupStep("${prefix}Device Name", fieldStatus(profile.deviceName), profile.deviceName.value),
        BackupStep("${prefix}Bluetooth Name", fieldStatus(profile.bluetoothName), profile.bluetoothName.value),
        BackupStep("${prefix}WiFi", verifyWifi(profile), "${profile.macAddress.value} / ${profile.macBssid.value} / ${profile.macSsid.value}"),
        BackupStep("${prefix}Bluetooth", verifyBluetooth(profile), "${profile.bluetoothMac.value} / ${profile.bluetoothName.value}"),
        BackupStep("${prefix}SIM / Telephony", verifyTelephony(profile), "IMSI=${profile.subscriberId.value}, SIM=${profile.simSerialId.value}, operator=${profile.simOperator.value}"),
        BackupStep("${prefix}Google Account", fieldStatus(profile.googleAccountEmail), profile.googleAccountEmail.value),
        BackupStep("${prefix}Enabled Fields", "OK", enabledFields(profile).toString())
    )

    private fun backupReport(profile: IdentityProfile, steps: List<BackupStep>): SlotBackupReport {
        val warnings = steps.any { it.status != "OK" }
        return SlotBackupReport(
            title = if (warnings) "Backup Completed With Warning" else "Backup Completed",
            summary = if (warnings) "Backup finished, but some fields were skipped or empty." else "All identity fields backed up and verified successfully.",
            steps = steps,
            reportText = report(profile, steps),
            hasWarnings = warnings
        )
    }

    private fun importReport(title: String, summary: String, steps: List<BackupStep>): SlotBackupReport = SlotBackupReport(
        title = title,
        summary = summary,
        steps = steps,
        reportText = buildString {
            appendLine(title)
            appendLine(summary)
            appendLine()
            steps.forEach { appendLine("${it.title}: ${it.status}${if (it.detail.isBlank()) "" else " - ${it.detail}"}") }
        },
        hasWarnings = steps.any { it.status != "OK" }
    )

    private fun report(profile: IdentityProfile, steps: List<BackupStep>): String = buildString {
        appendLine("IdentityVault Export Verification")
        steps.forEach { appendLine("${it.title}: ${it.status}${if (it.detail.isBlank()) "" else " - ${it.detail}"}") }
        appendLine()
        appendLine("Build Prop:")
        appendLine("Build.MODEL=${profile.buildProp.model}")
        appendLine("Build.FINGERPRINT=${profile.buildProp.fingerprint}")
        appendLine("Android version=${profile.buildProp.versionRelease}")
        appendLine("Build number=${profile.buildProp.displayId}")
        appendLine("SDK=${profile.buildProp.versionSdk}")
        appendLine("Security patch=${profile.buildProp.securityPatch}")
        appendLine()
        appendLine("SystemProperties:")
        appendLine("ro.product.model=${profile.buildProp.model}")
        appendLine("ro.build.fingerprint=${profile.buildProp.fingerprint}")
        appendLine("ro.build.version.release=${profile.buildProp.versionRelease}")
        appendLine("ro.build.display.id=${profile.buildProp.displayId}")
        appendLine()
        appendLine("Telephony:")
        appendLine("IMEI1=${profile.imei.value}")
        appendLine("IMEI2=${profile.imei2.value}")
        appendLine("MEID=${profile.meid.value}")
        appendLine("IMSI=${profile.subscriberId.value}")
        appendLine("SIM Serial=${profile.simSerialId.value}")
        appendLine("Mobile No=${profile.mobileNo.value}")
        appendLine("SIM Operator=${profile.simOperator.value}")
        appendLine("SIM Operator Name=${profile.simOperatorName.value}")
        appendLine()
        appendLine("Other:")
        appendLine("Android ID=${profile.androidId.value}")
        appendLine("GSF ID=${profile.gsfId.value}")
        appendLine("Advertising ID=${profile.advertisingId.value}")
        appendLine("Limit Ad Tracking=${profile.limitAdTrackingEnabled.value}")
        appendLine("MediaDrm ID=${profile.mediaDrmId.value}")
        appendLine("WiFi=${profile.macAddress.value} / ${profile.macBssid.value} / ${profile.macSsid.value}")
        appendLine("Bluetooth=${profile.bluetoothMac.value} / ${profile.bluetoothName.value}")
        appendLine("Google Account=${profile.googleAccountEmail.value}")
    }

    private fun failedReport(title: String, message: String): SlotBackupReport = SlotBackupReport(
        title = title,
        summary = message,
        steps = listOf(BackupStep(message, "FAILED")),
        reportText = message,
        hasWarnings = true
    )

    data class ImportPreview(
        val valid: Boolean,
        val message: String,
        val apps: List<ImportAppPreview>,
        val json: JSONObject?
    )

    data class ImportAppPreview(
        val packageName: String,
        val appLabel: String,
        val hasIdentity: Boolean
    )

    companion object {
        private const val APP_ID = "com.identityvault.app"
        private const val KEY_GROUPS = "groups"
        private const val KEY_SLOTS = "slots"
        private const val KEY_ASSIGNED_PROFILES = "assigned_profiles"
        private const val KEY_ACTIVE_GROUP_ID = "active_group_id"
    }
}
