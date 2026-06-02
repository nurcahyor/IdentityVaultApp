package com.identityvault.app.detector

import android.content.Context

class DetectorViewModel(context: Context) {
    private val detector = EnvironmentDetector(context.applicationContext)
    var result: DetectorResult = detector.detect()
        private set

    fun refresh(): DetectorResult {
        result = detector.detect()
        return result
    }

    fun report(): String = detector.toReport(result)
}
