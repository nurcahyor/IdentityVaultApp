package com.identityvault.app.identity

import com.identityvault.app.data.IdentityFieldState
import com.identityvault.app.data.IdentityProfile
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

class IdentityProfileGenerator {
    private val random = SecureRandom()
    private data class OperatorPreset(val code: String, val name: String, val countryIso: String)

    private val operatorPresets = listOf(
        OperatorPreset("51010", "Telkomsel", "id"),
        OperatorPreset("310260", "T-Mobile", "us"),
        OperatorPreset("311480", "Verizon", "us"),
        OperatorPreset("302720", "Rogers", "ca"),
        OperatorPreset("46601", "FarEasTone", "tw")
    )

    fun generate(name: String = "Generated ${System.currentTimeMillis()}"): IdentityProfile {
        val build = BuildPropPresetRepository.randomProfile()
        val operator = operatorPresets[random.nextInt(operatorPresets.size)]
        val subscriberId = generateImsi(operator.code)
        return IdentityProfile(
            name = name,
            imei = IdentityFieldState(generateImei()),
            imei2 = IdentityFieldState(generateImei()),
            meid = IdentityFieldState(generateMeid()),
            deviceName = IdentityFieldState(build.model),
            serial = IdentityFieldState(alnum(12).uppercase(Locale.US)),
            hardwareId = IdentityFieldState(hex(16)),
            macAddress = IdentityFieldState(mac()),
            macBssid = IdentityFieldState(mac()),
            macSsid = IdentityFieldState("IDV-${alnum(6).uppercase(Locale.US)}"),
            bluetoothMac = IdentityFieldState(mac()),
            bluetoothName = IdentityFieldState(build.model),
            androidId = IdentityFieldState(hex(16)),
            subscriberId = IdentityFieldState(subscriberId),
            subscriberId2 = IdentityFieldState(generateImsi(operator.code)),
            simSerialId = IdentityFieldState(generateIccid()),
            simSubIds = IdentityFieldState(subscriberId),
            mobileNo = IdentityFieldState(generateIndonesiaMobile()),
            mediaDrmId = IdentityFieldState(hex(32)),
            simOperator = IdentityFieldState(operator.code),
            networkOperator = IdentityFieldState(operator.code),
            simOperatorName = IdentityFieldState(operator.name),
            networkOperatorName = IdentityFieldState(operator.name),
            simCountryIso = IdentityFieldState(operator.countryIso),
            networkCountryIso = IdentityFieldState(operator.countryIso),
            phoneType = IdentityFieldState("1"),
            networkType = IdentityFieldState(if (build.versionSdk.toIntOrNull() ?: 0 >= 33) "20" else "13"),
            dataNetworkType = IdentityFieldState(if (build.versionSdk.toIntOrNull() ?: 0 >= 33) "20" else "13"),
            voiceNetworkType = IdentityFieldState("13"),
            gsfId = IdentityFieldState(hex(16)),
            advertisingId = IdentityFieldState(UUID.randomUUID().toString()),
            limitAdTrackingEnabled = IdentityFieldState("false"),
            googleAccountEmail = IdentityFieldState("idv${digits(6)}@gmail.com"),
            buildProp = build,
            buildPropEnabled = true,
            draft = false
        )
    }

    fun fieldValue(label: String): String {
        return when (label) {
            "IMEI", "IMEI 1", "IMEI 2" -> generateImei()
            "MEID" -> generateMeid()
            "Serial" -> alnum(12).uppercase(Locale.US)
            "Device Name" -> BuildPropPresetRepository.randomProfile().model
            "Hardware ID" -> hex(16)
            "MAC Address", "MAC BSSID", "Bluetooth MAC" -> mac()
            "Bluetooth Name" -> BuildPropPresetRepository.randomProfile().model
            "MAC SSID" -> "IDV-${alnum(6).uppercase(Locale.US)}"
            "Android ID", "GSF ID", "Google Services Framework ID" -> hex(16)
            "SIM Serial ID" -> generateIccid()
            "Subscriber ID / IMSI", "SIM Sub IDs" -> generateImsi("51010")
            "Mobile No" -> generateIndonesiaMobile()
            "MediaDrm ID" -> hex(32)
            "SIM Operator" -> "51010"
            "Network Operator" -> "51010"
            "SIM Operator Name", "Network Operator Name" -> "Telkomsel"
            "SIM Country ISO", "Network Country ISO" -> "id"
            "Phone Type" -> "1"
            "Network Type", "Data Network Type", "Voice Network Type" -> "13"
            "Advertising ID" -> UUID.randomUUID().toString()
            "Limit Ad Tracking" -> "false"
            "Google Account Email" -> "idv${digits(6)}@gmail.com"
            else -> ""
        }
    }

    fun buildProp() = BuildPropPresetRepository.randomProfile()

    private fun generateImei(): String {
        val tacs = listOf("35693803", "35988103", "35824005", "35175609")
        val tac = tacs[random.nextInt(tacs.size)]
        val body = tac + digits(6)
        return body + luhnCheckDigit(body)
    }

    private fun generateMeid(): String = hex(14).uppercase(Locale.US)

    private fun generateImsi(operator: String): String {
        val prefix = operator.filter(Char::isDigit).take(6).ifBlank { "51010" }
        return (prefix + digits(15 - prefix.length)).take(15)
    }

    private fun generateIccid(): String {
        val body = "89621010" + digits(10 + random.nextInt(2))
        return body + luhnCheckDigit(body)
    }

    private fun generateIndonesiaMobile(): String {
        val prefixes = listOf("+62811", "+62812", "+62813", "+62821", "+62822", "+62851", "+62852", "+62853")
        return prefixes[random.nextInt(prefixes.size)] + digits(7 + random.nextInt(2))
    }

    private fun mac(): String {
        val bytes = ByteArray(6)
        random.nextBytes(bytes)
        bytes[0] = ((bytes[0].toInt() or 0x02) and 0xFE).toByte()
        return bytes.joinToString(":") { "%02X".format(it.toInt() and 0xFF) }
    }

    private fun digits(count: Int): String = (1..count).joinToString("") { random.nextInt(10).toString() }

    private fun hex(count: Int): String = (1..count).joinToString("") { random.nextInt(16).toString(16) }

    private fun alnum(count: Int): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..count).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun luhnCheckDigit(number: String): Int {
        val sum = number.reversed().mapIndexed { index, c ->
            var digit = c.digitToInt()
            if (index % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            digit
        }.sum()
        return (10 - (sum % 10)) % 10
    }
}
