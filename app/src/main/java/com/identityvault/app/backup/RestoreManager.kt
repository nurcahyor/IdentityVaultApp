package com.identityvault.app.backup

import android.content.Context
import com.identityvault.app.data.OperationResult
import com.identityvault.app.identity.IdentityRepository
import com.identityvault.app.log.LogRepository
import com.identityvault.app.status.ModuleStatusRepository
import org.json.JSONObject

class RestoreManager(context: Context) {
    private val identityRepository = IdentityRepository(context)
    private val statusRepository = ModuleStatusRepository(context)
    private val logRepository = LogRepository(context)

    fun restore(raw: String): OperationResult {
        return try {
            val json = JSONObject(raw)
            val manifest = json.optJSONObject("manifest") ?: return OperationResult(false, "Backup manifest tidak ditemukan")
            if (manifest.optString("appId") != "com.identityvault.app") return OperationResult(false, "Backup bukan milik IdentityVault")
            json.optJSONObject("identity")?.let { identityRepository.importJson(it) }
            json.optJSONObject("moduleStatus")?.let { statusRepository.importJson(it) }
            json.optJSONArray("logs")?.let { logRepository.importJson(it) }
            OperationResult(true, "Restore selesai")
        } catch (e: Exception) {
            OperationResult(false, "Restore gagal: ${e.message}")
        }
    }
}
