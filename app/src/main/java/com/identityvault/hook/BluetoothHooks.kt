package com.identityvault.hook

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import de.robv.android.xposed.XC_MethodHook

object BluetoothHooks {
    fun install(session: HookSession, profile: HookProfile) {
        hookAddress(session, profile, BluetoothAdapter::class.java, "getAddress")
        hookAddress(session, profile, BluetoothDevice::class.java, "getAddress")
        hookName(session, profile, BluetoothAdapter::class.java, "getName")
        hookName(session, profile, BluetoothDevice::class.java, "getName")
    }

    private fun hookAddress(session: HookSession, profile: HookProfile, clazz: Class<*>, methodName: String) {
        HookCompat.safeHookMethod(session, "BLUETOOTH", clazz, methodName, hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                val value = validField(session, "BLUETOOTH", "Bluetooth MAC", profile.bluetoothMac, HookValidation::mac) ?: return
                param.result = value
                session.applied("BLUETOOTH", "Bluetooth MAC", "Applied Bluetooth MAC: $value")
            }
        })
    }

    private fun hookName(session: HookSession, profile: HookProfile, clazz: Class<*>, methodName: String) {
        HookCompat.safeHookMethod(session, "BLUETOOTH", clazz, methodName, hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                val value = bluetoothName(session, profile) ?: return
                param.result = value
                session.applied("BLUETOOTH", "Bluetooth Name", "Applied Bluetooth Name: $value")
            }
        })
    }

    private fun bluetoothName(session: HookSession, profile: HookProfile): String? {
        if (profile.bluetoothName.enabled) {
            val value = profile.bluetoothName.value.trim()
            if (HookValidation.bluetoothName(value)) return value
        }
        val fallback = profile.deviceName.value.trim()
            .takeIf { HookValidation.bluetoothName(it) }
            ?: profile.build["ro.product.model"]?.trim()?.takeIf { HookValidation.bluetoothName(it) }
            ?: "Android"
        session.skipped("WARNING", "BLUETOOTH", "Bluetooth Name disabled/invalid, using fallback Build Model: $fallback", "Bluetooth Name")
        return fallback
    }
}
