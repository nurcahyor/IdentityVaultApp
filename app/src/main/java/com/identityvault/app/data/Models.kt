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
    val imei2: IdentityFieldState = IdentityFieldState(""),
    val meid: IdentityFieldState = IdentityFieldState(""),
    val deviceName: IdentityFieldState = IdentityFieldState(""),
    val serial: IdentityFieldState,
    val hardwareId: IdentityFieldState,
    val macAddress: IdentityFieldState,
    val macBssid: IdentityFieldState,
    val macSsid: IdentityFieldState,
    val bluetoothMac: IdentityFieldState,
    val bluetoothName: IdentityFieldState = IdentityFieldState(""),
    val androidId: IdentityFieldState,
    val subscriberId: IdentityFieldState = IdentityFieldState(""),
    val subscriberId2: IdentityFieldState = IdentityFieldState("", enabled = false),
    val simSerialId: IdentityFieldState,
    val simSubIds: IdentityFieldState,
    val mobileNo: IdentityFieldState,
    val mediaDrmId: IdentityFieldState,
    val simOperator: IdentityFieldState,
    val networkOperator: IdentityFieldState = IdentityFieldState(""),
    val simOperatorName: IdentityFieldState = IdentityFieldState(""),
    val networkOperatorName: IdentityFieldState = IdentityFieldState(""),
    val simCountryIso: IdentityFieldState = IdentityFieldState(""),
    val networkCountryIso: IdentityFieldState = IdentityFieldState(""),
    val phoneType: IdentityFieldState = IdentityFieldState("1"),
    val networkType: IdentityFieldState = IdentityFieldState("13"),
    val dataNetworkType: IdentityFieldState = IdentityFieldState("13"),
    val voiceNetworkType: IdentityFieldState = IdentityFieldState("13"),
    val gsfId: IdentityFieldState,
    val advertisingId: IdentityFieldState = IdentityFieldState(""),
    val limitAdTrackingEnabled: IdentityFieldState = IdentityFieldState("false"),
    val googleAccountEmail: IdentityFieldState = IdentityFieldState(""),
    val buildProp: BuildPropProfile,
    val buildPropEnabled: Boolean = true,
    val draft: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("imei1", imei.toJson())
        .put("imei", imei.toJson())
        .put("imei2", imei2.toJson())
        .put("meid", meid.toJson())
        .put("deviceName", deviceName.toJson())
        .put("serial", serial.toJson())
        .put("hardwareId", hardwareId.toJson())
        .put("macAddress", macAddress.toJson())
        .put("macBssid", macBssid.toJson())
        .put("macSsid", macSsid.toJson())
        .put("bluetoothMac", bluetoothMac.toJson())
        .put("bluetoothName", bluetoothName.toJson())
        .put("androidId", androidId.toJson())
        .put("subscriberId", subscriberId.toJson())
        .put("subscriberId2", subscriberId2.toJson())
        .put("simSerialId", simSerialId.toJson())
        .put("simSubIds", simSubIds.toJson())
        .put("mobileNo", mobileNo.toJson())
        .put("mediaDrmId", mediaDrmId.toJson())
        .put("simOperator", simOperator.toJson())
        .put("networkOperator", networkOperator.toJson())
        .put("simOperatorName", simOperatorName.toJson())
        .put("networkOperatorName", networkOperatorName.toJson())
        .put("simCountryIso", simCountryIso.toJson())
        .put("networkCountryIso", networkCountryIso.toJson())
        .put("phoneType", phoneType.toJson())
        .put("networkType", networkType.toJson())
        .put("dataNetworkType", dataNetworkType.toJson())
        .put("voiceNetworkType", voiceNetworkType.toJson())
        .put("gsfId", gsfId.toJson())
        .put("googleServicesFrameworkId", gsfId.toJson())
        .put("advertisingId", advertisingId.toJson())
        .put("limitAdTrackingEnabled", limitAdTrackingEnabled.toJson())
        .put("googleAccountEmail", googleAccountEmail.toJson())
        .put("buildProp", buildProp.toJson())
        .put("buildPropEnabled", buildPropEnabled)
        .put("draft", draft)

    companion object {
        fun fromJson(json: JSONObject): IdentityProfile = IdentityProfile(
            name = json.optString("name", "Default Profile"),
            imei = IdentityFieldState.fromJson(json.optJSONObject("imei1") ?: json.optJSONObject("imei")),
            imei2 = IdentityFieldState.fromJson(json.optJSONObject("imei2"), IdentityFieldState.fromJson(json.optJSONObject("imei1") ?: json.optJSONObject("imei")).value),
            meid = IdentityFieldState.fromJson(json.optJSONObject("meid")),
            deviceName = IdentityFieldState.fromJson(json.optJSONObject("deviceName"), BuildPropProfile.fromJson(json.optJSONObject("buildProp")).model),
            serial = IdentityFieldState.fromJson(json.optJSONObject("serial")),
            hardwareId = IdentityFieldState.fromJson(json.optJSONObject("hardwareId")),
            macAddress = IdentityFieldState.fromJson(json.optJSONObject("macAddress")),
            macBssid = IdentityFieldState.fromJson(json.optJSONObject("macBssid")),
            macSsid = IdentityFieldState.fromJson(json.optJSONObject("macSsid")),
            bluetoothMac = IdentityFieldState.fromJson(json.optJSONObject("bluetoothMac")),
            bluetoothName = IdentityFieldState.fromJson(
                json.optJSONObject("bluetoothName"),
                IdentityFieldState.fromJson(json.optJSONObject("deviceName"), BuildPropProfile.fromJson(json.optJSONObject("buildProp")).model).value
                    .ifBlank { BuildPropProfile.fromJson(json.optJSONObject("buildProp")).model }
                    .ifBlank { "Android" }
            ),
            androidId = IdentityFieldState.fromJson(json.optJSONObject("androidId")),
            subscriberId = IdentityFieldState.fromJson(json.optJSONObject("subscriberId") ?: json.optJSONObject("simSubIds")),
            subscriberId2 = IdentityFieldState.fromJson(json.optJSONObject("subscriberId2"), IdentityFieldState.fromJson(json.optJSONObject("subscriberId") ?: json.optJSONObject("simSubIds")).value),
            simSerialId = IdentityFieldState.fromJson(json.optJSONObject("simSerialId")),
            simSubIds = IdentityFieldState.fromJson(json.optJSONObject("simSubIds")),
            mobileNo = IdentityFieldState.fromJson(json.optJSONObject("mobileNo")),
            mediaDrmId = IdentityFieldState.fromJson(json.optJSONObject("mediaDrmId")),
            simOperator = IdentityFieldState.fromJson(json.optJSONObject("simOperator")),
            networkOperator = IdentityFieldState.fromJson(json.optJSONObject("networkOperator"), IdentityFieldState.fromJson(json.optJSONObject("simOperator")).value),
            simOperatorName = IdentityFieldState.fromJson(json.optJSONObject("simOperatorName"), operatorName(IdentityFieldState.fromJson(json.optJSONObject("simOperator")).value)),
            networkOperatorName = IdentityFieldState.fromJson(json.optJSONObject("networkOperatorName"), operatorName(IdentityFieldState.fromJson(json.optJSONObject("networkOperator") ?: json.optJSONObject("simOperator")).value)),
            simCountryIso = IdentityFieldState.fromJson(json.optJSONObject("simCountryIso"), countryIso(IdentityFieldState.fromJson(json.optJSONObject("simOperator")).value)),
            networkCountryIso = IdentityFieldState.fromJson(json.optJSONObject("networkCountryIso"), countryIso(IdentityFieldState.fromJson(json.optJSONObject("networkOperator") ?: json.optJSONObject("simOperator")).value)),
            phoneType = IdentityFieldState.fromJson(json.optJSONObject("phoneType"), "1"),
            networkType = IdentityFieldState.fromJson(json.optJSONObject("networkType"), "13"),
            dataNetworkType = IdentityFieldState.fromJson(json.optJSONObject("dataNetworkType"), "13"),
            voiceNetworkType = IdentityFieldState.fromJson(json.optJSONObject("voiceNetworkType"), "13"),
            gsfId = IdentityFieldState.fromJson(json.optJSONObject("googleServicesFrameworkId") ?: json.optJSONObject("gsfId")),
            advertisingId = IdentityFieldState.fromJson(json.optJSONObject("advertisingId")),
            limitAdTrackingEnabled = IdentityFieldState.fromJson(json.optJSONObject("limitAdTrackingEnabled"), "false"),
            googleAccountEmail = IdentityFieldState.fromJson(json.optJSONObject("googleAccountEmail")),
            buildProp = BuildPropProfile.fromJson(json.optJSONObject("buildProp")),
            buildPropEnabled = json.optBoolean("buildPropEnabled", true),
            draft = json.optBoolean("draft", false)
        )

        private fun operatorName(operator: String): String = when (operator) {
            "51010" -> "Telkomsel"
            "310260" -> "T-Mobile"
            "311480" -> "Verizon"
            "302720" -> "Rogers"
            "46601" -> "FarEasTone"
            else -> "Telkomsel"
        }

        private fun countryIso(operator: String): String = when {
            operator.startsWith("510") -> "id"
            operator.startsWith("310") || operator.startsWith("311") -> "us"
            operator.startsWith("302") -> "ca"
            operator.startsWith("466") -> "tw"
            else -> "id"
        }
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
