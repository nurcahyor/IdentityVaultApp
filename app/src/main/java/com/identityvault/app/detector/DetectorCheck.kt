package com.identityvault.app.detector

data class DetectorCheck(
    val id: String,
    val title: String,
    val detail: String,
    val status: DetectorStatus
)

enum class DetectorStatus {
    CLEAN,
    DETECTED,
    WARNING,
    UNKNOWN
}

data class DetectorResult(
    val checks: List<DetectorCheck>,
    val redCount: Int,
    val warningCount: Int,
    val summary: String
)
