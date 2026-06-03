package com.identityvault.hook

import android.net.wifi.WifiInfo
import de.robv.android.xposed.XC_MethodHook

object WifiHooks {
    fun install(session: HookSession, profile: HookProfile) {
        hookString(session, "MAC Address", profile.macAddress, HookValidation::mac, "getMacAddress")
        hookString(session, "MAC BSSID", profile.macBssid, HookValidation::mac, "getBSSID")
        hookString(session, "MAC SSID", profile.macSsid, HookValidation::notBlank, "getSSID") { value ->
            if (value.startsWith("\"") && value.endsWith("\"")) value else "\"$value\""
        }
        HookCompat.safeHookMethod(session, "WIFI", WifiInfo::class.java, "getNetworkId", hook = object : XC_MethodHook() {})
        HookCompat.safeHookMethod(session, "WIFI", WifiInfo::class.java, "getIpAddress", hook = object : XC_MethodHook() {})
    }

    private fun hookString(
        session: HookSession,
        label: String,
        field: HookField,
        validator: (String) -> Boolean,
        methodName: String,
        transform: (String) -> String = { it }
    ) {
        HookCompat.safeHookMethod(session, "WIFI", WifiInfo::class.java, methodName, hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                val value = validField(session, "WIFI", label, field, validator) ?: return
                param.result = transform(value)
                session.applied("WIFI", label, "Applied $label: $value")
            }
        })
    }
}
