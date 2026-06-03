package com.identityvault.app.identity

import com.identityvault.app.data.BuildPropProfile

data class BuildPropPreset(
    val id: String,
    val family: String,
    val androidVersionLabel: String,
    val label: String,
    val modelLabel: String,
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val hardware: String,
    val androidRelease: String,
    val sdk: Int,
    val securityPatch: String,
    val buildId: String,
    val buildDisplay: String,
    val fingerprint: String,
    val type: String = "user",
    val tags: String = "release-keys"
) {
    fun toProfile(): BuildPropProfile = BuildPropProfile(
        fingerprint = fingerprint,
        brand = brand,
        model = model,
        manufacturer = manufacturer,
        device = device,
        name = product,
        board = board,
        hardware = hardware,
        buildId = buildId,
        displayId = buildDisplay,
        versionRelease = androidRelease,
        versionSdk = sdk.toString(),
        securityPatch = securityPatch
    )
}

object BuildPropPresetRepository {
    val presets: List<BuildPropPreset> = listOf(
        preset("Google Pixel / Android 9", "google", "Google", "Pixel 3", "blueline", "blueline", "sdm845", "qcom", "9", 28, "2019-09-05", "PQ3A.190801.002", "PQ3A.190801.002", "google/blueline/blueline:9/PQ3A.190801.002/5670241:user/release-keys"),
        preset("Google Pixel / Android 10", "google", "Google", "Pixel 4", "flame", "flame", "flame", "qcom", "10", 29, "2020-12-05", "QQ3A.200805.001", "QQ3A.200805.001", "google/flame/flame:10/QQ3A.200805.001/6578210:user/release-keys"),
        preset("Google Pixel / Android 11", "google", "Google", "Pixel 5", "redfin", "redfin", "redfin", "redfin", "11", 30, "2021-10-05", "RQ3A.211001.001", "RQ3A.211001.001", "google/redfin/redfin:11/RQ3A.211001.001/7641976:user/release-keys"),
        preset("Google Pixel / Android 12", "google", "Google", "Pixel 6", "oriole", "oriole", "oriole", "oriole", "12", 31, "2022-10-05", "SP1A.210812.016", "SP1A.210812.016.A1", "google/oriole/oriole:12/SP1A.210812.016/7917056:user/release-keys"),
        preset("Google Pixel / Android 13", "google", "Google", "Pixel 7", "panther", "panther", "panther", "panther", "13", 33, "2023-08-05", "TQ3A.230805.001", "TQ3A.230805.001", "google/panther/panther:13/TQ3A.230805.001/10316531:user/release-keys"),
        preset("Google Pixel / Android 14", "google", "Google", "Pixel 8", "shiba", "shiba", "shiba", "shiba", "14", 34, "2024-06-05", "AP2A.240605.024", "AP2A.240605.024", "google/shiba/shiba:14/AP2A.240605.024/11860263:user/release-keys"),
        preset("Google Pixel / Android 15", "google", "Google", "Pixel 9", "tokay", "tokay", "tokay", "tokay", "15", 35, "2025-05-05", "BP1A.250505.005", "BP1A.250505.005", "google/tokay/tokay:15/BP1A.250505.005/12345678:user/release-keys"),
        preset("Samsung Galaxy / Android 9", "samsung", "samsung", "SM-G973F", "beyond1", "beyond1lte", "exynos9820", "exynos9820", "9", 28, "2019-12-01", "PPR1.180610.011", "PPR1.180610.011.G973FXXS3BSL4", "samsung/beyond1lte/beyond1:9/PPR1.180610.011/G973FXXS3BSL4:user/release-keys"),
        preset("Samsung Galaxy / Android 10", "samsung", "samsung", "SM-G981B", "x1s", "x1sxxx", "kona", "qcom", "10", 29, "2020-12-01", "QP1A.190711.020", "QP1A.190711.020.G981BXXU5CTL5", "samsung/x1sxxx/x1s:10/QP1A.190711.020/G981BXXU5CTL5:user/release-keys"),
        preset("Samsung Galaxy / Android 11", "samsung", "samsung", "SM-G991B", "o1s", "o1sxxx", "lahaina", "qcom", "11", 30, "2021-12-01", "RP1A.200720.012", "RP1A.200720.012.G991BXXS3BUL1", "samsung/o1sxxx/o1s:11/RP1A.200720.012/G991BXXS3BUL1:user/release-keys"),
        preset("Samsung Galaxy A52s / Android 12", "samsung", "samsung", "SM-A528B", "a52sxq", "a52sxq", "lahaina", "qcom", "12", 31, "2022-12-01", "SP1A.210812.016", "SP1A.210812.016.A528BXXS1DVL2", "samsung/a52sxq/a52sxq:12/SP1A.210812.016/A528BXXS1DVL2:user/release-keys"),
        preset("Samsung Galaxy / Android 13", "samsung", "samsung", "SM-S911B", "dm1q", "dm1qxxx", "kalama", "qcom", "13", 33, "2023-11-01", "TP1A.220624.014", "TP1A.220624.014.S911BXXS3AWK1", "samsung/dm1qxxx/dm1q:13/TP1A.220624.014/S911BXXS3AWK1:user/release-keys"),
        preset("Xiaomi / Android 10", "Xiaomi", "Xiaomi", "M2007J3SG", "apollo", "apollo", "apollo", "qcom", "10", 29, "2021-05-01", "QKQ1.200419.002", "QKQ1.200419.002", "Xiaomi/apollo/apollo:10/QKQ1.200419.002/V12.0.6.0.QJDMIXM:user/release-keys"),
        preset("Xiaomi / Android 11", "Xiaomi", "Xiaomi", "M2102J20SG", "vayu", "vayu", "vayu", "qcom", "11", 30, "2021-12-01", "RKQ1.200826.002", "RKQ1.200826.002", "Xiaomi/vayu/vayu:11/RKQ1.200826.002/V12.5.7.0.RJUMIXM:user/release-keys"),
        preset("Poco / Android 10", "POCO", "Xiaomi", "M2007J20CG", "surya", "surya", "surya", "qcom", "10", 29, "2021-02-01", "QKQ1.200512.002", "QKQ1.200512.002", "POCO/surya_global/surya:10/QKQ1.200512.002/V12.0.8.0.QJGMIXM:user/release-keys"),
        preset("Poco / Android 11", "POCO", "Xiaomi", "M2102J20SG", "vayu", "vayu", "vayu", "qcom", "11", 30, "2022-01-01", "RKQ1.200826.002", "RKQ1.200826.002", "POCO/vayu_global/vayu:11/RKQ1.200826.002/V13.0.1.0.RJUMIXM:user/release-keys"),
        preset("Poco / Android 12", "POCO", "Xiaomi", "2201116PG", "veux", "veux", "veux", "qcom", "12", 31, "2022-11-01", "SKQ1.211006.001", "SKQ1.211006.001", "POCO/veux_global/veux:12/SKQ1.211006.001/V13.0.8.0.SKCMIXM:user/release-keys"),
        preset("OnePlus / Android 11", "OnePlus", "OnePlus", "LE2113", "OnePlus9", "OnePlus9", "lahaina", "qcom", "11", 30, "2021-12-01", "RKQ1.201105.002", "RKQ1.201105.002", "OnePlus/OnePlus9_EEA/OnePlus9:11/RKQ1.201105.002/211111:user/release-keys"),
        preset("OnePlus / Android 12", "OnePlus", "OnePlus", "NE2213", "OP516FL1", "OP516FL1", "taro", "qcom", "12", 31, "2022-10-01", "SKQ1.211113.001", "SKQ1.211113.001", "OnePlus/OP516FL1/OP516FL1:12/SKQ1.211113.001/R.202210:user/release-keys"),
        preset("Oppo / Android 12", "OPPO", "OPPO", "CPH2219", "OP4F0F", "OP4F0F", "kona", "qcom", "12", 31, "2022-08-05", "SKQ1.210216.001", "SKQ1.210216.001", "OPPO/CPH2219/OP4F0F:12/SKQ1.210216.001/1660123456789:user/release-keys"),
        preset("Vivo / Android 12", "vivo", "vivo", "V2145", "V2145", "V2145", "lahaina", "qcom", "12", 31, "2022-09-01", "SP1A.210812.003", "SP1A.210812.003", "vivo/PD2145F_EX/PD2145F:12/SP1A.210812.003/compiler0901:user/release-keys"),
        preset("Realme / Android 12", "realme", "realme", "RMX3363", "RE54BFL1", "RE54BFL1", "lahaina", "qcom", "12", 31, "2022-10-05", "SKQ1.210216.001", "SKQ1.210216.001", "realme/RMX3363/RE54BFL1:12/SKQ1.210216.001/1665012345678:user/release-keys")
    )

    fun profiles(): List<BuildPropProfile> = presets.map { it.toProfile() }

    fun randomProfile(): BuildPropProfile = profiles().random()

    fun findByFingerprint(fingerprint: String): BuildPropProfile? =
        presets.firstOrNull { it.fingerprint.equals(fingerprint.trim(), ignoreCase = true) }?.toProfile()

    val grouped: Map<String, Map<String, List<BuildPropPreset>>> = presets
        .groupBy { it.family }
        .mapValues { (_, familyPresets) -> familyPresets.groupBy { it.androidVersionLabel } }

    private fun preset(
        label: String,
        brand: String,
        manufacturer: String,
        model: String,
        device: String,
        product: String,
        board: String,
        hardware: String,
        androidRelease: String,
        sdk: Int,
        securityPatch: String,
        buildId: String,
        buildDisplay: String,
        fingerprint: String
    ): BuildPropPreset = BuildPropPreset(
        id = label.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
        family = familyFrom(label, brand),
        androidVersionLabel = "Android $androidRelease",
        label = label,
        modelLabel = modelLabel(label, model, device),
        brand = brand,
        manufacturer = manufacturer,
        model = model,
        device = device,
        product = product,
        board = board,
        hardware = hardware,
        androidRelease = androidRelease,
        sdk = sdk,
        securityPatch = securityPatch,
        buildId = buildId,
        buildDisplay = buildDisplay,
        fingerprint = fingerprint
    )

    private fun familyFrom(label: String, brand: String): String {
        val raw = label.substringBefore(" / ")
        return when {
            raw.startsWith("Google Pixel") -> "Google Pixel"
            raw.startsWith("Samsung Galaxy") -> "Samsung Galaxy"
            raw.startsWith("Xiaomi") || brand.equals("Xiaomi", true) -> "Xiaomi"
            raw.startsWith("Poco") || brand.equals("POCO", true) -> "Poco"
            raw.startsWith("OnePlus") -> "OnePlus"
            raw.startsWith("Oppo") || brand.equals("OPPO", true) -> "Oppo"
            raw.startsWith("Vivo") || brand.equals("vivo", true) -> "Vivo"
            raw.startsWith("Realme") || brand.equals("realme", true) -> "Realme"
            else -> raw
        }
    }

    private fun modelLabel(label: String, model: String, device: String): String {
        val name = label.substringBefore(" / ")
            .removePrefix("Google ")
            .removePrefix("Samsung ")
        return "$name / ${model.ifBlank { device }}"
    }
}
