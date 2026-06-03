package com.identityvault.hook

import android.content.ContentResolver
import android.provider.Settings
import de.robv.android.xposed.XC_MethodHook

object SettingsNameHooks {
    private const val CATEGORY = "SETTINGS_NAME"
    private val keys = setOf("device_name", "bluetooth_name")

    fun install(session: HookSession, profile: HookProfile) {
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param, session, profile)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param, session, profile)
        }
        HookCompat.safeHookMethod(session, CATEGORY, Settings.Global::class.java, "getString", ContentResolver::class.java, String::class.java, hook = hook)
        HookCompat.safeHookMethod(session, CATEGORY, Settings.Secure::class.java, "getString", ContentResolver::class.java, String::class.java, hook = hook)
        HookCompat.safeHookMethod(session, CATEGORY, Settings.System::class.java, "getString", ContentResolver::class.java, String::class.java, hook = hook)
    }

    private fun apply(param: XC_MethodHook.MethodHookParam, session: HookSession, profile: HookProfile) {
        val name = param.args.getOrNull(1) as? String ?: return
        if (name !in keys) return
        val value = if (name == "bluetooth_name") {
            profile.bluetoothName.value.trim().takeIf { profile.bluetoothName.enabled && HookValidation.bluetoothName(it) }
                ?: profile.deviceName.value.trim().takeIf { HookValidation.bluetoothName(it) }
                ?: profile.build["ro.product.model"]?.takeIf { HookValidation.bluetoothName(it) }
        } else {
            validField(session, CATEGORY, "Device Name", profile.deviceName, HookValidation::notBlank)
                ?: profile.build["ro.product.model"]?.takeIf { it.isNotBlank() }
        } ?: return
        param.result = value
        session.applied(CATEGORY, name, "Applied Settings name $name: $value")
    }
}
