package com.identityvault.app.status

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.identityvault.app.log.DetailedLogRepository

class HookMarkerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_HOOK_ACTIVE) return
        val appContext = context.applicationContext
        val logLevel = intent.getStringExtra("log_level")
        if (!logLevel.isNullOrBlank()) {
            DetailedLogRepository(appContext).add(
                level = logLevel,
                category = intent.getStringExtra("log_category").orEmpty(),
                message = intent.getStringExtra("log_message").orEmpty(),
                packageName = intent.getStringExtra("log_package").orEmpty(),
                sdkInt = intent.getIntExtra("log_sdk", 0),
                detail = intent.getStringExtra("log_detail").orEmpty()
            )
        }
        val packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val fields = intent.getStringArrayListExtra(EXTRA_FIELDS).orEmpty()
        if (packageName.isNotBlank() || fields.isNotEmpty()) {
            ModuleStatusRepository(appContext).setHookActive(
                packageName = packageName,
                appliedFields = fields,
                sdkInt = intent.getIntExtra("sdk", 0).takeIf { it > 0 },
                markerSummary = intent.getStringExtra("marker_summary")
            )
        }
    }

    companion object {
        const val ACTION_HOOK_ACTIVE = "com.identityvault.app.HOOK_ACTIVE"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_FIELDS = "fields"
    }
}
