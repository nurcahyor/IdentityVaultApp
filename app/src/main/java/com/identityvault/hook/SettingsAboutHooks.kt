package com.identityvault.hook

import android.content.Context
import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SettingsAboutHooks {
    private const val CATEGORY = "SETTINGS_ABOUT"
    private val applyingPreferenceValue = ThreadLocal.withInitial { false }

    fun install(
        context: Context,
        lpparam: XC_LoadPackage.LoadPackageParam,
        profile: HookProfile,
        session: HookSession
    ) {
        if (lpparam.packageName != "com.android.settings") return

        session.logger.log("INFO", CATEGORY, "Android Settings detected")
        session.logger.log("INFO", CATEGORY, "Installing Settings About hooks")
        session.logger.log(
            "INFO",
            CATEGORY,
            "Emulator host/device model panel may show original emulator template because it is outside scoped Android app processes."
        )

        var installedAny = false
        preferenceClasses(lpparam.classLoader).forEach { preferenceClass ->
            installedAny = true
            hookPreferenceText(preferenceClass, profile, session)
        }

        installedAny = hookControllerClasses(lpparam, profile, session) || installedAny

        if (!installedAny) {
            session.skipped(
                "WARNING",
                CATEGORY,
                "Settings About hooks not installed; About phone may show cached/original values."
            )
        }
    }

    private fun preferenceClasses(classLoader: ClassLoader?): List<Class<*>> {
        return listOf(
            "androidx.preference.Preference",
            "android.support.v7.preference.Preference"
        ).mapNotNull { HookCompat.safeHookClass(it, classLoader) }
    }

    private fun hookPreferenceText(preferenceClass: Class<*>, profile: HookProfile, session: HookSession) {
        HookCompat.safeHookMethod(
            session,
            CATEGORY,
            preferenceClass,
            "setSummary",
            CharSequence::class.java,
            hook = preferenceCharSequenceHook("summary", profile, session)
        )
        HookCompat.safeHookMethod(
            session,
            CATEGORY,
            preferenceClass,
            "setSummary",
            Int::class.javaPrimitiveType!!,
            hook = preferenceResourceHook("setSummary", profile, session)
        )
        HookCompat.safeHookMethod(
            session,
            CATEGORY,
            preferenceClass,
            "getSummary",
            hook = preferenceGetterHook("summary", profile, session)
        )
    }

    private fun preferenceCharSequenceHook(
        label: String,
        profile: HookProfile,
        session: HookSession
    ) = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (applyingPreferenceValue.get() == true) return
            val key = preferenceKey(param.thisObject) ?: return
            val replacement = preferenceValue(key, profile, session) ?: return
            val old = param.args.firstOrNull()?.toString().orEmpty()
            param.args[0] = replacement
            logPreferenceReplacement(session, key, old, replacement, label)
        }
    }

    private fun preferenceResourceHook(
        methodName: String,
        profile: HookProfile,
        session: HookSession
    ) = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (applyingPreferenceValue.get() == true) return
            val key = preferenceKey(param.thisObject) ?: return
            val replacement = preferenceValue(key, profile, session) ?: return
            setPreferenceText(param.thisObject, methodName, replacement)
            param.result = null
            logPreferenceReplacement(session, key, "", replacement, methodName.removePrefix("set").lowercase())
        }
    }

    private fun preferenceGetterHook(
        label: String,
        profile: HookProfile,
        session: HookSession
    ) = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val key = preferenceKey(param.thisObject) ?: return
            val replacement = preferenceValue(key, profile, session) ?: return
            val old = param.result?.toString().orEmpty()
            param.result = replacement
            logPreferenceReplacement(session, key, old, replacement, label)
        }
    }

    private fun hookControllerClasses(
        lpparam: XC_LoadPackage.LoadPackageParam,
        profile: HookProfile,
        session: HookSession
    ): Boolean {
        var installedAny = false
        val preferenceClass = preferenceClasses(lpparam.classLoader).firstOrNull()
        val preferenceScreenClass = listOf(
            "androidx.preference.PreferenceScreen",
            "android.support.v7.preference.PreferenceScreen"
        ).firstNotNullOfOrNull { HookCompat.safeHookClass(it, lpparam.classLoader) }

        val classNames = listOf(
            "com.android.settings.deviceinfo.DeviceNamePreferenceController",
            "com.android.settings.deviceinfo.aboutphone.DeviceNamePreferenceController",
            "com.android.settings.deviceinfo.aboutphone.MyDeviceInfoFragment",
            "com.android.settings.deviceinfo.MyDeviceInfoFragment",
            "com.android.settings.deviceinfo.DeviceModelPreferenceController",
            "com.android.settings.deviceinfo.firmwareversion.FirmwareVersionPreferenceController",
            "com.android.settings.deviceinfo.BuildNumberPreferenceController",
            "com.android.settings.deviceinfo.imei.ImeiInfoPreferenceController",
            "com.android.settings.deviceinfo.simstatus.SimStatusDialogController"
        )

        classNames.forEach { className ->
            val clazz = HookCompat.safeHookClass(className, lpparam.classLoader)
            if (clazz == null) {
                session.skipped("SKIPPED", CATEGORY, "$className class not found")
                return@forEach
            }
            installedAny = true
            hookControllerSummary(clazz, profile, session)
            hookNamedControllerMethod(clazz, "getDeviceName", profile, session)
            hookNamedControllerMethod(clazz, "getDeviceModel", profile, session)
            if (preferenceClass != null) hookUpdateState(clazz, preferenceClass, profile, session)
            if (preferenceScreenClass != null) hookDisplayPreference(clazz, preferenceScreenClass, session)
            hookLifecycle(clazz, session)
        }
        return installedAny
    }

    private fun hookNamedControllerMethod(
        clazz: Class<*>,
        methodName: String,
        profile: HookProfile,
        session: HookSession
    ) {
        HookCompat.safeHookMethod(session, CATEGORY, clazz, methodName, hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val replacement = when (methodName) {
                    "getDeviceName" -> validField(session, CATEGORY, "Device Name", profile.deviceName, HookValidation::notBlank)
                        ?: buildValue(profile, "ro.product.model")
                    "getDeviceModel" -> buildValue(profile, "ro.product.model")
                    else -> null
                } ?: return
                param.result = replacement
                session.applied(CATEGORY, "SettingsAbout:${clazz.simpleName}:$methodName", "Applied $methodName: $replacement")
            }
        })
    }

    private fun hookControllerSummary(clazz: Class<*>, profile: HookProfile, session: HookSession) {
        HookCompat.safeHookMethod(session, CATEGORY, clazz, "getSummary", hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val replacement = controllerSummary(clazz.name, profile, session) ?: return
                param.result = replacement
                logControllerReplacement(session, clazz.name, replacement)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val replacement = controllerSummary(clazz.name, profile, session) ?: return
                param.result = replacement
                logControllerReplacement(session, clazz.name, replacement)
            }
        })
    }

    private fun hookUpdateState(
        clazz: Class<*>,
        preferenceClass: Class<*>,
        profile: HookProfile,
        session: HookSession
    ) {
        HookCompat.safeHookMethod(
            session,
            CATEGORY,
            clazz,
            "updateState",
            preferenceClass,
            hook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val preference = param.args.firstOrNull() ?: return
                    applyPreferenceValue(preference, profile, session)
                }
            }
        )
    }

    private fun hookDisplayPreference(clazz: Class<*>, preferenceScreenClass: Class<*>, session: HookSession) {
        HookCompat.safeHookMethod(
            session,
            CATEGORY,
            clazz,
            "displayPreference",
            preferenceScreenClass,
            hook = object : XC_MethodHook() {}
        )
    }

    private fun hookLifecycle(clazz: Class<*>, session: HookSession) {
        HookCompat.safeHookMethod(session, CATEGORY, clazz, "onResume", hook = object : XC_MethodHook() {})
        HookCompat.safeHookMethod(
            session,
            CATEGORY,
            clazz,
            "onCreatePreferences",
            Bundle::class.java,
            String::class.java,
            hook = object : XC_MethodHook() {}
        )
    }

    private fun applyPreferenceValue(preference: Any, profile: HookProfile, session: HookSession) {
        val key = preferenceKey(preference) ?: return
        val replacement = preferenceValue(key, profile, session) ?: return
        setPreferenceText(preference, "setSummary", replacement)
        logPreferenceReplacement(session, key, "", replacement, "summary")
    }

    private fun preferenceKey(preference: Any?): String? {
        if (preference == null) return null
        return runCatching {
            XposedHelpers.callMethod(preference, "getKey") as? String
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun setPreferenceText(preference: Any, methodName: String, value: String) {
        applyingPreferenceValue.set(true)
        try {
            XposedHelpers.callMethod(preference, methodName, value as CharSequence)
        } finally {
            applyingPreferenceValue.set(false)
        }
    }

    private fun preferenceValue(key: String, profile: HookProfile, session: HookSession): String? {
        return when {
            key.contains("device_name", true) -> validField(session, CATEGORY, "Device Name", profile.deviceName, HookValidation::notBlank)
                ?: buildValue(profile, "ro.product.model")
            key.contains("device_model", true) ||
                key.contains("model_number", true) ||
                key.contains("about_phone", true) ||
                key.contains("my_device_info", true) ||
                key.equals("model", true) ||
                key.contains("_model", true) -> buildValue(profile, "ro.product.model")
                ?.also { session.applied(CATEGORY, "SettingsAbout:model", "Applied Settings About model: $it") }
            key.contains("firmware_version", true) || key.contains("android_version", true) -> buildValue(profile, "ro.build.version.release")
            key.contains("build_number", true) -> buildValue(profile, "ro.build.display.id") ?: buildValue(profile, "ro.build.id")
            key.contains("security_patch", true) -> buildValue(profile, "ro.build.version.security_patch")
            key.contains("imei", true) -> validField(session, CATEGORY, "IMEI 1", profile.imei, HookValidation::imei)
            key.contains("meid", true) -> validField(session, CATEGORY, "MEID", profile.meid, HookValidation::meid)
            key.contains("phone_number", true) -> validField(session, CATEGORY, "Mobile No", profile.mobileNo, HookValidation::mobile)
            key.contains("sim_status", true) -> validField(session, CATEGORY, "SIM Operator", profile.simOperator, HookValidation::simOperator)
            else -> null
        }
    }

    private fun controllerSummary(className: String, profile: HookProfile, session: HookSession): String? {
        return when {
            className.contains("DeviceModel", ignoreCase = true) ->
                buildValue(profile, "ro.product.model")
                    ?.also { session.applied(CATEGORY, "SettingsAbout:model", "Applied Settings About model: $it") }
            className.contains("DeviceName", ignoreCase = true) ->
                validField(session, CATEGORY, "Device Name", profile.deviceName, HookValidation::notBlank)
                    ?: buildValue(profile, "ro.product.model")
            className.contains("FirmwareVersion", ignoreCase = true) ->
                buildValue(profile, "ro.build.version.release")
            className.contains("BuildNumber", ignoreCase = true) ->
                buildValue(profile, "ro.build.display.id") ?: buildValue(profile, "ro.build.id")
            className.contains("Imei", ignoreCase = true) ->
                validField(session, CATEGORY, "IMEI 1", profile.imei, HookValidation::imei)
            className.contains("SimStatus", ignoreCase = true) ->
                validField(session, CATEGORY, "Mobile No", profile.mobileNo, HookValidation::mobile)
            else -> null
        }
    }

    private fun buildValue(profile: HookProfile, key: String): String? {
        if (!profile.buildPropEnabled) return null
        return profile.build[key]?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun logPreferenceReplacement(session: HookSession, key: String, old: String, value: String, target: String) {
        session.applied(
            CATEGORY,
            "SettingsAbout:$key:$target",
            if (old.isBlank()) "key=$key new=$value" else "key=$key old=$old new=$value"
        )
    }

    private fun logControllerReplacement(session: HookSession, className: String, value: String) {
        session.applied(
            CATEGORY,
            "SettingsAbout:${className.substringAfterLast('.')}",
            "Applied ${className.substringAfterLast('.')} summary: $value"
        )
    }
}
