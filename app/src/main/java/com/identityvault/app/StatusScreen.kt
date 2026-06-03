package com.identityvault.app

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.identityvault.app.data.BuildPropProfile
import com.identityvault.app.data.IdentityFieldState
import com.identityvault.app.data.IdentityProfile
import com.identityvault.app.status.StatusViewModel

class StatusScreen(
    private val context: Context,
    private val viewModel: StatusViewModel,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onExportBackup()
        fun onImportBackup()
        fun onShowMenu(anchor: View)
    }

    private val bg = Color.rgb(13, 17, 24)
    private val panelBg = Color.rgb(19, 24, 32)
    private val bottomBg = Color.rgb(15, 20, 28)
    private val line = Color.rgb(38, 46, 58)
    private val textColor = Color.rgb(225, 231, 239)
    private val muted = Color.rgb(142, 153, 168)
    private val quiet = Color.rgb(96, 108, 124)
    private val accent = Color.rgb(69, 178, 166)
    private val iconIdle = Color.rgb(118, 130, 146)
    private val errorColor = Color.rgb(205, 100, 94)
    private val dialogBg = Color.rgb(18, 23, 31)

    private lateinit var content: LinearLayout
    private val fieldRows = linkedMapOf<String, FieldRow>()
    private var buildProfile: BuildPropProfile = BuildPropProfile.default()
    private lateinit var buildRow: FieldRow
    private val smartEditFields = setOf(
        "IMEI",
        "Android ID",
        "Google Services Framework ID",
        "MediaDrm ID",
        "Serial",
        "Hardware ID",
        "MAC Address",
        "MAC BSSID",
        "Bluetooth MAC",
        "SIM Serial ID",
        "Mobile No",
        "SIM Operator"
    )

    fun create(): View {
        val shell = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(context).apply {
            setBackgroundColor(bg)
            isFillViewport = false
        }
        content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 22)
            setBackgroundColor(bg)
        }
        scroll.addView(content)
        shell.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        render()
        return shell
    }

    fun render() {
        content.removeAllViews()
        fieldRows.clear()
        topBar()
        fieldList(viewModel.identityRepository.getProfile())
    }

    private fun topBar() {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(TextView(context).apply {
            text = "IdentityVault"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        titleBox.addView(TextView(context).apply {
            text = "IdentityVault changed !"
            textSize = 11f
            setTextColor(muted)
        })
        row.addView(titleBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(overflowButton().apply { setOnClickListener { callbacks.onShowMenu(this) } })
        content.addView(row)
    }

    private fun fieldList(profile: IdentityProfile) {
        buildProfile = profile.buildProp
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 12)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(panelBg)
                cornerRadius = 7f
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 14, 0, 0)
            }
        }
        panel.addView(actionRow())
        addField(panel, "IMEI", profile.imei)
        addField(panel, "Android ID", profile.androidId)
        addField(panel, "Google Services Framework ID", profile.gsfId)
        addField(panel, "MediaDrm ID", profile.mediaDrmId)
        addField(panel, "Serial", profile.serial)
        addField(panel, "Hardware ID", profile.hardwareId)
        addField(panel, "MAC Address", profile.macAddress)
        addField(panel, "MAC BSSID", profile.macBssid)
        addField(panel, "MAC SSID", profile.macSsid)
        addField(panel, "Bluetooth MAC", profile.bluetoothMac)
        addField(panel, "SIM Serial ID", profile.simSerialId)
        addField(panel, "SIM Sub IDs", profile.simSubIds)
        addField(panel, "Mobile No", profile.mobileNo)
        addField(panel, "SIM Operator", profile.simOperator)
        addBuildProp(panel, profile.buildPropEnabled)
        content.addView(panel)
    }

    private fun addField(parent: LinearLayout, label: String, field: IdentityFieldState) {
        val box = rowBox()
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(labelView(label), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(iconButton(R.drawable.ic_shuffle, true).apply { setOnClickListener { randomizeField(label) } })
        if (label in smartEditFields) {
            header.addView(iconButton(R.drawable.ic_mask_edit, true).apply { setOnClickListener { showSmartMaskEdit(label) } })
        }
        var enabled = field.enabled
        val toggle = iconButton(R.drawable.ic_power, enabled)
        toggle.setOnClickListener {
            enabled = !enabled
            styleIcon(toggle, enabled)
            fieldRows[label] = fieldRows[label]!!.copy(enabled = enabled)
            validateSingleField(label)
        }
        header.addView(toggle)
        val value = valueEdit(field.value)
        val error = errorView()
        box.addView(header)
        box.addView(value)
        box.addView(error)
        parent.addView(box)
        fieldRows[label] = FieldRow(value, error, enabled)
        value.addTextChangedListener(simpleWatcher { validateSingleField(label) })
    }

    private fun addBuildProp(parent: LinearLayout, enabledInitial: Boolean) {
        val box = rowBox()
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(labelView("Build Prop"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(iconButton(R.drawable.ic_shuffle, true).apply {
            setOnClickListener {
                buildProfile = presets().random()
                buildRow.value.setText(buildProfile.fingerprint)
            }
        })
        var enabled = enabledInitial
        val toggle = iconButton(R.drawable.ic_power, enabled)
        toggle.setOnClickListener {
            enabled = !enabled
            styleIcon(toggle, enabled)
            buildRow = buildRow.copy(enabled = enabled)
            validateBuildProp()
        }
        header.addView(toggle)
        val value = valueEdit(buildProfile.fingerprint)
        val error = errorView()
        box.addView(header)
        box.addView(value)
        box.addView(error)
        parent.addView(box)
        buildRow = FieldRow(value, error, enabled)
        value.addTextChangedListener(simpleWatcher { validateBuildProp() })
    }

    private fun actionRow(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(0, 2, 0, 10)
        addView(actionButton("Generate All").apply { setOnClickListener { generateAllActive() } }, LinearLayout.LayoutParams(0, 44, 1f).apply { setMargins(0, 0, 6, 0) })
        addView(actionButton("Apply").apply { setOnClickListener { applyProfile() } }, LinearLayout.LayoutParams(0, 44, 1f).apply { setMargins(6, 0, 0, 0) })
    }

    private fun randomizeField(label: String) {
        val generated = viewModel.generateProfileObject()
        val state = when (label) {
            "IMEI" -> generated.imei
            "Android ID" -> generated.androidId
            "Google Services Framework ID" -> generated.gsfId
            "MediaDrm ID" -> generated.mediaDrmId
            "Serial" -> generated.serial
            "Hardware ID" -> generated.hardwareId
            "MAC Address" -> generated.macAddress
            "MAC BSSID" -> generated.macBssid
            "MAC SSID" -> generated.macSsid
            "Bluetooth MAC" -> generated.bluetoothMac
            "SIM Serial ID" -> generated.simSerialId
            "SIM Sub IDs" -> generated.simSubIds
            "Mobile No" -> generated.mobileNo
            "SIM Operator" -> generated.simOperator
            else -> null
        } ?: return
        fieldRows[label]?.value?.setText(state.value)
    }

    private fun generateAllActive() {
        val generated = viewModel.generateProfileObject()
        fun set(label: String, state: IdentityFieldState) {
            val row = fieldRows[label] ?: return
            if (row.enabled) row.value.setText(state.value)
        }
        set("IMEI", generated.imei)
        set("Android ID", generated.androidId)
        set("Google Services Framework ID", generated.gsfId)
        set("MediaDrm ID", generated.mediaDrmId)
        set("Serial", generated.serial)
        set("Hardware ID", generated.hardwareId)
        set("MAC Address", generated.macAddress)
        set("MAC BSSID", generated.macBssid)
        set("MAC SSID", generated.macSsid)
        set("Bluetooth MAC", generated.bluetoothMac)
        set("SIM Serial ID", generated.simSerialId)
        set("SIM Sub IDs", generated.simSubIds)
        set("Mobile No", generated.mobileNo)
        set("SIM Operator", generated.simOperator)
        if (buildRow.enabled) {
            buildProfile = generated.buildProp
            buildRow.value.setText(generated.buildProp.fingerprint)
        }
        toast("Active fields generated")
    }

    private fun saveProfile(showToast: Boolean = true): Boolean {
        val errors = validateAllFields()
        if (errors.isNotEmpty()) {
            focusFirstInvalid(errors)
            toast("Periksa field yang belum valid")
            return false
        }
        viewModel.identityRepository.saveProfile(currentProfile())
        viewModel.markProfileDirty()
        if (showToast) toast("Profile saved")
        return true
    }

    private fun applyProfile() {
        if (!saveProfile(showToast = false)) return
        viewModel.applyProfile()
        toast("Profile applied. Force stop dan buka ulang app target jika perlu.")
    }

    private fun showSmartMaskEdit(label: String) {
        val row = fieldRows[label] ?: return
        val current = row.value.text.toString().trim()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 16)
            setBackgroundColor(dialogBg)
        }
        container.addView(TextView(context).apply {
            text = label
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        container.addView(TextView(context).apply {
            text = "Current: $current"
            textSize = 12f
            setTextColor(muted)
            setPadding(0, 8, 0, 8)
        })
        val resultPreview = TextView(context).apply {
            text = ""
            textSize = 12f
            setTextColor(accent)
            visibility = View.GONE
            setPadding(0, 8, 0, 0)
        }
        val maskInput = EditText(context).apply {
            setText(current.map { if (it.isLetterOrDigit()) 'x' else it }.joinToString(""))
            textSize = 13f
            setSingleLine(true)
            setTextColor(textColor)
            setHintTextColor(quiet)
            backgroundTintList = ColorStateList.valueOf(line)
            hint = "Mask, contoh xxxxxxxxx911xxx"
        }
        val attention = TextView(context).apply {
            textSize = 11f
            setTextColor(errorColor)
            visibility = View.GONE
            setPadding(0, 8, 0, 0)
        }
        container.addView(maskInput)
        container.addView(resultPreview)
        container.addView(attention)
        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, 16, 0, 0)
        }
        val dialog = AlertDialog.Builder(context).create()
        var pendingResult: String? = null
        maskInput.addTextChangedListener(simpleWatcher {
            pendingResult = null
            resultPreview.visibility = View.GONE
            attention.visibility = View.GONE
        })
        buttonRow.addView(dialogButton("Cancel").apply { setOnClickListener { dialog.dismiss() } }, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(0, 0, 5, 0) })
        buttonRow.addView(dialogButton("Random").apply {
            setOnClickListener {
                val result = smartMaskedValue(label, maskInput.text.toString())
                if (result == null) {
                    attention.text = "Mask tidak bisa menghasilkan nilai valid untuk $label."
                    attention.visibility = View.VISIBLE
                    resultPreview.visibility = View.GONE
                    pendingResult = null
                    return@setOnClickListener
                }
                pendingResult = result
                resultPreview.text = "Result: $result"
                resultPreview.visibility = View.VISIBLE
                attention.visibility = View.GONE
            }
        }, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(5, 0, 5, 0) })
        buttonRow.addView(dialogButton("Apply").apply {
            setOnClickListener {
                val result = pendingResult ?: smartMaskedValue(label, maskInput.text.toString())
                if (result == null) {
                    attention.text = "Mask tidak bisa menghasilkan nilai valid untuk $label."
                    attention.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                row.value.setText(result)
                validateSingleField(label)
                dialog.dismiss()
            }
        }, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(5, 0, 0, 0) })
        container.addView(buttonRow)
        dialog.setView(container)
        dialog.setOnShowListener { dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(dialogBg)) }
        dialog.show()
    }

    private fun smartMaskedValue(label: String, mask: String): String? {
        val cleanMask = mask.trim()
        if (cleanMask.isBlank()) return null
        repeat(if (label == "IMEI") 5000 else 60) {
            val generated = generatedValueFor(label) ?: return null
            val merged = mergeMask(cleanMask, generated)
            if (validateField(label, merged, true) == null) return merged
        }
        return null
    }

    private fun mergeMask(mask: String, generated: String): String {
        return mask.mapIndexed { index, char ->
            if (char == 'x' || char == 'X') generated.getOrNull(index) ?: generated.lastOrNull() ?: '0' else char
        }.joinToString("")
    }

    private fun generatedValueFor(label: String): String? {
        val generated = viewModel.generateProfileObject()
        return when (label) {
            "IMEI" -> generated.imei.value
            "Android ID" -> generated.androidId.value
            "Google Services Framework ID" -> generated.gsfId.value
            "MediaDrm ID" -> generated.mediaDrmId.value
            "Serial" -> generated.serial.value
            "Hardware ID" -> generated.hardwareId.value
            "MAC Address" -> generated.macAddress.value
            "MAC BSSID" -> generated.macBssid.value
            "Bluetooth MAC" -> generated.bluetoothMac.value
            "SIM Serial ID" -> generated.simSerialId.value
            "Mobile No" -> generated.mobileNo.value
            "SIM Operator" -> generated.simOperator.value
            else -> null
        }
    }

    private fun currentProfile(): IdentityProfile {
        fun field(label: String): IdentityFieldState {
            val row = fieldRows[label]
            return IdentityFieldState(row?.value?.text?.toString()?.trim().orEmpty(), row?.enabled == true)
        }
        return IdentityProfile(
            name = viewModel.identityRepository.getProfile().name.ifBlank { "Default Profile" },
            imei = field("IMEI"),
            serial = field("Serial"),
            hardwareId = field("Hardware ID"),
            macAddress = field("MAC Address"),
            macBssid = field("MAC BSSID"),
            macSsid = field("MAC SSID"),
            bluetoothMac = field("Bluetooth MAC"),
            androidId = field("Android ID"),
            simSerialId = field("SIM Serial ID"),
            simSubIds = field("SIM Sub IDs"),
            mobileNo = field("Mobile No"),
            mediaDrmId = field("MediaDrm ID"),
            simOperator = field("SIM Operator"),
            gsfId = field("Google Services Framework ID"),
            buildProp = parseBuildProp(buildRow.value.text.toString(), buildProfile),
            buildPropEnabled = buildRow.enabled,
            draft = false
        )
    }

    fun showLogs() {
        val detailed = viewModel.logRepository.getDetailedRepository()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 14)
            setBackgroundColor(dialogBg)
        }
        container.addView(TextView(context).apply {
            text = "Log"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }
        )
        val summary = detailed.summary()
        val summaryText = TextView(context).apply {
            text = "Success: ${summary["SUCCESS"] ?: 0}   Warning: ${summary["WARNING"] ?: 0}   Error: ${summary["ERROR"] ?: 0}   Skipped: ${summary["SKIPPED"] ?: 0}"
            textSize = 12f
            setTextColor(muted)
            setPadding(0, 8, 0, 0)
        }
        val diagnostics = viewModel.diagnosticsHookedPackages()
        val diagnosticsText = TextView(context).apply {
            val settingsSeen = diagnostics.any { it.first == "com.android.settings" && it.second }
            text = buildString {
                append("Diagnostics Hooked Packages\n")
                diagnostics.forEach { (pkg, seen) -> append(pkg).append(": ").append(if (seen) "hooked" else "not seen").append('\n') }
                if (!settingsSeen) append("Warning: Settings belum pernah loaded oleh LSPosed. Centang com.android.settings lalu reboot emulator.")
            }.trim()
            textSize = 12f
            setTextColor(if (settingsSeen) muted else Color.rgb(220, 174, 90))
            setPadding(0, 10, 0, 0)
        }
        val logList = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        fun setFilter(level: String) {
            logList.removeAllViews()
            val entries = detailed.getEntries().filter { level == "ALL" || it.level.equals(level, ignoreCase = true) }
            if (entries.isEmpty()) {
                logList.addView(TextView(context).apply {
                    text = "Belum ada log"
                    textSize = 12f
                    setTextColor(muted)
                    setPadding(0, 12, 0, 0)
                })
                return
            }
            entries.takeLast(500).forEach { entry ->
                logList.addView(TextView(context).apply {
                    text = "[${entry.level}] ${entry.category}\n${entry.packageName.ifBlank { "local" }} · SDK ${if (entry.sdkInt == 0) "-" else entry.sdkInt}\n${entry.message}${if (entry.detail.isBlank()) "" else "\n${entry.detail}"}"
                    textSize = 12f
                    setTextColor(levelColor(entry.level))
                    setPadding(0, 10, 0, 10)
                })
            }
        }
        val filters = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 0)
        }
        listOf("ALL" to "All", "SUCCESS" to "Success", "WARNING" to "Warning", "ERROR" to "Error", "SKIPPED" to "Skipped").forEach { (level, label) ->
            filters.addView(dialogButton(label).apply {
                text = label
                textSize = 10f
                setOnClickListener { setFilter(level) }
            }, LinearLayout.LayoutParams(0, 36, 1f).apply { setMargins(2, 0, 2, 0) })
        }
        container.addView(summaryText)
        container.addView(diagnosticsText)
        container.addView(filters)
        container.addView(ScrollView(context).apply { addView(logList) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 420))
        val dialog = AlertDialog.Builder(context).create()
        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 0)
        }
        buttons.addView(dialogButton("Clear").apply {
            setOnClickListener {
                viewModel.logRepository.clear()
                setFilter("ALL")
            }
        }, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(0, 0, 6, 0) })
        buttons.addView(dialogButton("Copy").apply {
            setOnClickListener {
                val text = detailed.getDisplayLines("ALL").joinToString("\n\n")
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("IdentityVault logs", text))
                toast("Log copied")
            }
        }, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(6, 0, 6, 0) })
        buttons.addView(dialogButton("Close").apply { setOnClickListener { dialog.dismiss() } }, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(6, 0, 0, 0) })
        container.addView(buttons)
        setFilter("ALL")
        dialog.setView(container)
        dialog.setOnShowListener { dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(dialogBg)) }
        dialog.show()
    }

    fun showAbout() {
        AlertDialog.Builder(context)
            .setTitle("IdentityVault")
            .setMessage("Identity profile testing module.\n\nUse for controlled testing only.")
            .setPositiveButton("Close", null)
            .show()
    }

    fun resetProfile() {
        viewModel.resetProfile()
        toast("Profile reset")
        render()
    }

    private fun rowBox(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(10, 8, 10, 7)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = 4f
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 7, 0, 0)
        }
    }

    private fun labelView(label: String): TextView = TextView(context).apply {
        text = label
        textSize = 13.5f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(textColor)
    }

    private fun valueEdit(value: String): EditText = EditText(context).apply {
        setText(value)
        textSize = 12.5f
        setTextColor(muted)
        setSingleLine(true)
        ellipsize = TextUtils.TruncateAt.END
        inputType = InputType.TYPE_CLASS_TEXT
        includeFontPadding = false
        setPadding(0, 4, 0, 9)
        backgroundTintList = ColorStateList.valueOf(line)
        setHintTextColor(quiet)
    }

    private fun errorView(): TextView = TextView(context).apply {
        text = ""
        textSize = 11f
        setTextColor(errorColor)
        visibility = View.GONE
        setPadding(0, 2, 0, 0)
    }

    private fun iconButton(icon: Int, active: Boolean): ImageButton = ImageButton(context).apply {
        setImageResource(icon)
        scaleType = android.widget.ImageView.ScaleType.CENTER
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = 5f
        }
        styleIcon(this, active)
        layoutParams = LinearLayout.LayoutParams(30, 28).apply { setMargins(5, 0, 0, 0) }
    }

    private fun styleIcon(button: ImageButton, active: Boolean) {
        button.imageTintList = ColorStateList.valueOf(if (active) accent else iconIdle)
    }

    private fun topButton(label: String): Button = Button(context).apply {
        text = label
        textSize = 11.5f
        setAllCaps(false)
        minHeight = 0
        minWidth = 0
        setPadding(10, 4, 10, 4)
        setTextColor(muted)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(1, line)
            cornerRadius = 5f
        }
    }

    private fun overflowButton(): TextView = TextView(context).apply {
        text = "\u22EE"
        textSize = 19f
        gravity = Gravity.CENTER
        setTextColor(muted)
        layoutParams = LinearLayout.LayoutParams(30, 34)
    }

    private fun actionButton(label: String): Button = Button(context).apply {
        text = label
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setAllCaps(false)
        minHeight = 0
        minWidth = 0
        gravity = Gravity.CENTER
        includeFontPadding = false
        setPadding(4, 0, 4, 0)
        setTextColor(textColor)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(25, 33, 44))
            setStroke(1, Color.rgb(47, 59, 73))
            cornerRadius = 6f
        }
    }

    private fun dialogButton(label: String): Button = Button(context).apply {
        text = label
        textSize = 11.5f
        typeface = Typeface.DEFAULT_BOLD
        setAllCaps(false)
        minHeight = 0
        minWidth = 0
        gravity = Gravity.CENTER
        includeFontPadding = false
        setPadding(8, 0, 8, 0)
        setTextColor(accent)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(25, 33, 44))
            setStroke(1, Color.rgb(52, 64, 78))
            cornerRadius = 6f
        }
    }

    private fun levelColor(level: String): Int = when (level.uppercase()) {
        "SUCCESS" -> Color.rgb(105, 190, 140)
        "INFO" -> Color.rgb(111, 180, 220)
        "WARNING" -> Color.rgb(220, 174, 90)
        "ERROR" -> Color.rgb(220, 105, 100)
        "SKIPPED" -> Color.rgb(176, 184, 196)
        else -> textColor
    }

    private fun validateAllFields(): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        fieldRows.forEach { (label, row) ->
            validateField(label, row.value.text.toString(), row.enabled)?.let { errors[label] = it }
        }
        validateField("Build Prop", buildRow.value.text.toString(), buildRow.enabled)?.let { errors["Build Prop"] = it }
        showFieldErrors(errors)
        return errors
    }

    private fun validateSingleField(label: String) {
        val row = fieldRows[label] ?: return
        val message = validateField(label, row.value.text.toString(), row.enabled)
        showRowError(row, message)
    }

    private fun validateBuildProp() {
        showRowError(buildRow, validateField("Build Prop", buildRow.value.text.toString(), buildRow.enabled))
    }

    private fun validateField(label: String, raw: String, enabled: Boolean): String? {
        if (!enabled) return null
        val value = raw.trim()
        return when (label) {
            "IMEI" -> if (value.matches(Regex("[0-9]{15}")) && luhnValid(value)) null else "IMEI harus 15 digit dan lolos Luhn checksum."
            "Android ID" -> if (value.matches(Regex("[0-9a-fA-F]{16}"))) null else "Android ID harus 16 karakter hex."
            "Google Services Framework ID" -> if (value.matches(Regex("[0-9a-fA-F]{16}"))) null else "Google Services Framework ID harus 16 karakter hex."
            "MediaDrm ID" -> if (value.matches(Regex("[0-9a-fA-F]{16,64}"))) null else "MediaDrm ID harus hex dan tidak kosong."
            "Serial" -> if (value.isNotBlank()) null else "Serial tidak boleh kosong."
            "Hardware ID" -> if (value.isNotBlank()) null else "Hardware ID tidak boleh kosong."
            "MAC Address", "MAC BSSID", "Bluetooth MAC" -> if (value.matches(Regex("(?i)^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))) null else "$label harus format XX:XX:XX:XX:XX:XX."
            "SIM Serial ID" -> if (value.matches(Regex("[0-9]{19,20}"))) null else "SIM Serial ID harus 19-20 digit."
            "Mobile No" -> if (value.matches(Regex("\\+?[0-9]{8,15}"))) null else "Mobile No hanya boleh angka dan tanda + dengan panjang wajar."
            "SIM Operator" -> if (value.matches(Regex("[0-9]{5,6}"))) null else "SIM Operator harus MCC+MNC numerik, contoh 51010."
            "Build Prop" -> if (value.isNotBlank() && value.contains("/") && value.contains(":")) null else "Build Prop harus fingerprint/build string dan mengandung / serta :."
            else -> null
        }
    }

    private fun showFieldErrors(errors: Map<String, String>) {
        fieldRows.forEach { (label, row) -> showRowError(row, errors[label]) }
        showRowError(buildRow, errors["Build Prop"])
    }

    private fun showRowError(row: FieldRow, message: String?) {
        row.error.text = message.orEmpty()
        row.error.visibility = if (message == null) View.GONE else View.VISIBLE
        row.value.backgroundTintList = ColorStateList.valueOf(if (message == null) line else errorColor)
    }

    private fun focusFirstInvalid(errors: Map<String, String>) {
        val key = errors.keys.firstOrNull() ?: return
        val target = if (key == "Build Prop") buildRow.value else fieldRows[key]?.value
        target?.requestFocus()
    }

    private fun luhnValid(number: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in number.length - 1 downTo 0) {
            var n = number[i].digitToIntOrNull() ?: return false
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private fun simpleWatcher(after: () -> Unit): TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) = after()
    }

    private fun presets(): List<BuildPropProfile> = listOf(
        BuildPropProfile(
            fingerprint = "samsung/a52sxq/a52sxq:12/SP1A.210812.016/A528BXXS1DVL2:user/release-keys",
            brand = "samsung",
            model = "SM-A528B",
            manufacturer = "samsung",
            device = "a52sxq",
            name = "a52sxq",
            board = "lahaina",
            hardware = "qcom",
            buildId = "SP1A.210812.016",
            displayId = "SP1A.210812.016.A528BXXS1DVL2",
            versionRelease = "12",
            versionSdk = "31",
            securityPatch = "2022-12-01"
        ),
        BuildPropProfile.default(),
        BuildPropProfile(
            fingerprint = "Xiaomi/vayu/vayu:11/RKQ1.200826.002/V12.5.7.0.RJUMIXM:user/release-keys",
            brand = "Xiaomi",
            model = "M2102J20SG",
            manufacturer = "Xiaomi",
            device = "vayu",
            name = "vayu",
            board = "vayu",
            hardware = "qcom",
            buildId = "RKQ1.200826.002",
            displayId = "RKQ1.200826.002",
            versionRelease = "11",
            versionSdk = "30",
            securityPatch = "2021-12-01"
        )
    )

    private fun parseBuildProp(raw: String, fallback: BuildPropProfile): BuildPropProfile {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return fallback
        if (!trimmed.contains("=") && trimmed.contains(":") && trimmed.contains("/")) {
            return fallback.copy(fingerprint = trimmed)
        }
        val map = trimmed.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
            .associate {
                val index = it.indexOf("=")
                it.substring(0, index).trim() to it.substring(index + 1).trim()
            }
        return BuildPropProfile(
            fingerprint = map["ro.build.fingerprint"] ?: fallback.fingerprint,
            brand = map["ro.product.brand"] ?: fallback.brand,
            model = map["ro.product.model"] ?: fallback.model,
            manufacturer = map["ro.product.manufacturer"] ?: fallback.manufacturer,
            device = map["ro.product.device"] ?: fallback.device,
            name = map["ro.product.name"] ?: fallback.name,
            board = map["ro.product.board"] ?: fallback.board,
            hardware = map["ro.hardware"] ?: fallback.hardware,
            buildId = map["ro.build.id"] ?: fallback.buildId,
            displayId = map["ro.build.display.id"] ?: fallback.displayId,
            versionRelease = map["ro.build.version.release"] ?: fallback.versionRelease,
            versionSdk = map["ro.build.version.sdk"] ?: fallback.versionSdk,
            securityPatch = map["ro.build.version.security_patch"] ?: fallback.securityPatch
        )
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private data class FieldRow(val value: EditText, val error: TextView, val enabled: Boolean)
}
