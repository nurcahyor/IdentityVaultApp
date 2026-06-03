package com.identityvault.hook

import android.content.ContentResolver
import android.provider.Settings
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

object AndroidIdHooks {
    fun install(session: HookSession, lpparam: XC_LoadPackage.LoadPackageParam, profile: HookProfile) {
        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = overrideAndroidId(param, session, profile)
            override fun afterHookedMethod(param: MethodHookParam) = overrideAndroidId(param, session, profile)
        }
        HookCompat.safeHookMethod(session, "ANDROID_ID", Settings.Secure::class.java, "getString", ContentResolver::class.java, String::class.java, hook = callback)
        HookCompat.safeHookMethod(session, "ANDROID_ID", Settings.Secure::class.java, "getStringForUser", ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType!!, hook = callback)
        HookCompat.safeHookMethod(session, "ANDROID_ID", Settings.Global::class.java, "getString", ContentResolver::class.java, String::class.java, hook = callback)
    }

    private fun overrideAndroidId(param: XC_MethodHook.MethodHookParam, session: HookSession, profile: HookProfile) {
        val name = param.args.getOrNull(1) as? String ?: return
        if (name != Settings.Secure.ANDROID_ID && name != "android_id") return
        val field = profile.androidId
        val value = validField(session, "ANDROID_ID", "Android ID", field, HookValidation::hex16) ?: return
        param.result = value
        session.applied("ANDROID_ID", "AndroidID", "Applied Android ID: $value")
    }
}
