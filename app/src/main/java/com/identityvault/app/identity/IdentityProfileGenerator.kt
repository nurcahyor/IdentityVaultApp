package com.identityvault.app.identity

import com.identityvault.app.data.BuildPropProfile
import com.identityvault.app.data.IdentityFieldState
import com.identityvault.app.data.IdentityProfile
import java.security.SecureRandom
import java.util.Locale

class IdentityProfileGenerator {
    private val random = SecureRandom()

    private val buildPresets = listOf(
        BuildPropProfile(
            fingerprint = "google/panther/panther:13/TQ3A.230805.001/10316531:user/release-keys",
            brand = "google",
            model = "Pixel 7",
            manufacturer = "Google",
            device = "panther",
            name = "panther",
            board = "panther",
            hardware = "panther",
            buildId = "TQ3A.230805.001",
            displayId = "TQ3A.230805.001",
            versionRelease = "13",
            versionSdk = "33",
            securityPatch = "2023-08-05"
        ),
        BuildPropProfile(
            fingerprint = "samsung/a52sxq/a52sxq:12/SP1A.210812.016/A528BXXS1DVL2:user/release-keys",
            brand = "samsung",
            model = "SM-A528B",
            manufacturer = "samsung",
            device = "a52sxq",
            name = "a52sxq",
            board = "lahaina",
            hardware = "qcom",
            buildId = "SP1A.210812.016",
            displayId = "SP1A.210812.016.A528BXXS1DVL2",
            versionRelease = "12",
            versionSdk = "31",
            securityPatch = "2022-12-01"
        ),
        BuildPropProfile(
            fingerprint = "Xiaomi/vayu/vayu:11/RKQ1.200826.002/V12.5.7.0.RJUMIXM:user/release-keys",
            brand = "Xiaomi",
            model = "M2102J20SG",
            manufacturer = "Xiaomi",
            device = "vayu",
            name = "vayu",
            board = "vayu",
            hardware = "qcom",
            buildId = "RKQ1.200826.002",
            displayId = "RKQ1.200826.002",
            versionRelease = "11",
            versionSdk = "30",
            securityPatch = "2021-12-01"
        )
    )

    fun generate(name: String = "Generated ${System.currentTimeMillis()}"): IdentityProfile {
        val build = buildPresets[random.nextInt(buildPresets.size)]
        return IdentityProfile(
            name = name,
            imei = IdentityFieldState(generateImei()),
            serial = IdentityFieldState(alnum(12).uppercase(Locale.US)),
            hardwareId = IdentityFieldState(hex(16)),
            macAddress = IdentityFieldState(mac()),
            macBssid = IdentityFieldState(mac()),
            macSsid = IdentityFieldState("IDV-${alnum(6).uppercase(Locale.US)}"),
            bluetoothMac = IdentityFieldState(mac()),
            androidId = IdentityFieldState(hex(16)),
            simSerialId = IdentityFieldState(generateIccid()),
            simSubIds = IdentityFieldState("${random.nextInt(8) + 1}"),
            mobileNo = IdentityFieldState(generateIndonesiaMobile()),
            mediaDrmId = IdentityFieldState(hex(32)),
            simOperator = IdentityFieldState("51010"),
            gsfId = IdentityFieldState(hex(16)),
            buildProp = build,
            buildPropEnabled = true,
            draft = false
        )
    }

    fun fieldValue(label: String): String {
        return when (label) {
            "IMEI" -> generateImei()
            "Serial" -> alnum(12).uppercase(Locale.US)
            "Hardware ID" -> hex(16)
            "MAC Address", "MAC BSSID", "Bluetooth MAC" -> mac()
            "MAC SSID" -> "IDV-${alnum(6).uppercase(Locale.US)}"
            "Android ID", "GSF ID" -> hex(16)
            "SIM Serial ID" -> generateIccid()
            "SIM Sub IDs" -> "${random.nextInt(8) + 1}"
            "Mobile No" -> generateIndonesiaMobile()
            "MediaDrm ID" -> hex(32)
            "SIM Operator" -> "51010"
            else -> ""
        }
    }

    fun buildProp(): BuildPropProfile = buildPresets[random.nextInt(buildPresets.size)]

    private fun generateImei(): String {
        val tacs = listOf("35693803", "35988103", "35824005", "35175609")
        val tac = tacs[random.nextInt(tacs.size)]
        val body = tac + digits(6)
        return body + luhnCheckDigit(body)
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
