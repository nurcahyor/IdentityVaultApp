package com.identityvault.hook

import android.telephony.TelephonyManager
import de.robv.android.xposed.XC_MethodHook

object TelephonyHooks {
    fun install(session: HookSession, profile: HookProfile) {
        hookString(session, "IMEI", profile.imei, HookValidation::imei, "getDeviceId")
        hookString(session, "IMEI", profile.imei, HookValidation::imei, "getDeviceId", Int::class.javaPrimitiveType!!)
        hookString(session, "IMEI", profile.imei, HookValidation::imei, "getImei")
        hookString(session, "IMEI", profile.imei, HookValidation::imei, "getImei", Int::class.javaPrimitiveType!!)
        hookString(session, "IMEI", profile.imei, HookValidation::imei, "getMeid")
        hookString(session, "IMEI", profile.imei, HookValidation::imei, "getMeid", Int::class.javaPrimitiveType!!)
        hookString(session, "SIM Sub IDs", profile.simSubIds, HookValidation::notBlank, "getSubscriberId")
        hookString(session, "SIM Sub IDs", profile.simSubIds, HookValidation::notBlank, "getSubscriberId", Int::class.javaPrimitiveType!!)
        hookString(session, "SIM Serial ID", profile.simSerialId, HookValidation::simSerial, "getSimSerialNumber")
        hookString(session, "Mobile No", profile.mobileNo, HookValidation::mobile, "getLine1Number")
        hookString(session, "SIM Operator", profile.simOperator, HookValidation::simOperator, "getSimOperator")
        hookString(session, "SIM Operator", profile.simOperator, HookValidation::simOperator, "getNetworkOperator")
        hookOriginal(session, "SIM Operator Name", "getSimOperatorName")
        hookOriginal(session, "Network Operator Name", "getNetworkOperatorName")
        hookOriginal(session, "SIM Country ISO", "getSimCountryIso")
        hookOriginal(session, "Network Country ISO", "getNetworkCountryIso")
        hookOriginal(session, "Phone Type", "getPhoneType")
        hookOriginal(session, "Network Type", "getNetworkType")
        hookOriginal(session, "Data Network Type", "getDataNetworkType")
        hookOriginal(session, "Voice Network Type", "getVoiceNetworkType")
    }

    private fun hookString(
        session: HookSession,
        label: String,
        field: HookField,
        validator: (String) -> Boolean,
        methodName: String,
        vararg params: Class<*>
    ) {
        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = apply(param)
            override fun afterHookedMethod(param: MethodHookParam) = apply(param)
            private fun apply(param: MethodHookParam) {
                val value = validField(session, "TELEPHONY", label, field, validator) ?: return
                param.result = value
                session.applied("TELEPHONY", label, "Applied $label: $value")
            }
        }
        HookCompat.safeHookMethod(session, "TELEPHONY", TelephonyManager::class.java, methodName, *params, hook = callback)
    }

    private fun hookOriginal(session: HookSession, label: String, methodName: String) {
        HookCompat.safeHookMethod(session, "TELEPHONY", TelephonyManager::class.java, methodName, hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                session.skipped("SKIPPED", "TELEPHONY", "$label has no profile mapping, using original value", label)
            }
        })
    }
}
