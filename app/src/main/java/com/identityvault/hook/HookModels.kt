package com.identityvault.hook

import org.json.JSONObject

data class HookField(val value: String, val enabled: Boolean)

data class HookProfile(
    val imei: HookField,
    val imei2: HookField,
    val meid: HookField,
    val deviceName: HookField,
    val serial: HookField,
    val hardwareId: HookField,
    val macAddress: HookField,
    val macBssid: HookField,
    val macSsid: HookField,
    val bluetoothMac: HookField,
    val bluetoothName: HookField,
    val androidId: HookField,
    val subscriberId: HookField,
    val subscriberId2: HookField,
    val simSerialId: HookField,
    val simSubIds: HookField,
    val mobileNo: HookField,
    val mediaDrmId: HookField,
    val simOperator: HookField,
    val networkOperator: HookField,
    val simOperatorName: HookField,
    val networkOperatorName: HookField,
    val simCountryIso: HookField,
    val networkCountryIso: HookField,
    val phoneType: HookField,
    val networkType: HookField,
    val dataNetworkType: HookField,
    val voiceNetworkType: HookField,
    val googleServicesFrameworkId: HookField,
    val advertisingId: HookField,
    val limitAdTrackingEnabled: HookField,
    val googleAccountEmail: HookField,
    val buildPropEnabled: Boolean,
    val build: Map<String, String>
) {
    companion object {
        fun fromJson(json: JSONObject): HookProfile = HookProfile(
            imei = field(json, "imei1", field(json, "imei").value),
            imei2 = field(json, "imei2", field(json, "imei1", field(json, "imei").value).value),
            meid = field(json, "meid"),
            deviceName = field(json, "deviceName", buildModel(json)),
            serial = field(json, "serial"),
            hardwareId = field(json, "hardwareId"),
            macAddress = field(json, "macAddress"),
            macBssid = field(json, "macBssid"),
            macSsid = field(json, "macSsid"),
            bluetoothMac = field(json, "bluetoothMac"),
            bluetoothName = field(json, "bluetoothName", field(json, "deviceName", buildModel(json).ifBlank { "Android" }).value),
            androidId = field(json, "androidId"),
            subscriberId = field(json, "subscriberId", field(json, "simSubIds").value),
            subscriberId2 = field(json, "subscriberId2"),
            simSerialId = field(json, "simSerialId"),
            simSubIds = field(json, "simSubIds"),
            mobileNo = field(json, "mobileNo"),
            mediaDrmId = field(json, "mediaDrmId"),
            simOperator = field(json, "simOperator"),
            networkOperator = field(json, "networkOperator", field(json, "simOperator").value),
            simOperatorName = field(json, "simOperatorName", operatorName(field(json, "simOperator").value)),
            networkOperatorName = field(json, "networkOperatorName", operatorName(field(json, "simOperator").value)),
            simCountryIso = field(json, "simCountryIso", countryIso(field(json, "simOperator").value)),
            networkCountryIso = field(json, "networkCountryIso", countryIso(field(json, "simOperator").value)),
            phoneType = field(json, "phoneType", "1"),
            networkType = field(json, "networkType", "13"),
            dataNetworkType = field(json, "dataNetworkType", "13"),
            voiceNetworkType = field(json, "voiceNetworkType", "13"),
            googleServicesFrameworkId = field(json, "googleServicesFrameworkId", field(json, "gsfId").value),
            advertisingId = field(json, "advertisingId"),
            limitAdTrackingEnabled = field(json, "limitAdTrackingEnabled", "false"),
            googleAccountEmail = field(json, "googleAccountEmail"),
            buildPropEnabled = json.optBoolean("buildPropEnabled", true),
            build = buildMap(json.optJSONObject("buildProp") ?: JSONObject())
        )

        private fun field(json: JSONObject, key: String, fallback: String = ""): HookField {
            val obj = json.optJSONObject(key) ?: JSONObject()
            return HookField(obj.optString("value", fallback), obj.optBoolean("enabled", true))
        }

        private fun buildMap(json: JSONObject): Map<String, String> {
            val map = linkedMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optString(key)
            }
            return map
        }

        private fun buildModel(json: JSONObject): String {
            return json.optJSONObject("buildProp")?.optString("ro.product.model").orEmpty()
        }

        private fun operatorName(operator: String): String = when (operator) {
            "51010" -> "Telkomsel"
            "310260" -> "T-Mobile"
            "311480" -> "Verizon"
            "302720" -> "Rogers"
            "46601" -> "FarEasTone"
            else -> ""
        }

        private fun countryIso(operator: String): String = when {
            operator.startsWith("510") -> "id"
            operator.startsWith("310") || operator.startsWith("311") -> "us"
            operator.startsWith("302") -> "ca"
            operator.startsWith("466") -> "tw"
            else -> ""
        }
    }
}

data class HookSession(
    val packageName: String,
    val sdkInt: Int,
    val logger: HookDebugLogger
) {
    val hooksInstalled = linkedSetOf<String>()
    val appliedFields = linkedSetOf<String>()
    val skippedHooks = linkedSetOf<String>()
    val skippedFields = linkedSetOf<String>()
    val errors = linkedSetOf<String>()
}
