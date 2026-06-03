package com.identityvault.hook

import org.json.JSONObject

data class HookField(val value: String, val enabled: Boolean)

data class HookProfile(
    val imei: HookField,
    val serial: HookField,
    val hardwareId: HookField,
    val macAddress: HookField,
    val macBssid: HookField,
    val macSsid: HookField,
    val bluetoothMac: HookField,
    val androidId: HookField,
    val simSerialId: HookField,
    val simSubIds: HookField,
    val mobileNo: HookField,
    val mediaDrmId: HookField,
    val simOperator: HookField,
    val gsfId: HookField,
    val buildPropEnabled: Boolean,
    val build: Map<String, String>
) {
    companion object {
        fun fromJson(json: JSONObject): HookProfile = HookProfile(
            imei = field(json, "imei"),
            serial = field(json, "serial"),
            hardwareId = field(json, "hardwareId"),
            macAddress = field(json, "macAddress"),
            macBssid = field(json, "macBssid"),
            macSsid = field(json, "macSsid"),
            bluetoothMac = field(json, "bluetoothMac"),
            androidId = field(json, "androidId"),
            simSerialId = field(json, "simSerialId"),
            simSubIds = field(json, "simSubIds"),
            mobileNo = field(json, "mobileNo"),
            mediaDrmId = field(json, "mediaDrmId"),
            simOperator = field(json, "simOperator"),
            gsfId = field(json, "gsfId"),
            buildPropEnabled = json.optBoolean("buildPropEnabled", true),
            build = buildMap(json.optJSONObject("buildProp") ?: JSONObject())
        )

        private fun field(json: JSONObject, key: String): HookField {
            val obj = json.optJSONObject(key) ?: JSONObject()
            return HookField(obj.optString("value"), obj.optBoolean("enabled", true))
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
