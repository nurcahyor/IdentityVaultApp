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
