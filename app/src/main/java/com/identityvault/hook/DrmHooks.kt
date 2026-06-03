package com.identityvault.hook

import android.media.MediaDrm
import de.robv.android.xposed.XC_MethodHook

object DrmHooks {
    fun install(session: HookSession, profile: HookProfile) {
        HookCompat.safeHookMethod(session, "DRM", MediaDrm::class.java, "getPropertyByteArray", String::class.java, hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                val property = param.args.firstOrNull() as? String ?: return
                if (property != "deviceUniqueId") return
                val value = validField(session, "DRM", "MediaDrm ID", profile.mediaDrmId, HookValidation::mediaDrm) ?: return
                param.result = HookValidation.hexBytes(value) ?: value.toByteArray(Charsets.UTF_8)
                session.applied("DRM", "MediaDrm ID", "Applied MediaDrm ID")
            }
        })
    }
}
