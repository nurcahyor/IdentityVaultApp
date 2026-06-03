package com.identityvault.app.slots

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.identityvault.app.log.DetailedLogRepository
import java.util.Date

class IdentitySlotsScreen(private val context: Context) {
    private val activity = context as Activity
    private val repository = IdentitySlotRepository(context)
    private val logger = DetailedLogRepository(context)
    private val bg = Color.rgb(13, 17, 24)
    private val panelBg = Color.rgb(19, 24, 32)
    private val softBg = Color.rgb(25, 33, 44)
    private val line = Color.rgb(38, 46, 58)
    private val textColor = Color.rgb(225, 231, 239)
    private val muted = Color.rgb(142, 153, 168)
    private val accent = Color.rgb(69, 178, 166)
    private val warning = Color.rgb(220, 174, 90)
    private lateinit var list: LinearLayout
    private var pending: PendingOperation? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var installedAppsCache: List<IdentityGroupApp>? = null
    private var appsLoading = false
    private val appLoadCallbacks = mutableListOf<(List<IdentityGroupApp>) -> Unit>()
    private val iconCache = mutableMapOf<String, Drawable?>()

    fun create(): View {
        val root = FrameLayout(context).apply { setBackgroundColor(bg) }
        val scroll = ScrollView(context).apply { setBackgroundColor(bg) }
        list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 14, 14, 86)
            setBackgroundColor(bg)
        }
        scroll.addView(list)
        root.addView(scroll)
        root.addView(fab(), FrameLayout.LayoutParams(54, 54, Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, 20, 20) })
        loadInstalledAppsAsync {}
        render()
        return root
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode !in REQUEST_CODES) return false
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            pending = null
            return true
        }
        val uri = data.data ?: return true
        when (requestCode) {
            REQUEST_EXPORT_GROUP -> exportGroupToUri(uri)
            REQUEST_EXPORT_APP -> exportAppToUri(uri)
            REQUEST_IMPORT_GROUP -> importGroupFromUri(uri)
            REQUEST_IMPORT_APP -> importAppFromUri(uri)
        }
        return true
    }

    private fun render() {
        list.removeAllViews()
        list.addView(TextView(context).apply {
            text = "Identity Slots"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        list.addView(TextView(context).apply {
            text = "Identity groups and per-app vault export/import."
            textSize = 12f
            setTextColor(muted)
            setPadding(0, 3, 0, 12)
        })
        repository.getGroups().forEach { list.addView(groupCard(it)) }
    }

    private fun groupCard(group: IdentityGroup): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 11)
            background = rounded(panelBg, 1, line, 8f)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 12)
            }
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(context).apply {
            text = if (group.collapsed) ">" else "v"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(accent)
            setOnClickListener {
                repository.updateGroup(group.copy(collapsed = !group.collapsed))
                render()
            }
        }, LinearLayout.LayoutParams(32, 36))
        header.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = group.name
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textColor)
            })
            addView(TextView(context).apply {
                text = "${group.apps.size} apps"
                textSize = 11.5f
                setTextColor(muted)
            })
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(iconText("+").apply { setOnClickListener { showManageApps(group) } }, LinearLayout.LayoutParams(40, 38))
        header.addView(iconText("\u22ee").apply { setOnClickListener { showGroupMenu(group, this) } }, LinearLayout.LayoutParams(36, 38))
        card.addView(header)
        if (!group.collapsed) {
            if (group.apps.isEmpty()) {
                card.addView(TextView(context).apply {
                    text = "No apps selected. Use + or Manage Apps."
                    textSize = 12f
                    setTextColor(muted)
                    setPadding(36, 12, 0, 4)
                })
            } else {
                card.addView(appGrid(group))
            }
        }
        return card
    }

    private fun appGrid(group: IdentityGroup): View {
        val grid = GridLayout(context).apply {
            columnCount = 2
            setPadding(34, 10, 0, 2)
        }
        group.apps.forEach { app ->
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(0, 0, 8, 8)
            }
            grid.addView(appTile(group, app), params)
        }
        return grid
    }

    private fun appTile(group: IdentityGroup, app: IdentityGroupApp): View {
        val tile = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 5, 8)
            background = rounded(Color.rgb(15, 20, 28), 1, Color.rgb(32, 40, 51), 7f)
            minimumHeight = 70
        }
        tile.addView(appIcon(app.packageName), LinearLayout.LayoutParams(34, 34).apply { setMargins(0, 0, 8, 0) })
        tile.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = app.appLabel
                textSize = 12.4f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textColor)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            addView(TextView(context).apply {
                text = app.packageName
                textSize = 10.3f
                setTextColor(Color.rgb(164, 174, 188))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            val time = timestamp(app)
            if (time.isNotBlank()) {
                addView(TextView(context).apply {
                    text = time
                    textSize = 10.1f
                    setTextColor(Color.rgb(174, 184, 198))
                    maxLines = 1
                })
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        tile.addView(iconText("\u22ee").apply { setOnClickListener { showAppMenu(group, app, this) } }, LinearLayout.LayoutParams(28, 36))
        return tile
    }

    private fun showGroupMenu(group: IdentityGroup, anchor: View) {
        PopupMenu(context, anchor).apply {
            menu.add("Manage Apps")
            menu.add("Rename Group")
            menu.add("Backup Group")
            menu.add("Restore Group")
            menu.add("Clear Group")
            menu.add("Delete Group")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Manage Apps" -> showManageApps(group)
                    "Rename Group" -> renameGroup(group)
                    "Backup Group" -> requestExportGroup(group)
                    "Restore Group" -> requestImportGroup(group)
                    "Clear Group" -> confirm("Clear ${group.name}?", "Remove all apps from this group. Identity exports on files are not deleted.") {
                        repository.clearGroup(group.id)
                        render()
                    }
                    "Delete Group" -> confirm("Delete ${group.name}?", "This removes only the IdentityVault group.") {
                        repository.deleteGroup(group.id)
                        render()
                    }
                }
                true
            }
        }.show()
    }

    private fun showAppMenu(group: IdentityGroup, app: IdentityGroupApp, anchor: View) {
        PopupMenu(context, anchor).apply {
            menu.add("Backup / Export Identity")
            menu.add("Restore / Import Identity")
            menu.add("Manage Slots")
            menu.add("Remove from Group")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Backup / Export Identity" -> requestExportApp(group, app)
                    "Restore / Import Identity" -> requestImportApp(group, app)
                    "Manage Slots" -> showReport(repository.assignCurrentProfileToApp(app.packageName))
                    "Remove from Group" -> {
                        repository.removeApp(group.id, app.packageName)
                        render()
                    }
                }
                true
            }
        }.show()
    }

    private fun showManageApps(group: IdentityGroup) {
        var includeSystem = group.showSystemApps
        val selected = group.apps.map { it.packageName }.toMutableSet()
        val dialog = Dialog(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(panelBg)
        }
        root.addView(TextView(context).apply {
            text = "Manage Apps & Add Apps"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            setPadding(22, 20, 22, 12)
            setBackgroundColor(panelBg)
        })
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 0, 22, 0)
            setBackgroundColor(panelBg)
        }
        val search = EditText(context).apply {
            hint = "Search apps"
            textSize = 13f
            setSingleLine(true)
            setTextColor(textColor)
            setHintTextColor(muted)
            backgroundTintList = android.content.res.ColorStateList.valueOf(line)
            setPadding(0, 8, 0, 10)
        }
        val filterRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val userButton = dialogButton("User Apps")
        val systemButton = dialogButton("System Apps")
        filterRow.addView(userButton, LinearLayout.LayoutParams(0, 46, 1f).apply { setMargins(0, 12, 6, 12) })
        filterRow.addView(systemButton, LinearLayout.LayoutParams(0, 46, 1f).apply { setMargins(6, 12, 0, 12) })
        val loadingText = TextView(context).apply {
            text = "Loading apps..."
            textSize = 12.5f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        val listView = ListView(context).apply {
            divider = null
            cacheColorHint = Color.TRANSPARENT
            setBackgroundColor(panelBg)
            setPadding(0, 6, 0, 10)
            clipToPadding = false
            isFastScrollEnabled = false
        }
        container.addView(search)
        container.addView(filterRow)
        container.addView(FrameLayout(context).apply {
            setBackgroundColor(panelBg)
            addView(listView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(loadingText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 420))
        root.addView(container)
        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setPadding(18, 12, 18, 16)
            setBackgroundColor(panelBg)
        }
        val cancelButton = flatTextButton("Cancel")
        val saveButton = flatTextButton("Save").apply { isEnabled = installedAppsCache != null }
        footer.addView(cancelButton, LinearLayout.LayoutParams(104, 42).apply { setMargins(0, 0, 8, 0) })
        footer.addView(saveButton, LinearLayout.LayoutParams(96, 42))
        root.addView(footer)
        var adapter: AppPickerAdapter? = null
        var loadedApps: List<IdentityGroupApp> = emptyList()
        fun updateFilterButtons() {
            styleFilterButton(userButton, !includeSystem)
            styleFilterButton(systemButton, includeSystem)
        }
        fun redraw() {
            updateFilterButtons()
            adapter?.updateFilter(includeSystem, search.text.toString())
        }
        userButton.setOnClickListener {
            includeSystem = false
            redraw()
        }
        systemButton.setOnClickListener {
            includeSystem = true
            redraw()
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = redraw()
        })
        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            val chosen = loadedApps.filter { it.packageName in selected }
            repository.setGroupApps(group.id, chosen)
            dialog.dismiss()
            render()
        }
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((context.resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        updateFilterButtons()
        loadInstalledAppsAsync { apps ->
            if (!dialog.isShowing) return@loadInstalledAppsAsync
            loadedApps = apps
            loadingText.visibility = View.GONE
            adapter = AppPickerAdapter(apps, selected, includeSystem, search.text.toString())
            listView.adapter = adapter
            saveButton.isEnabled = true
        }
    }

    private fun renameGroup(group: IdentityGroup) {
        val input = EditText(context).apply {
            setText(group.name)
            setSingleLine(true)
        }
        AlertDialog.Builder(context)
            .setTitle("Rename Group")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) repository.updateGroup(group.copy(name = name))
                render()
            }
            .show()
    }

    private fun requestExportGroup(group: IdentityGroup) {
        pending = PendingOperation.ExportGroup(group.id, group.name)
        createJson("identityvault-${safeName(group.name)}-group.json", REQUEST_EXPORT_GROUP)
    }

    private fun requestExportApp(group: IdentityGroup, app: IdentityGroupApp) {
        pending = PendingOperation.ExportApp(group.id, app.packageName, app.appLabel)
        createJson("identityvault-${safeName(app.appLabel)}.json", REQUEST_EXPORT_APP)
    }

    private fun requestImportGroup(group: IdentityGroup) {
        pending = PendingOperation.ImportGroup(group.id, group.name)
        openJson(REQUEST_IMPORT_GROUP)
    }

    private fun requestImportApp(group: IdentityGroup, app: IdentityGroupApp) {
        pending = PendingOperation.ImportApp(group.id, app.packageName, app.appLabel)
        openJson(REQUEST_IMPORT_APP)
    }

    private fun exportGroupToUri(uri: Uri) {
        val op = pending as? PendingOperation.ExportGroup ?: return
        val progress = progressDialog("Backup Group", listOf("Preparing export", "Capturing app identities", "Writing JSON", "Verifying export"))
        progress.show()
        progress.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(panelBg))
        progress.window?.decorView?.postDelayed({
            try {
                val jsonObject = repository.exportGroupJson(op.groupId)
                val json = jsonObject.toString(2)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                progress.dismiss()
                showReport(repository.exportReport(jsonObject))
                render()
            } catch (e: Exception) {
                progress.dismiss()
                showStatusReport("Backup Group Failed", e.message ?: "Unknown error", listOf(BackupStep("Export failed", "FAILED", e.message.orEmpty())))
            } finally {
                pending = null
            }
        }, 300)
    }

    private fun exportAppToUri(uri: Uri) {
        val op = pending as? PendingOperation.ExportApp ?: return
        val progress = progressDialog("Backup Identity", listOf("Preparing export", "Capturing identity", "Writing JSON", "Verifying export"))
        progress.show()
        progress.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(panelBg))
        progress.window?.decorView?.postDelayed({
            try {
                val jsonObject = repository.exportAppJson(op.groupId, op.packageName)
                val json = jsonObject.toString(2)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                progress.dismiss()
                showReport(repository.exportReport(jsonObject))
                render()
            } catch (e: Exception) {
                progress.dismiss()
                showStatusReport("Backup App Failed", e.message ?: "Unknown error", listOf(BackupStep("Export failed", "FAILED", e.message.orEmpty())))
            } finally {
                pending = null
            }
        }, 300)
    }

    private fun importGroupFromUri(uri: Uri) {
        val op = pending as? PendingOperation.ImportGroup ?: return
        val raw = readText(uri)
        val preview = repository.previewImport(raw)
        if (!preview.valid) {
            showStatusReport("Restore Failed", preview.message, listOf(BackupStep("Validating IdentityVault export", "FAILED", preview.message)))
            pending = null
            return
        }
        confirmRestore(preview, "Restore ${op.label}?") {
            val progress = progressDialog("Restore Group", listOf("Validating file", "Restoring package identities", "Saving assignments", "Verifying restore"))
            progress.show()
            progress.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(panelBg))
            progress.window?.decorView?.postDelayed({
                val report = repository.importGroup(op.groupId, raw)
                progress.dismiss()
                showReport(report)
                render()
                pending = null
            }, 300)
        }
    }

    private fun importAppFromUri(uri: Uri) {
        val op = pending as? PendingOperation.ImportApp ?: return
        val raw = readText(uri)
        val preview = repository.previewImport(raw)
        if (!preview.valid) {
            showStatusReport("Restore Failed", preview.message, listOf(BackupStep("Validating IdentityVault export", "FAILED", preview.message)))
            pending = null
            return
        }
        confirmRestore(preview, "Restore ${op.label}?") {
            val progress = progressDialog("Restore Identity", listOf("Validating file", "Matching package", "Restoring identity", "Verifying restore"))
            progress.show()
            progress.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(panelBg))
            progress.window?.decorView?.postDelayed({
                val report = repository.importApp(op.groupId, op.packageName, raw)
                progress.dismiss()
                showReport(report)
                render()
                pending = null
            }, 300)
        }
    }

    private fun confirmRestore(preview: IdentitySlotRepository.ImportPreview, title: String, action: () -> Unit) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 12)
            setBackgroundColor(panelBg)
            addView(TextView(context).apply {
                text = preview.message
                textSize = 13f
                setTextColor(textColor)
                setPadding(0, 0, 0, 12)
            })
            preview.apps.forEach { item ->
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 7, 0, 7)
                    addView(TextView(context).apply {
                        text = item.appLabel.ifBlank { item.packageName }
                        textSize = 13f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(textColor)
                    })
                    addView(TextView(context).apply {
                        text = item.packageName
                        textSize = 11.2f
                        setTextColor(muted)
                    })
                    addView(TextView(context).apply {
                        text = if (item.hasIdentity) "Identity profile: OK" else "Identity profile: missing"
                        textSize = 10.8f
                        setTextColor(if (item.hasIdentity) accent else warning)
                    })
                })
            }
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(ScrollView(context).apply {
                setBackgroundColor(panelBg)
                addView(container)
            })
            .setNegativeButton("Cancel") { _, _ -> pending = null }
            .setPositiveButton("Restore") { _, _ -> action() }
            .create()
        dialog.setOnShowListener { dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(panelBg)) }
        dialog.show()
    }

    private fun createJson(title: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
            .putExtra(Intent.EXTRA_TITLE, title)
        activity.startActivityForResult(intent, requestCode)
    }

    private fun openJson(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
        activity.startActivityForResult(intent, requestCode)
    }

    private fun readText(uri: Uri): String = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()

    private fun progressDialog(title: String, steps: List<String>): AlertDialog {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 12)
            setBackgroundColor(panelBg)
            steps.forEachIndexed { index, step ->
                addView(TextView(context).apply {
                    text = "${if (index == 0) "RUNNING" else "PENDING"}  $step"
                    textSize = 12f
                    setTextColor(if (index == 0) accent else muted)
                    setPadding(0, 4, 0, 4)
                })
            }
        }
        return AlertDialog.Builder(context).setTitle(title).setView(box).create()
    }

    private fun showReport(report: SlotBackupReport) {
        showStatusReport(report.title, report.summary, report.steps, report.reportText)
    }

    private fun showStatusReport(title: String, summary: String, steps: List<BackupStep>, copyText: String = "") {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 12)
            setBackgroundColor(panelBg)
            addView(TextView(context).apply {
                text = summary
                textSize = 13f
                setTextColor(textColor)
                setPadding(0, 0, 0, 10)
            })
            steps.forEach {
                addView(TextView(context).apply {
                    text = "${it.title}: ${it.status}${if (it.detail.isBlank()) "" else "\n${it.detail}"}"
                    textSize = 12f
                    setTextColor(when (it.status) {
                        "OK" -> accent
                        "FAILED" -> Color.rgb(220, 105, 100)
                        "SKIPPED", "SKIPPED disabled", "SKIPPED empty" -> muted
                        else -> warning
                    })
                    setPadding(0, 3, 0, 3)
                })
            }
        }
        val scroller = ScrollView(context).apply {
            setBackgroundColor(panelBg)
            addView(box)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(scroller)
            .setNegativeButton("OK", null)
            .setPositiveButton("Copy Report") { _, _ ->
                val text = copyText.ifBlank {
                    buildString {
                        appendLine(title)
                        appendLine(summary)
                        steps.forEach { appendLine("${it.title}: ${it.status} ${it.detail}") }
                    }
                }
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("IdentityVault report", text))
                Toast.makeText(context, "Report copied", Toast.LENGTH_SHORT).show()
            }
            .create()
        dialog.setOnShowListener { dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(panelBg)) }
        dialog.show()
    }

    private fun showAddGroupDialog() {
        val input = EditText(context).apply {
            hint = "Group name"
            setText("Identity Group")
            setSingleLine(true)
        }
        AlertDialog.Builder(context)
            .setTitle("Add Identity Group")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                repository.addGroup(input.text.toString().trim().ifBlank { "Identity Group" })
                render()
            }
            .show()
    }

    private fun confirm(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ -> action() }
            .show()
    }

    private fun loadInstalledAppsAsync(callback: (List<IdentityGroupApp>) -> Unit) {
        installedAppsCache?.let {
            callback(it)
            return
        }
        synchronized(appLoadCallbacks) {
            appLoadCallbacks += callback
            if (appsLoading) return
            appsLoading = true
        }
        Thread {
            val apps = queryInstalledApps()
            mainHandler.post {
                val callbacks = synchronized(appLoadCallbacks) {
                    installedAppsCache = apps
                    appsLoading = false
                    appLoadCallbacks.toList().also { appLoadCallbacks.clear() }
                }
                callbacks.forEach { it(apps) }
            }
        }.start()
    }

    private fun queryInstalledApps(): List<IdentityGroupApp> {
        val pm = context.packageManager
        return pm.getInstalledApplications(0)
            .filter { it.packageName != context.packageName }
            .map { info ->
                val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                IdentityGroupApp(
                    packageName = info.packageName,
                    appLabel = pm.getApplicationLabel(info).toString(),
                    isSystemApp = isSystem,
                    lastExportAt = 0L,
                    lastImportAt = 0L,
                    status = IdentityGroupApp.STATUS_EMPTY
                )
            }
            .sortedBy { it.appLabel.lowercase() }
    }

    private fun appPickerRow(app: IdentityGroupApp, selected: MutableSet<String>): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, 9, 2, 9)
            minimumHeight = 66
            isClickable = true
        }
        val checkbox = CheckBox(context).apply {
            buttonTintList = android.content.res.ColorStateList.valueOf(accent)
            isChecked = app.packageName in selected
            setPadding(0, 0, 8, 0)
        }
        fun setChecked(value: Boolean) {
            checkbox.isChecked = value
            if (value) selected += app.packageName else selected -= app.packageName
        }
        checkbox.setOnCheckedChangeListener { _, checked ->
            if (checked) selected += app.packageName else selected -= app.packageName
        }
        row.setOnClickListener { setChecked(!checkbox.isChecked) }
        row.addView(checkbox, LinearLayout.LayoutParams(46, 50).apply { setMargins(0, 0, 8, 0) })
        row.addView(appIcon(app.packageName), LinearLayout.LayoutParams(42, 42).apply { setMargins(0, 0, 14, 0) })
        row.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 8, 0)
            addView(TextView(context).apply {
                text = app.appLabel
                textSize = 13.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textColor)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            addView(TextView(context).apply {
                text = app.packageName
                textSize = 11.2f
                setTextColor(muted)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    private inner class AppPickerAdapter(
        private val allApps: List<IdentityGroupApp>,
        private val selected: MutableSet<String>,
        includeSystemInitial: Boolean,
        queryInitial: String
    ) : BaseAdapter() {
        private var includeSystem = includeSystemInitial
        private var query = queryInitial.trim().lowercase()
        private var rows = filterRows()

        fun updateFilter(includeSystem: Boolean, query: String) {
            val nextQuery = query.trim().lowercase()
            if (this.includeSystem == includeSystem && this.query == nextQuery) return
            this.includeSystem = includeSystem
            this.query = nextQuery
            rows = filterRows()
            notifyDataSetChanged()
        }

        override fun getCount(): Int = rows.size
        override fun getItem(position: Int): IdentityGroupApp = rows[position]
        override fun getItemId(position: Int): Long = getItem(position).packageName.hashCode().toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val holder: PickerHolder
            val root = if (convertView == null) {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(10, 10, 10, 10)
                    minimumHeight = 68
                    isClickable = true
                    background = rounded(Color.TRANSPARENT, 0, Color.TRANSPARENT, 0f)
                }
                val check = TextView(context).apply {
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                }
                val icon = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(2, 2, 2, 2)
                }
                val texts = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val title = TextView(context).apply {
                    textSize = 13.6f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(textColor)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
                val pkg = TextView(context).apply {
                    textSize = 11.2f
                    setTextColor(muted)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
                texts.addView(title)
                texts.addView(pkg)
                row.addView(check, LinearLayout.LayoutParams(30, 30).apply { setMargins(4, 0, 22, 0) })
                row.addView(icon, LinearLayout.LayoutParams(40, 40).apply { setMargins(0, 0, 18, 0) })
                row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                holder = PickerHolder(check, icon, title, pkg)
                row.tag = holder
                row
            } else {
                holder = convertView.tag as PickerHolder
                convertView as LinearLayout
            }
            val app = getItem(position)
            val checked = app.packageName in selected
            stylePickerCheck(holder.check, checked)
            holder.icon.setImageDrawable(iconDrawable(app.packageName))
            holder.title.text = app.appLabel
            holder.packageName.text = app.packageName
            root.setOnClickListener {
                val next = app.packageName !in selected
                if (next) selected += app.packageName else selected -= app.packageName
                stylePickerCheck(holder.check, next)
            }
            return root
        }

        private fun filterRows(): List<IdentityGroupApp> =
            allApps.asSequence()
                .filter { if (includeSystem) it.isSystemApp else !it.isSystemApp }
                .filter { query.isBlank() || it.appLabel.lowercase().contains(query) || it.packageName.lowercase().contains(query) }
                .take(260)
                .toList()
    }

    private data class PickerHolder(
        val check: TextView,
        val icon: ImageView,
        val title: TextView,
        val packageName: TextView
    )

    private fun appIcon(packageName: String): View = ImageView(context).apply {
        setImageDrawable(iconDrawable(packageName))
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setPadding(3, 3, 3, 3)
    }

    private fun iconDrawable(packageName: String): Drawable? = iconCache.getOrPut(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    private fun iconText(value: String): TextView = TextView(context).apply {
        text = value
        textSize = 21f
        gravity = Gravity.CENTER
        setTextColor(accent)
        background = rounded(Color.TRANSPARENT, 0, Color.TRANSPARENT, 6f)
    }

    private fun fab(): TextView = TextView(context).apply {
        text = "+"
        textSize = 25f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(232, 248, 247))
        gravity = Gravity.CENTER
        includeFontPadding = false
        elevation = 8f
        background = rounded(Color.rgb(45, 156, 147), 1, Color.rgb(72, 188, 178), 54f)
        setOnClickListener { showAddGroupDialog() }
    }

    private fun dialogButton(label: String): Button = Button(context).apply {
        text = label
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setAllCaps(false)
        includeFontPadding = false
        minHeight = 0
        minWidth = 0
        gravity = Gravity.CENTER
        setPadding(8, 0, 8, 0)
        setTextColor(accent)
        background = rounded(softBg, 1, Color.rgb(52, 64, 78), 6f)
    }

    private fun styleFilterButton(button: Button, selected: Boolean) {
        button.setTextColor(if (selected) Color.rgb(235, 244, 246) else accent)
        button.background = rounded(
            if (selected) Color.rgb(31, 48, 55) else softBg,
            1,
            if (selected) accent else Color.rgb(52, 64, 78),
            6f
        )
    }

    private fun flatTextButton(label: String): TextView = TextView(context).apply {
        text = label
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        includeFontPadding = false
        setTextColor(accent)
        background = rounded(Color.TRANSPARENT, 0, Color.TRANSPARENT, 6f)
    }

    private fun stylePickerCheck(view: TextView, checked: Boolean) {
        view.text = if (checked) "✓" else ""
        view.setTextColor(if (checked) Color.rgb(14, 30, 33) else accent)
        view.background = rounded(
            if (checked) accent else Color.TRANSPARENT,
            2,
            accent,
            2f
        )
    }

    private fun rounded(color: Int, strokeWidth: Int, strokeColor: Int, radius: Float): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
            cornerRadius = radius
        }

    private fun timestamp(app: IdentityGroupApp): String {
        val time = maxOf(app.lastImportAt, app.lastExportAt)
        if (time <= 0) return ""
        return DateFormat.format("yyyy-MM-dd HH:mm", Date(time)).toString()
    }

    private fun safeName(raw: String): String = raw.lowercase().replace(Regex("[^a-z0-9._-]+"), "-").trim('-').ifBlank { "identity" }

    private sealed class PendingOperation {
        data class ExportGroup(val groupId: String, val label: String) : PendingOperation()
        data class ExportApp(val groupId: String, val packageName: String, val label: String) : PendingOperation()
        data class ImportGroup(val groupId: String, val label: String) : PendingOperation()
        data class ImportApp(val groupId: String, val packageName: String, val label: String) : PendingOperation()
    }

    companion object {
        const val REQUEST_EXPORT_GROUP = 801
        const val REQUEST_EXPORT_APP = 802
        const val REQUEST_IMPORT_GROUP = 803
        const val REQUEST_IMPORT_APP = 804
        val REQUEST_CODES = setOf(REQUEST_EXPORT_GROUP, REQUEST_EXPORT_APP, REQUEST_IMPORT_GROUP, REQUEST_IMPORT_APP)
    }
}
