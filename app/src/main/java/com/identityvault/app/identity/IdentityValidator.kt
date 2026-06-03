package com.identityvault.app.identity

import com.identityvault.app.data.IdentityFieldState
import com.identityvault.app.data.IdentityProfile

class IdentityValidator {
    fun validate(profile: IdentityProfile): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (profile.name.isBlank()) errors["Profile name"] = "Nama profile wajib diisi"
        check(errors, "IMEI 1", profile.imei) { it.length == 15 && it.all(Char::isDigit) && luhnValid(it) }
        check(errors, "IMEI 2", profile.imei2) { it.length == 15 && it.all(Char::isDigit) && luhnValid(it) }
        check(errors, "MEID", profile.meid) { it.matches(Regex("[0-9A-Fa-f]{14}")) }
        check(errors, "Device Name", profile.deviceName) { it.isNotBlank() }
        check(errors, "Serial", profile.serial) { it.length in 6..32 && it.matches(Regex("[A-Za-z0-9._-]+")) }
        check(errors, "Hardware ID", profile.hardwareId) { it.matches(Regex("[0-9a-fA-F]{8,32}")) }
        check(errors, "MAC Address", profile.macAddress) { macValid(it) }
        check(errors, "MAC BSSID", profile.macBssid) { macValid(it) }
        check(errors, "MAC SSID", profile.macSsid) { it.length in 1..32 }
        check(errors, "Bluetooth MAC", profile.bluetoothMac) { macValid(it) }
        check(errors, "Bluetooth Name", profile.bluetoothName) { bluetoothNameValid(it) }
        check(errors, "Android ID", profile.androidId) { it.matches(Regex("[0-9a-fA-F]{16}")) }
        check(errors, "Subscriber ID / IMSI", profile.subscriberId) { subscriberValid(it, profile.simOperator.value) }
        check(errors, "Subscriber ID 2 / IMSI", profile.subscriberId2) { subscriberValid(it, profile.simOperator.value) }
        check(errors, "SIM Serial ID", profile.simSerialId) { it.length in 19..20 && it.all(Char::isDigit) && luhnValid(it) }
        check(errors, "SIM Sub IDs", profile.simSubIds) { subscriberValid(it, profile.simOperator.value) }
        check(errors, "Mobile No", profile.mobileNo) { it.matches(Regex("\\+628[0-9]{8,11}")) }
        check(errors, "MediaDrm ID", profile.mediaDrmId) { it.matches(Regex("[0-9a-fA-F]{16,64}")) }
        check(errors, "SIM Operator", profile.simOperator) { it.matches(Regex("[0-9]{5,6}")) }
        check(errors, "Network Operator", profile.networkOperator) { it.matches(Regex("[0-9]{5,6}")) }
        check(errors, "SIM Operator Name", profile.simOperatorName) { it.isNotBlank() }
        check(errors, "Network Operator Name", profile.networkOperatorName) { it.isNotBlank() }
        check(errors, "SIM Country ISO", profile.simCountryIso) { it.matches(Regex("[a-z]{2}")) }
        check(errors, "Network Country ISO", profile.networkCountryIso) { it.matches(Regex("[a-z]{2}")) }
        check(errors, "Phone Type", profile.phoneType) { intValid(it, setOf(0, 1, 2)) }
        check(errors, "Network Type", profile.networkType) { intValid(it, setOf(0, 1, 2, 3, 8, 9, 10, 13, 15, 20)) }
        check(errors, "Data Network Type", profile.dataNetworkType) { intValid(it, setOf(0, 1, 2, 3, 8, 9, 10, 13, 15, 20)) }
        check(errors, "Voice Network Type", profile.voiceNetworkType) { intValid(it, setOf(0, 1, 2, 3, 8, 9, 10, 13, 15, 20)) }
        check(errors, "GSF ID", profile.gsfId) { it.matches(Regex("[0-9a-fA-F]{16}")) }
        check(errors, "Advertising ID", profile.advertisingId) { uuidValid(it) }
        check(errors, "Limit Ad Tracking", profile.limitAdTrackingEnabled) { it.equals("true", true) || it.equals("false", true) }
        check(errors, "Google Account Email", profile.googleAccountEmail) { emailValid(it) }

        if (profile.buildPropEnabled) {
            val b = profile.buildProp
            val fpPattern = Regex("[^/]+/[^/]+/[^:]+:[0-9.]+/[^/]+/[^:]+:[^/]+/[^/]+")
            if (!b.fingerprint.matches(fpPattern)) errors["Build fingerprint"] = "Format fingerprint tidak valid"
            if (!b.fingerprint.startsWith("${b.brand}/${b.name}/${b.device}:")) {
                errors["Build fingerprint consistency"] = "brand/product/device tidak konsisten dengan fingerprint"
            }
            if (b.versionSdk.toIntOrNull() !in 28..34) errors["SDK"] = "SDK harus Android 9-14 (28-34)"
            if (b.versionRelease.isBlank() || b.securityPatch.isBlank()) errors["Build Prop"] = "Versi dan security patch wajib diisi"
        }
        return errors
    }

    private fun check(errors: MutableMap<String, String>, title: String, field: IdentityFieldState, valid: (String) -> Boolean) {
        if (field.enabled && !valid(field.value)) errors[title] = "Nilai tidak valid: ${field.value}"
    }

    private fun macValid(value: String): Boolean = value.matches(Regex("(?i)^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))
    private fun uuidValid(value: String): Boolean = value.matches(Regex("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
    private fun emailValid(value: String): Boolean = value.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    private fun bluetoothNameValid(value: String): Boolean = value.trim().length in 1..64 && value.none { it.isISOControl() }
    private fun subscriberValid(value: String, operator: String): Boolean {
        val cleanOperator = operator.filter(Char::isDigit)
        return value.matches(Regex("[0-9]{14,16}")) && (cleanOperator.isBlank() || value.startsWith(cleanOperator))
    }
    private fun intValid(value: String, allowed: Set<Int>): Boolean = value.toIntOrNull() in allowed

    private fun luhnValid(number: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in number.length - 1 downTo 0) {
            var n = number[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}
