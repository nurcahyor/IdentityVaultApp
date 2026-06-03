package com.identityvault.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SystemPropertiesHooks {
    fun install(session: HookSession, lpparam: XC_LoadPackage.LoadPackageParam, profile: HookProfile) {
        val props = properties(profile)
        if (props.isEmpty()) {
            session.skipped("WARNING", "SYSTEM_PROPERTIES", "Build Prop disabled or empty, using original properties")
            return
        }
        val className = "android.os.SystemProperties"
        HookCompat.safeHookMethod(session, "SYSTEM_PROPERTIES", className, lpparam.classLoader, "get", String::class.java, hook = stringHook(session, props))
        HookCompat.safeHookMethod(session, "SYSTEM_PROPERTIES", className, lpparam.classLoader, "get", String::class.java, String::class.java, hook = stringHook(session, props))
        HookCompat.safeHookMethod(session, "SYSTEM_PROPERTIES", className, lpparam.classLoader, "getInt", String::class.java, Int::class.javaPrimitiveType!!, hook = typedHook(session, props) { it.toIntOrNull() })
        HookCompat.safeHookMethod(session, "SYSTEM_PROPERTIES", className, lpparam.classLoader, "getLong", String::class.java, Long::class.javaPrimitiveType!!, hook = typedHook(session, props) { it.toLongOrNull() })
        HookCompat.safeHookMethod(session, "SYSTEM_PROPERTIES", className, lpparam.classLoader, "getBoolean", String::class.java, Boolean::class.javaPrimitiveType!!, hook = typedHook(session, props) { it.toBooleanStrictOrNull() })
    }

    private fun properties(profile: HookProfile): Map<String, String> {
        if (!profile.buildPropEnabled) return emptyMap()
        val map = profile.build.toMutableMap()
        fun copyKeys(source: String, targets: List<String>) {
            val value = map[source]?.takeIf { it.isNotBlank() } ?: return
            targets.forEach { map[it] = value }
        }
        copyKeys(
            "ro.product.model",
            listOf(
                "ro.product.marketname",
                "ro.product.vendor.marketname",
                "ro.product.odm.marketname",
                "ro.product.system.marketname",
                "ro.product.vendor.model",
                "ro.product.odm.model",
                "ro.product.system.model",
                "ro.product.product.model"
            )
        )
        copyKeys(
            "ro.product.brand",
            listOf("ro.product.vendor.brand", "ro.product.odm.brand", "ro.product.system.brand")
        )
        copyKeys(
            "ro.product.manufacturer",
            listOf("ro.product.vendor.manufacturer", "ro.product.odm.manufacturer")
        )
        copyKeys(
            "ro.product.device",
            listOf("ro.product.vendor.device", "ro.product.odm.device")
        )
        copyKeys(
            "ro.product.name",
            listOf("ro.product.vendor.name", "ro.product.odm.name")
        )
        val deviceName = profile.deviceName.value.trim().ifBlank { map["ro.product.model"].orEmpty() }
        if (deviceName.isNotBlank()) {
            map["persist.sys.device_name"] = deviceName
            map["bluetooth.device.default_name"] = deviceName
            map["net.hostname"] = deviceName
        }
        if (profile.serial.enabled && profile.serial.value.isNotBlank()) {
            map["ro.serialno"] = profile.serial.value
            map["ril.serialnumber"] = profile.serial.value
        }
        return map.filterValues { it.isNotBlank() }
    }

    private fun stringHook(session: HookSession, props: Map<String, String>) = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
        override fun afterHookedMethod(param: MethodHookParam) = apply(param)
        private fun apply(param: MethodHookParam) {
            val key = param.args.firstOrNull() as? String ?: return
            val value = props[key] ?: return
            param.result = value
            session.applied("SYSTEM_PROPERTIES", key, "Applied SystemProperties key $key")
        }
    }

    private fun <T> typedHook(session: HookSession, props: Map<String, String>, parser: (String) -> T?) = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val key = param.args.firstOrNull() as? String ?: return
            val raw = props[key] ?: return
            val value = parser(raw)
            if (value == null) {
                session.skipped("SKIPPED", "SYSTEM_PROPERTIES", "Cannot parse $key for typed SystemProperties getter", key)
                return
            }
            param.result = value
            session.applied("SYSTEM_PROPERTIES", key, "Applied SystemProperties key $key")
        }
    }
}
