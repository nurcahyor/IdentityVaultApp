package com.identityvault.hook

import android.telephony.TelephonyManager
import de.robv.android.xposed.XC_MethodHook

object TelephonyHooks {
    fun install(session: HookSession, profile: HookProfile) {
        session.logger.log("INFO", "TELEPHONY", "IMEI 2 will only appear if target app calls getImei(1) or getDeviceId(1).")
        hookImeiNoSlot(session, profile, "getDeviceId")
        hookImeiSlot(session, profile, "getDeviceId")
        hookImeiNoSlot(session, profile, "getImei")
        hookImeiSlot(session, profile, "getImei")
        hookMeid(session, profile, "getMeid")
        hookMeid(session, profile, "getMeid", Int::class.javaPrimitiveType!!)
        hookSubscriberNoSlot(session, profile)
        hookSubscriberSlot(session, profile)
        hookString(session, "SIM Serial ID", profile.simSerialId, HookValidation::simSerial, "getSimSerialNumber")
        hookString(session, "Mobile No", profile.mobileNo, HookValidation::mobile, "getLine1Number")
        hookString(session, "SIM Operator", profile.simOperator, HookValidation::simOperator, "getSimOperator")
        hookString(session, "Network Operator", profile.networkOperator, HookValidation::simOperator, "getNetworkOperator", fallback = profile.simOperator)
        hookString(session, "SIM Operator Name", profile.simOperatorName, HookValidation::notBlank, "getSimOperatorName")
        hookString(session, "Network Operator Name", profile.networkOperatorName, HookValidation::notBlank, "getNetworkOperatorName", fallback = profile.simOperatorName)
        hookString(session, "SIM Country ISO", profile.simCountryIso, HookValidation::countryIso, "getSimCountryIso")
        hookString(session, "Network Country ISO", profile.networkCountryIso, HookValidation::countryIso, "getNetworkCountryIso", fallback = profile.simCountryIso)
        hookInt(session, "Phone Type", profile.phoneType, HookValidation::phoneType, "getPhoneType")
        hookInt(session, "Network Type", profile.networkType, HookValidation::networkType, "getNetworkType")
        hookInt(session, "Data Network Type", profile.dataNetworkType, HookValidation::networkType, "getDataNetworkType")
        hookInt(session, "Voice Network Type", profile.voiceNetworkType, HookValidation::networkType, "getVoiceNetworkType")
    }

    private fun hookMeid(session: HookSession, profile: HookProfile, methodName: String, vararg params: Class<*>) {
        HookCompat.safeHookMethod(session, "TELEPHONY", TelephonyManager::class.java, methodName, *params, hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                if (!profile.meid.enabled) {
                    param.result = null
                    session.skipped("SKIPPED", "TELEPHONY", "Skipped MEID: disabled / no profile mapping", "MEID")
                    return
                }
                val value = profile.meid.value.trim()
                if (!HookValidation.meid(value)) {
                    param.result = null
                    session.skipped("SKIPPED", "TELEPHONY", "Skipped MEID: invalid / no profile mapping", "MEID")
                    return
                }
                param.result = value
                session.applied("TELEPHONY", "MEID", "Applied MEID: $value")
            }
        })
    }

    private fun hookSubscriberNoSlot(session: HookSession, profile: HookProfile) {
        HookCompat.safeHookMethod(session, "TELEPHONY", TelephonyManager::class.java, "getSubscriberId", hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                applySubscriber(param, session, profile.subscriberId, "Subscriber ID")
            }
        })
    }

    private fun hookSubscriberSlot(session: HookSession, profile: HookProfile) {
        HookCompat.safeHookMethod(session, "TELEPHONY", TelephonyManager::class.java, "getSubscriberId", Int::class.javaPrimitiveType!!, hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val slot = param.args.firstOrNull() as? Int ?: 0
                val field = if (slot == 1) profile.subscriberId2 else profile.subscriberId
                applySubscriber(param, session, field, if (slot == 1) "Subscriber ID 2" else "Subscriber ID")
            }
        })
    }

    private fun applySubscriber(param: XC_MethodHook.MethodHookParam, session: HookSession, field: HookField, label: String) {
        if (!field.enabled) {
            param.result = null
            session.skipped("SKIPPED", "TELEPHONY", "$label disabled, returned null safely", label)
            return
        }
        val value = field.value.trim()
        if (!HookValidation.imsi(value)) {
            param.result = null
            session.skipped("SKIPPED", "TELEPHONY", "$label invalid, returned null safely", label)
            return
        }
        param.result = value
        session.applied("TELEPHONY", label, "Applied Subscriber ID / IMSI: $value")
    }

    private fun hookImeiNoSlot(session: HookSession, profile: HookProfile, methodName: String) {
        hookString(
            session = session,
            label = "IMEI 1",
            field = profile.imei,
            validator = HookValidation::imei,
            methodName = methodName,
            applyMessage = { value -> "TelephonyManager.$methodName() applied IMEI 1: $value" }
        )
    }

    private fun hookImeiSlot(session: HookSession, profile: HookProfile, methodName: String) {
        HookCompat.safeHookMethod(session, "TELEPHONY", TelephonyManager::class.java, methodName, Int::class.javaPrimitiveType!!, hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                val slot = param.args.firstOrNull() as? Int ?: 0
                val label = if (slot == 1) "IMEI 2" else "IMEI 1"
                val field = if (slot == 1) profile.imei2 else profile.imei
                val value = validField(session, "TELEPHONY", label, field, HookValidation::imei) ?: return
                param.result = value
                session.applied("TELEPHONY", label, "TelephonyManager.$methodName($slot) applied $label: $value")
            }
        })
    }

    private fun hookString(
        session: HookSession,
        label: String,
        field: HookField,
        validator: (String) -> Boolean,
        methodName: String,
        vararg params: Class<*>,
        fallback: HookField? = null,
        applyMessage: (String) -> String = { value -> "Applied $label: $value" }
    ) {
        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                val value = validField(session, "TELEPHONY", label, field, validator)
                    ?: fallback?.let { validField(session, "TELEPHONY", label, it, validator) }
                    ?: return
                param.result = value
                session.applied("TELEPHONY", label, applyMessage(value))
            }
        }
        HookCompat.safeHookMethod(session, "TELEPHONY", TelephonyManager::class.java, methodName, *params, hook = callback)
    }

    private fun hookInt(
        session: HookSession,
        label: String,
        field: HookField,
        validator: (String) -> Boolean,
        methodName: String
    ) {
        HookCompat.safeHookMethod(session, "TELEPHONY", TelephonyManager::class.java, methodName, hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                val raw = validField(session, "TELEPHONY", label, field, validator) ?: return
                val value = raw.toIntOrNull() ?: return
                param.result = value
                session.applied("TELEPHONY", label, "Applied $label: $value")
            }
        })
    }
}
