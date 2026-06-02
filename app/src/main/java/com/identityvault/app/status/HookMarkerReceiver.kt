package com.identityvault.app.status

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HookMarkerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_HOOK_ACTIVE) return
        val packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        ModuleStatusRepository(context.applicationContext).setHookActive(packageName)
    }

    companion object {
        const val ACTION_HOOK_ACTIVE = "com.identityvault.app.HOOK_ACTIVE"
        const val EXTRA_PACKAGE = "package"
    }
}
