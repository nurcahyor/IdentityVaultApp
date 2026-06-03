package com.identityvault.hook

import android.os.Build
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

object BuildPropHooks {
    fun install(session: HookSession, lpparam: XC_LoadPackage.LoadPackageParam, profile: HookProfile) {
        if (!profile.buildPropEnabled) {
            session.skipped("WARNING", "BUILD", "Build Prop disabled, using original values", "Build Prop")
            return
        }
        val build = profile.build
        if (!HookValidation.buildProp(build["ro.build.fingerprint"].orEmpty())) {
            session.skipped("SKIPPED", "BUILD", "Build Prop invalid, using original values", "Build Prop")
            return
        }
        mapOf(
            "BRAND" to "ro.product.brand",
            "MODEL" to "ro.product.model",
            "MANUFACTURER" to "ro.product.manufacturer",
            "DEVICE" to "ro.product.device",
            "PRODUCT" to "ro.product.name",
            "BOARD" to "ro.product.board",
            "HARDWARE" to "ro.hardware",
            "FINGERPRINT" to "ro.build.fingerprint",
            "ID" to "ro.build.id",
            "DISPLAY" to "ro.build.display.id",
            "TAGS" to "ro.build.tags",
            "TYPE" to "ro.build.type"
        ).forEach { (field, key) ->
            build[key]?.takeIf { it.isNotBlank() }?.let { HookCompat.safeSetStaticField(session, Build::class.java, field, it) }
        }
        mapOf(
            "RELEASE" to "ro.build.version.release",
            "SDK" to "ro.build.version.sdk",
            "SECURITY_PATCH" to "ro.build.version.security_patch"
        ).forEach { (field, key) ->
            build[key]?.takeIf { it.isNotBlank() }?.let { HookCompat.safeSetStaticField(session, Build.VERSION::class.java, field, it) }
        }
        build["ro.build.version.sdk"]?.toIntOrNull()?.takeIf { it in 28..34 }?.let {
            HookCompat.safeSetStaticField(session, Build.VERSION::class.java, "SDK_INT", it)
        }
        hookBuildSerial(session, lpparam, profile)
    }

    private fun hookBuildSerial(session: HookSession, lpparam: XC_LoadPackage.LoadPackageParam, profile: HookProfile) {
        HookCompat.safeHookMethod(session, "BUILD", "android.os.Build", lpparam.classLoader, "getSerial", hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                val value = validField(session, "BUILD", "Serial", profile.serial, HookValidation::notBlank) ?: return
                param.result = value
                session.applied("BUILD", "Serial", "Applied Build.getSerial: $value")
            }
        })
    }
}
