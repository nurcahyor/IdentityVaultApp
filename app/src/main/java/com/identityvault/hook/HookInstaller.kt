package com.identityvault.hook

import android.content.Context
import android.os.Build
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject

object HookInstaller {
    fun installAll(context: Context, lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName ?: return
        val sdk = Build.VERSION.SDK_INT
        val logger = HookDebugLogger(context, packageName, sdk)
        val session = HookSession(packageName, sdk, logger)
        logger.log("SUCCESS", "LSPOSED", "MainHook loaded for $packageName")
        logger.log("INFO", "LSPOSED", "Installing hooks for $packageName SDK $sdk")

        val profile = loadProfile(context, session)
        logger.marker(session)
        if (profile == null) {
            session.skipped("SKIPPED", "PROFILE", "Profile missing", "Profile")
            return
        }
        logger.log("SUCCESS", "PROFILE", "Active profile loaded")

        installGroup(session, "ANDROID_ID") { AndroidIdHooks.install(session, lpparam, profile) }
        installGroup(session, "TELEPHONY") { TelephonyHooks.install(session, profile) }
        installGroup(session, "WIFI") { WifiHooks.install(session, profile) }
        installGroup(session, "BLUETOOTH") { BluetoothHooks.install(session, profile) }
        installGroup(session, "DRM") { DrmHooks.install(session, profile) }
        installGroup(session, "BUILD") { BuildPropHooks.install(session, lpparam, profile) }
        installGroup(session, "SYSTEM_PROPERTIES") { SystemPropertiesHooks.install(session, lpparam, profile) }
        logger.marker(session)
    }

    private fun installGroup(session: HookSession, category: String, block: () -> Unit) {
        try {
            session.logger.log("INFO", category, "Installing $category hooks")
            block()
            session.logger.log("SUCCESS", category, "Installed ${category.toTitle()}Hooks")
        } catch (t: Throwable) {
            session.error(category, "Failed installing ${category.toTitle()}Hooks: ${t.message.orEmpty()}")
        }
    }

    private fun String.toTitle(): String = lowercase()
        .split("_")
        .joinToString("") { it.replaceFirstChar { char -> char.uppercaseChar() } }

    private fun loadProfile(context: Context, session: HookSession): HookProfile? {
        return runCatching {
            val bundle = context.contentResolver.call(HookConstants.PROFILE_URI, "getProfile", null, null)
                ?: throw IllegalStateException("provider returned null")
            val raw = bundle.getString("profileJson").orEmpty()
            val profileJson = JSONObject(raw).optJSONObject("profile")
                ?: throw IllegalStateException("profile JSON empty")
            HookProfile.fromJson(profileJson)
        }.onFailure {
            session.error("PROVIDER", "Failed to load active profile: ${it.message.orEmpty()}")
        }.getOrNull()
    }
}
