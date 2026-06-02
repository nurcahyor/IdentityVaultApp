package com.identityvault.app.data

import org.json.JSONArray
import org.json.JSONObject

data class IdentityFieldState(
    val value: String,
    val enabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject()
        .put("value", value)
        .put("enabled", enabled)

    companion object {
        fun fromJson(json: JSONObject?, fallback: String = ""): IdentityFieldState {
            if (json == null) return IdentityFieldState(fallback)
            return IdentityFieldState(
                value = json.optString("value", fallback),
                enabled = json.optBoolean("enabled", true)
            )
        }
    }
}

data class BuildPropProfile(
    val fingerprint: String,
    val brand: String,
    val model: String,
    val manufacturer: String,
    val device: String,
    val name: String,
    val board: String,
    val hardware: String,
    val buildId: String,
    val displayId: String,
    val versionRelease: String,
    val versionSdk: String,
    val securityPatch: String
) {
    fun toJson(): JSONObject = JSONObject()
        .put("ro.build.fingerprint", fingerprint)
        .put("ro.product.brand", brand)
        .put("ro.product.model", model)
        .put("ro.product.manufacturer", manufacturer)
        .put("ro.product.device", device)
        .put("ro.product.name", name)
        .put("ro.product.board", board)
        .put("ro.hardware", hardware)
        .put("ro.build.id", buildId)
        .put("ro.build.display.id", displayId)
        .put("ro.build.version.release", versionRelease)
        .put("ro.build.version.sdk", versionSdk)
        .put("ro.build.version.security_patch", securityPatch)

    companion object {
        fun fromJson(json: JSONObject?): BuildPropProfile {
            val fallback = default()
            if (json == null) return fallback
            return BuildPropProfile(
                fingerprint = json.optString("ro.build.fingerprint", fallback.fingerprint),
                brand = json.optString("ro.product.brand", fallback.brand),
                model = json.optString("ro.product.model", fallback.model),
                manufacturer = json.optString("ro.product.manufacturer", fallback.manufacturer),
                device = json.optString("ro.product.device", fallback.device),
                name = json.optString("ro.product.name", fallback.name),
                board = json.optString("ro.product.board", fallback.board),
                hardware = json.optString("ro.hardware", fallback.hardware),
                buildId = json.optString("ro.build.id", fallback.buildId),
                displayId = json.optString("ro.build.display.id", fallback.displayId),
                versionRelease = json.optString("ro.build.version.release", fallback.versionRelease),
                versionSdk = json.optString("ro.build.version.sdk", fallback.versionSdk),
                securityPatch = json.optString("ro.build.version.security_patch", fallback.securityPatch)
            )
        }

        fun default(): BuildPropProfile = BuildPropProfile(
            fingerprint = "google/panther/panther:13/TQ3A.230805.001/10316531:user/release-keys",
            brand = "google",
            model = "Pixel 7",
            manufacturer = "Google",
            device = "panther",
            name = "panther",
            board = "panther",
            hardware = "panther",
            buildId = "TQ3A.230805.001",
            displayId = "TQ3A.230805.001",
            versionRelease = "13",
            versionSdk = "33",
            securityPatch = "2023-08-05"
        )
    }
}

data class IdentityProfile(
    val name: String,
    val imei: IdentityFieldState,
    val serial: IdentityFieldState,
    val hardwareId: IdentityFieldState,
    val macAddress: IdentityFieldState,
    val macBssid: IdentityFieldState,
    val macSsid: IdentityFieldState,
    val bluetoothMac: IdentityFieldState,
    val androidId: IdentityFieldState,
    val simSerialId: IdentityFieldState,
    val simSubIds: IdentityFieldState,
    val mobileNo: IdentityFieldState,
    val mediaDrmId: IdentityFieldState,
    val simOperator: IdentityFieldState,
    val gsfId: IdentityFieldState,
    val buildProp: BuildPropProfile,
    val buildPropEnabled: Boolean = true,
    val draft: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("imei", imei.toJson())
        .put("serial", serial.toJson())
        .put("hardwareId", hardwareId.toJson())
        .put("macAddress", macAddress.toJson())
        .put("macBssid", macBssid.toJson())
        .put("macSsid", macSsid.toJson())
        .put("bluetoothMac", bluetoothMac.toJson())
        .put("androidId", androidId.toJson())
        .put("simSerialId", simSerialId.toJson())
        .put("simSubIds", simSubIds.toJson())
        .put("mobileNo", mobileNo.toJson())
        .put("mediaDrmId", mediaDrmId.toJson())
        .put("simOperator", simOperator.toJson())
        .put("gsfId", gsfId.toJson())
        .put("buildProp", buildProp.toJson())
        .put("buildPropEnabled", buildPropEnabled)
        .put("draft", draft)

    companion object {
        fun fromJson(json: JSONObject): IdentityProfile = IdentityProfile(
            name = json.optString("name", "Default Profile"),
            imei = IdentityFieldState.fromJson(json.optJSONObject("imei")),
            serial = IdentityFieldState.fromJson(json.optJSONObject("serial")),
            hardwareId = IdentityFieldState.fromJson(json.optJSONObject("hardwareId")),
            macAddress = IdentityFieldState.fromJson(json.optJSONObject("macAddress")),
            macBssid = IdentityFieldState.fromJson(json.optJSONObject("macBssid")),
            macSsid = IdentityFieldState.fromJson(json.optJSONObject("macSsid")),
            bluetoothMac = IdentityFieldState.fromJson(json.optJSONObject("bluetoothMac")),
            androidId = IdentityFieldState.fromJson(json.optJSONObject("androidId")),
            simSerialId = IdentityFieldState.fromJson(json.optJSONObject("simSerialId")),
            simSubIds = IdentityFieldState.fromJson(json.optJSONObject("simSubIds")),
            mobileNo = IdentityFieldState.fromJson(json.optJSONObject("mobileNo")),
            mediaDrmId = IdentityFieldState.fromJson(json.optJSONObject("mediaDrmId")),
            simOperator = IdentityFieldState.fromJson(json.optJSONObject("simOperator")),
            gsfId = IdentityFieldState.fromJson(json.optJSONObject("gsfId")),
            buildProp = BuildPropProfile.fromJson(json.optJSONObject("buildProp")),
            buildPropEnabled = json.optBoolean("buildPropEnabled", true),
            draft = json.optBoolean("draft", false)
        )
    }
}

data class BackupManifest(
    val appId: String,
    val version: Int,
    val createdAt: Long,
    val entries: List<String>
) {
    fun toJson(): JSONObject = JSONObject()
        .put("appId", appId)
        .put("version", version)
        .put("createdAt", createdAt)
        .put("entries", JSONArray(entries))

    companion object {
        fun fromJson(json: JSONObject): BackupManifest {
            val arr = json.optJSONArray("entries") ?: JSONArray()
            return BackupManifest(
                appId = json.optString("appId"),
                version = json.optInt("version"),
                createdAt = json.optLong("createdAt"),
                entries = List(arr.length()) { arr.optString(it) }
            )
        }
    }
}

data class RootStatus(
    val available: Boolean,
    val granted: Boolean,
    val detail: String
)

data class LsposedStatus(
    val installed: Boolean,
    val hookActive: Boolean,
    val hookedPackages: List<String>,
    val detail: String
)

data class OperationResult(
    val success: Boolean,
    val message: String
)
