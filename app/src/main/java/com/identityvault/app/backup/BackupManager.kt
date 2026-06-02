package com.identityvault.app.backup

import android.content.Context
import com.identityvault.app.data.BackupManifest
import com.identityvault.app.identity.IdentityRepository
import com.identityvault.app.log.LogRepository
import com.identityvault.app.status.ModuleStatusRepository
import org.json.JSONObject

class BackupManager(context: Context) {
    private val identityRepository = IdentityRepository(context)
    private val statusRepository = ModuleStatusRepository(context)
    private val logRepository = LogRepository(context)

    fun createBackup(): JSONObject {
        val manifest = BackupManifest(
            appId = "com.identityvault.app",
            version = 1,
            createdAt = System.currentTimeMillis(),
            entries = listOf("identity", "moduleStatus", "logs")
        )
        return JSONObject()
            .put("manifest", manifest.toJson())
            .put("identity", identityRepository.exportJson())
            .put("moduleStatus", statusRepository.exportJson())
            .put("logs", logRepository.exportJson())
    }
}
