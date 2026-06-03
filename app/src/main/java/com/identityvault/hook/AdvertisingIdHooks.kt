package com.identityvault.hook

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

object AdvertisingIdHooks {
    private const val CLIENT_CLASS = "com.google.android.gms.ads.identifier.AdvertisingIdClient"
    private const val INFO_CLASS = "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info"

    fun install(session: HookSession, lpparam: XC_LoadPackage.LoadPackageParam, profile: HookProfile) {
        val client = HookCompat.safeHookClass(CLIENT_CLASS, lpparam.classLoader)
        val info = HookCompat.safeHookClass(INFO_CLASS, lpparam.classLoader)

        if (client == null && info == null) {
            session.skipped("SKIPPED", "ADVERTISING_ID", "AdvertisingIdClient class not found in this target app", "Advertising ID")
            session.logger.log(
                "INFO",
                "ADVERTISING_ID",
                "Target app does not include Google Advertising ID client library. Test with app that actually reads Advertising ID."
            )
            return
        }

        if (client == null) {
            session.skipped("SKIPPED", "ADVERTISING_ID", "Skipped AdvertisingIdClient.getAdvertisingIdInfo: class not found", "Advertising ID")
        } else {
            HookCompat.safeHookMethod(session, "ADVERTISING_ID", client, "getAdvertisingIdInfo", Context::class.java, hook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    applyId(session, profile, "AdvertisingIdClient.getAdvertisingIdInfo")
                }
            })
        }

        if (info == null) {
            session.skipped("SKIPPED", "ADVERTISING_ID", "Skipped AdvertisingIdClient.Info.getId: class not found", "Advertising ID")
            return
        }

        HookCompat.safeHookMethod(session, "ADVERTISING_ID", info, "getId", hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                val value = applyId(session, profile, "AdvertisingIdClient.Info.getId") ?: return
                param.result = value
            }
        })
        HookCompat.safeHookMethod(session, "ADVERTISING_ID", info, "isLimitAdTrackingEnabled", hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                validField(session, "ADVERTISING_ID", "Advertising ID", profile.advertisingId, HookValidation::uuid) ?: return
                val raw = validField(session, "ADVERTISING_ID", "Limit Ad Tracking", profile.limitAdTrackingEnabled, HookValidation::boolean) ?: return
                val value = raw.equals("true", ignoreCase = true)
                param.result = value
                session.applied("ADVERTISING_ID", "Limit Ad Tracking", "Applied Limit Ad Tracking: $value")
            }
        })
    }

    private fun applyId(session: HookSession, profile: HookProfile, source: String): String? {
        val value = validField(session, "ADVERTISING_ID", "Advertising ID", profile.advertisingId, HookValidation::uuid) ?: return null
        session.applied("ADVERTISING_ID", "Advertising ID", "Applied Advertising ID: $value via $source")
        return value
    }
}
