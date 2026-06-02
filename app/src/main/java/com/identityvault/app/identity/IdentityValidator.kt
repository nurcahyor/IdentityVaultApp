package com.identityvault.app.identity

import com.identityvault.app.data.IdentityFieldState
import com.identityvault.app.data.IdentityProfile

class IdentityValidator {
    fun validate(profile: IdentityProfile): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (profile.name.isBlank()) errors["Profile name"] = "Nama profile wajib diisi"
        check(errors, "IMEI", profile.imei) { it.length == 15 && it.all(Char::isDigit) && luhnValid(it) }
        check(errors, "Serial", profile.serial) { it.length in 6..32 && it.matches(Regex("[A-Za-z0-9._-]+")) }
        check(errors, "Hardware ID", profile.hardwareId) { it.matches(Regex("[0-9a-fA-F]{8,32}")) }
        check(errors, "MAC Address", profile.macAddress) { macValid(it) }
        check(errors, "MAC BSSID", profile.macBssid) { macValid(it) }
        check(errors, "MAC SSID", profile.macSsid) { it.length in 1..32 }
        check(errors, "Bluetooth MAC", profile.bluetoothMac) { macValid(it) }
        check(errors, "Android ID", profile.androidId) { it.matches(Regex("[0-9a-fA-F]{16}")) }
        check(errors, "SIM Serial ID", profile.simSerialId) { it.length in 19..20 && it.all(Char::isDigit) && luhnValid(it) }
        check(errors, "SIM Sub IDs", profile.simSubIds) { it.matches(Regex("[0-9, ]+")) }
        check(errors, "Mobile No", profile.mobileNo) { it.matches(Regex("\\+628[0-9]{8,11}")) }
        check(errors, "MediaDrm ID", profile.mediaDrmId) { it.matches(Regex("[0-9a-fA-F]{16,64}")) }
        check(errors, "SIM Operator", profile.simOperator) { it.matches(Regex("[0-9]{5,6}")) }
        check(errors, "GSF ID", profile.gsfId) { it.matches(Regex("[0-9a-fA-F]{16}")) }

        if (profile.buildPropEnabled) {
            val b = profile.buildProp
            val fpPattern = Regex("[^/]+/[^/]+/[^:]+:[0-9.]+/[^/]+/[^:]+:[^/]+/[^/]+")
            if (!b.fingerprint.matches(fpPattern)) errors["Build fingerprint"] = "Format fingerprint tidak valid"
            if (!b.fingerprint.startsWith("${b.brand}/${b.name}/${b.device}:")) {
                errors["Build fingerprint consistency"] = "brand/product/device tidak konsisten dengan fingerprint"
            }
            if (b.versionSdk.toIntOrNull() !in 28..33) errors["SDK"] = "SDK harus Android 9-13 (28-33)"
            if (b.versionRelease.isBlank() || b.securityPatch.isBlank()) errors["Build Prop"] = "Versi dan security patch wajib diisi"
        }
        return errors
    }

    private fun check(errors: MutableMap<String, String>, title: String, field: IdentityFieldState, valid: (String) -> Boolean) {
        if (field.enabled && !valid(field.value)) errors[title] = "Nilai tidak valid: ${field.value}"
    }

    private fun macValid(value: String): Boolean = value.matches(Regex("(?i)^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))

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
