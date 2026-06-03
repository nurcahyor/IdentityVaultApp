package com.identityvault.hook

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import de.robv.android.xposed.XC_MethodHook

object BluetoothHooks {
    fun install(session: HookSession, profile: HookProfile) {
        hookAddress(session, profile, BluetoothAdapter::class.java, "getAddress")
        hookAddress(session, profile, BluetoothDevice::class.java, "getAddress")
        HookCompat.safeHookMethod(session, "BLUETOOTH", BluetoothAdapter::class.java, "getName", hook = object : XC_MethodHook() {})
        HookCompat.safeHookMethod(session, "BLUETOOTH", BluetoothDevice::class.java, "getName", hook = object : XC_MethodHook() {})
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
}
