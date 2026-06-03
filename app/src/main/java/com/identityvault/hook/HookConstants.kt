package com.identityvault.hook

import android.net.Uri

object HookConstants {
    const val MODULE_PACKAGE = "com.identityvault.app"
    const val MARKER_ACTION = "com.identityvault.app.HOOK_ACTIVE"
    const val MARKER_RECEIVER = "com.identityvault.app.status.HookMarkerReceiver"
    val PROFILE_URI: Uri = Uri.parse("content://com.identityvault.app.profile")
}
