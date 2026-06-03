package com.identityvault.app

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import com.identityvault.app.backup.BackupFilePicker
import com.identityvault.app.backup.BackupManager
import com.identityvault.app.backup.RestoreManager
import com.identityvault.app.detector.DetectorBottomSheet
import com.identityvault.app.status.StatusViewModel

class MainActivity : Activity(), StatusScreen.Callbacks {
    private lateinit var viewModel: StatusViewModel
    private lateinit var screen: StatusScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = StatusViewModel(this)
        screen = StatusScreen(this, viewModel, this)
        setContentView(screen.create())
    }

    override fun onExportBackup() {
        BackupFilePicker.openExport(this)
    }

    override fun onImportBackup() {
        BackupFilePicker.openImport(this)
    }

    override fun onShowMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Detector")
            menu.add("Backup Profile")
            menu.add("Restore Profile")
            menu.add("Reset Profile")
            menu.add("Log")
            menu.add("About")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Detector" -> DetectorBottomSheet(this@MainActivity).show()
                    "Backup Profile" -> onExportBackup()
                    "Restore Profile" -> onImportBackup()
                    "Reset Profile" -> screen.resetProfile()
                    "Log" -> screen.showLogs()
                    "About" -> screen.showAbout()
                }
                true
            }
        }.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data ?: return
        when (requestCode) {
            BackupFilePicker.REQUEST_EXPORT -> {
                val backup = BackupManager(this).createBackup().toString(2)
                contentResolver.openOutputStream(uri)?.use { it.write(backup.toByteArray()) }
                viewModel.logRepository.add("Backup exported")
                Toast.makeText(this, "Backup disimpan", Toast.LENGTH_SHORT).show()
            }
            BackupFilePicker.REQUEST_IMPORT -> {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val result = RestoreManager(this).restore(raw)
                viewModel.logRepository.add(result.message)
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                viewModel.refresh()
                screen.render()
            }
        }
    }
}
