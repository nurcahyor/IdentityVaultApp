package com.identityvault.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object HookCompat {
    fun safeHookClass(className: String, classLoader: ClassLoader?): Class<*>? {
        return runCatching {
            XposedHelpers.findClass(className, classLoader)
        }.getOrNull()
    }

    fun safeHookMethod(
        session: HookSession,
        category: String,
        className: String,
        classLoader: ClassLoader?,
        methodName: String,
        vararg parameterTypes: Class<*>,
        hook: XC_MethodHook
    ) {
        val clazz = safeHookClass(className, classLoader)
        if (clazz == null) {
            session.skipped("SKIPPED", category, "$className.$methodName class not found")
            return
        }
        safeHookMethod(session, category, clazz, methodName, *parameterTypes, hook = hook)
    }

    fun safeHookMethod(
        session: HookSession,
        category: String,
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>,
        hook: XC_MethodHook
    ) {
        val signature = "${clazz.simpleName}.$methodName(${parameterTypes.joinToString { it.simpleName }})"
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, *parameterTypes, hook)
            session.installed(category, "Hook installed: $signature")
        } catch (t: NoSuchMethodError) {
            session.skipped("SKIPPED", category, "Skipped $signature: method not found")
        } catch (t: Throwable) {
            session.error(category, "Failed hook $signature: ${t.message.orEmpty()}")
        }
    }

    fun safeSetStaticField(session: HookSession, className: String, classLoader: ClassLoader?, fieldName: String, value: Any?) {
        val clazz = safeHookClass(className, classLoader)
        if (clazz == null) {
            session.skipped("SKIPPED", "BUILD", "$className.$fieldName class not found")
            return
        }
        safeSetStaticField(session, clazz, fieldName, value)
    }

    fun safeSetStaticField(session: HookSession, clazz: Class<*>, fieldName: String, value: Any?) {
        try {
            when (value) {
                is Int -> XposedHelpers.setStaticIntField(clazz, fieldName, value)
                else -> XposedHelpers.setStaticObjectField(clazz, fieldName, value)
            }
            session.applied("BUILD", "Build.$fieldName", "Applied Build.$fieldName = $value")
        } catch (t: Throwable) {
            session.skipped("SKIPPED", "BUILD", "Skipped Build.$fieldName: ${t.message.orEmpty()}")
        }
    }

    fun safeCall(label: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            XposedBridge.log("IdentityVault hook failed at $label: ${t.message}")
        }
    }
}
