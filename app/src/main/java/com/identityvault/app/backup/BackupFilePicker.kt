package com.identityvault.app.backup

import android.app.Activity
import android.content.Intent

object BackupFilePicker {
    const val REQUEST_EXPORT = 701
    const val REQUEST_IMPORT = 702

    fun openExport(activity: Activity) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
            .putExtra(Intent.EXTRA_TITLE, "identityvault-backup.json")
        activity.startActivityForResult(intent, REQUEST_EXPORT)
    }

    fun openImport(activity: Activity) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
        activity.startActivityForResult(intent, REQUEST_IMPORT)
    }
}
