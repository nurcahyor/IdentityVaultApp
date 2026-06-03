package com.identityvault.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.identityvault.app.backup.BackupFilePicker
import com.identityvault.app.backup.BackupManager
import com.identityvault.app.backup.RestoreManager
import com.identityvault.app.detector.DetectorBottomSheet
import com.identityvault.app.slots.IdentitySlotsScreen
import com.identityvault.app.status.StatusViewModel

class MainActivity : Activity(), StatusScreen.Callbacks {
    private lateinit var viewModel: StatusViewModel
    private lateinit var screen: StatusScreen
    private lateinit var slotsScreen: IdentitySlotsScreen
    private lateinit var container: FrameLayout
    private lateinit var identityTab: TextView
    private lateinit var slotsTab: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = StatusViewModel(this)
        screen = StatusScreen(this, viewModel, this)
        slotsScreen = IdentitySlotsScreen(this)
        setContentView(createTabbedRoot())
        showIdentityScreen()
    }

    private fun createTabbedRoot(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(13, 17, 24))
        }
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 10, 12, 8)
            setBackgroundColor(Color.rgb(13, 17, 24))
        }
        identityTab = tab("IdentityVault") { showIdentityScreen() }
        slotsTab = tab("Identity Slots") { showSlotsScreen() }
        tabs.addView(identityTab, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(0, 0, 6, 0) })
        tabs.addView(slotsTab, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(6, 0, 0, 0) })
        container = FrameLayout(this)
        root.addView(tabs)
        root.addView(container, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun tab(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 12.5f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setOnClickListener { action() }
    }

    private fun showIdentityScreen() {
        container.removeAllViews()
        container.addView(screen.create())
        styleTab(identityTab, true)
        styleTab(slotsTab, false)
    }

    private fun showSlotsScreen() {
        container.removeAllViews()
        container.addView(slotsScreen.create())
        styleTab(identityTab, false)
        styleTab(slotsTab, true)
    }

    private fun styleTab(tab: TextView, selected: Boolean) {
        tab.setTextColor(if (selected) Color.rgb(225, 231, 239) else Color.rgb(142, 153, 168))
        tab.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(if (selected) Color.rgb(25, 33, 44) else Color.TRANSPARENT)
            setStroke(1, if (selected) Color.rgb(47, 59, 73) else Color.rgb(38, 46, 58))
            cornerRadius = 7f
        }
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
        if (slotsScreen.handleActivityResult(requestCode, resultCode, data)) return
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
