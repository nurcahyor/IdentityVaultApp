package com.identityvault.app

import android.app.AlertDialog
import android.app.Dialog
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
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.ListView
import android.widget.PopupWindow
import com.identityvault.app.data.BuildPropProfile
import com.identityvault.app.data.IdentityFieldState
import com.identityvault.app.data.IdentityProfile
import com.identityvault.app.identity.BuildPropPreset
import com.identityvault.app.identity.BuildPropPresetRepository
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
    private val prefs = context.getSharedPreferences("identityvault_ui", Context.MODE_PRIVATE)

    private lateinit var content: LinearLayout
    private val fieldRows = linkedMapOf<String, FieldRow>()
    private var buildProfile: BuildPropProfile = BuildPropProfile.default()
    private lateinit var buildRow: FieldRow
    private var buildPropChooserOpen = false
    private var lastBuildPropActionAt = 0L
    private val smartEditFields = setOf(
        "IMEI",
        "IMEI 1",
        "IMEI 2",
        "MEID",
        "Android ID",
        "Google Services Framework ID",
        "Advertising ID",
        "Google Account Email",
        "Device Name",
        "MediaDrm ID",
        "Serial",
        "Hardware ID",
        "MAC Address",
        "MAC BSSID",
        "Bluetooth MAC",
        "Bluetooth Name",
        "SIM Serial ID",
        "Subscriber ID / IMSI",
        "Mobile No",
        "SIM Operator",
        "Network Operator",
        "SIM Operator Name",
        "Network Operator Name",
        "SIM Country ISO",
        "Network Country ISO",
        "Phone Type",
        "Network Type",
        "Data Network Type",
        "Voice Network Type",
        "Limit Ad Tracking"
    )
    private var advancedExpanded: Boolean
        get() = prefs.getBoolean("advanced_sim_network_expanded", false)
        set(value) {
            prefs.edit().putBoolean("advanced_sim_network_expanded", value).apply()
        }

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
        addField(panel, "IMEI 1", profile.imei)
        addField(panel, "IMEI 2", profile.imei2)
        addField(panel, "Android ID", profile.androidId)
        addField(panel, "Google Services Framework ID", profile.gsfId)
        addField(panel, "Advertising ID", profile.advertisingId)
        addField(panel, "Limit Ad Tracking", profile.limitAdTrackingEnabled)
        addField(panel, "Google Account Email", profile.googleAccountEmail)
        addField(panel, "Device Name", profile.deviceName)
        addField(panel, "MediaDrm ID", profile.mediaDrmId)
        addField(panel, "Serial", profile.serial)
        addField(panel, "Hardware ID", profile.hardwareId)
        addField(panel, "MAC Address", profile.macAddress)
        addField(panel, "MAC BSSID", profile.macBssid)
        addField(panel, "MAC SSID", profile.macSsid)
        addField(panel, "Bluetooth MAC", profile.bluetoothMac)
        addField(panel, "Bluetooth Name", profile.bluetoothName)
        addField(panel, "SIM Serial ID", profile.simSerialId)
        addField(panel, "Subscriber ID / IMSI", profile.subscriberId)
        addField(panel, "SIM Sub IDs", profile.simSubIds)
        addField(panel, "Mobile No", profile.mobileNo)
        addField(panel, "SIM Operator", profile.simOperator)
        addAdvancedSection(panel, profile)
        addBuildProp(panel, profile.buildPropEnabled)
        content.addView(panel)
    }

    private fun addAdvancedSection(parent: LinearLayout, profile: IdentityProfile) {
        val advancedBody = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (advancedExpanded) View.VISIBLE else View.GONE
        }
        val chevron = TextView(context).apply {
            text = if (advancedExpanded) "▾" else "▸"
            textSize = 17f
            setTextColor(accent)
            gravity = Gravity.CENTER
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 8)
            isClickable = true
            setOnClickListener {
                val expanded = advancedBody.visibility != View.VISIBLE
                advancedBody.visibility = if (expanded) View.VISIBLE else View.GONE
                chevron.text = if (expanded) "▾" else "▸"
                advancedExpanded = expanded
            }
        }
        header.addView(TextView(context).apply {
            text = "Advanced SIM / Network"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(chevron, LinearLayout.LayoutParams(30, 30))
        parent.addView(header)

        addField(advancedBody, "MEID", profile.meid)
        addField(advancedBody, "SIM Operator Name", profile.simOperatorName)
        addField(advancedBody, "Network Operator", profile.networkOperator)
        addField(advancedBody, "Network Operator Name", profile.networkOperatorName)
        addField(advancedBody, "SIM Country ISO", profile.simCountryIso)
        addField(advancedBody, "Network Country ISO", profile.networkCountryIso)
        addField(advancedBody, "Phone Type", profile.phoneType)
        addField(advancedBody, "Network Type", profile.networkType)
        addField(advancedBody, "Data Network Type", profile.dataNetworkType)
        addField(advancedBody, "Voice Network Type", profile.voiceNetworkType)
        parent.addView(advancedBody)
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
                if (!allowBuildPropAction()) return@setOnClickListener
                applyBuildPropPreset(BuildPropPresetRepository.presets.random())
            }
        })
        header.addView(iconButton(R.drawable.ic_mask_edit, true).apply {
            setOnClickListener {
                if (!allowBuildPropAction() || buildPropChooserOpen) return@setOnClickListener
                showBuildPropChooser()
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
            "IMEI", "IMEI 1" -> generated.imei
            "IMEI 2" -> generated.imei2
            "MEID" -> generated.meid
            "Android ID" -> generated.androidId
            "Google Services Framework ID" -> generated.gsfId
            "Advertising ID" -> generated.advertisingId
            "Limit Ad Tracking" -> generated.limitAdTrackingEnabled
            "Google Account Email" -> generated.googleAccountEmail
            "Device Name" -> generated.deviceName
            "MediaDrm ID" -> generated.mediaDrmId
            "Serial" -> generated.serial
            "Hardware ID" -> generated.hardwareId
            "MAC Address" -> generated.macAddress
            "MAC BSSID" -> generated.macBssid
            "MAC SSID" -> generated.macSsid
            "Bluetooth MAC" -> generated.bluetoothMac
            "Bluetooth Name" -> generated.bluetoothName
            "SIM Serial ID" -> generated.simSerialId
            "Subscriber ID / IMSI" -> generated.subscriberId
            "SIM Sub IDs" -> generated.simSubIds
            "Mobile No" -> generated.mobileNo
            "SIM Operator" -> generated.simOperator
            "Network Operator" -> generated.networkOperator
            "SIM Operator Name" -> generated.simOperatorName
            "Network Operator Name" -> generated.networkOperatorName
            "SIM Country ISO" -> generated.simCountryIso
            "Network Country ISO" -> generated.networkCountryIso
            "Phone Type" -> generated.phoneType
            "Network Type" -> generated.networkType
            "Data Network Type" -> generated.dataNetworkType
            "Voice Network Type" -> generated.voiceNetworkType
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
        set("IMEI 1", generated.imei)
        set("IMEI 2", generated.imei2)
        set("MEID", generated.meid)
        set("Android ID", generated.androidId)
        set("Google Services Framework ID", generated.gsfId)
        set("Advertising ID", generated.advertisingId)
        set("Limit Ad Tracking", generated.limitAdTrackingEnabled)
        set("Google Account Email", generated.googleAccountEmail)
        set("MediaDrm ID", generated.mediaDrmId)
        set("Serial", generated.serial)
        set("Hardware ID", generated.hardwareId)
        set("MAC Address", generated.macAddress)
        set("MAC BSSID", generated.macBssid)
        set("MAC SSID", generated.macSsid)
        set("Bluetooth MAC", generated.bluetoothMac)
        set("SIM Serial ID", generated.simSerialId)
        set("Subscriber ID / IMSI", generated.subscriberId)
        set("SIM Sub IDs", generated.simSubIds)
        set("Mobile No", generated.mobileNo)
        set("SIM Operator", generated.simOperator)
        set("Network Operator", generated.networkOperator)
        set("SIM Operator Name", generated.simOperatorName)
        set("Network Operator Name", generated.networkOperatorName)
        set("SIM Country ISO", generated.simCountryIso)
        set("Network Country ISO", generated.networkCountryIso)
        set("Phone Type", generated.phoneType)
        set("Network Type", generated.networkType)
        set("Data Network Type", generated.dataNetworkType)
        set("Voice Network Type", generated.voiceNetworkType)
        if (buildRow.enabled) {
            applyBuildPropPreset(BuildPropPresetRepository.presets.random())
        }
        toast("Active fields generated")
    }

    private fun syncNamesWithBuildModel(force: Boolean) {
        val model = parseBuildProp(buildRow.value.text.toString(), buildProfile).model.ifBlank { buildProfile.model }.ifBlank { "Android" }
        listOf("Device Name", "Bluetooth Name").forEach { label ->
            val row = fieldRows[label] ?: return@forEach
            if (force || row.value.text.toString().trim().isBlank()) {
                row.value.setText(model)
            }
        }
    }

    private fun applyBuildPropPreset(preset: BuildPropPreset) {
        buildProfile = preset.toProfile()
        buildRow.value.setText(preset.fingerprint)
        syncNamesWithBuildModel(force = true)
    }

    private fun showBuildPropChooser() {
        buildPropChooserOpen = true
        val grouped = BuildPropPresetRepository.grouped
        val presets = BuildPropPresetRepository.presets
        var selected = presets.firstOrNull { it.fingerprint == buildRow.value.text.toString().trim() } ?: presets.first()
        var openPopup: PopupWindow? = null
        val dialog = Dialog(context)
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 16)
            setBackgroundColor(dialogBg)
        }
        box.addView(TextView(context).apply {
            text = "Build Prop Chooser"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            setPadding(0, 0, 0, 14)
        })
        val familyField = chooserField()
        val versionField = chooserField()
        val modelField = chooserField()
        val preview = TextView(context).apply {
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextColor(muted)
            setPadding(0, 12, 0, 6)
            setSingleLine(false)
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
        }
        fun label(text: String) = TextView(context).apply {
            this.text = text
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            setPadding(0, 10, 0, 5)
        }
        box.addView(label("Brand / Family"))
        box.addView(familyField)
        box.addView(label("Android Version"))
        box.addView(versionField)
        box.addView(label("Model"))
        box.addView(modelField)
        box.addView(label("Preview fingerprint"))
        box.addView(preview)

        fun families(): List<String> = grouped.keys.toList()
        fun versions(family: String): List<String> = grouped[family]?.keys?.toList().orEmpty()
        fun modelPresets(family: String, version: String): List<BuildPropPreset> = grouped[family]?.get(version).orEmpty()
        fun models(family: String, version: String): List<String> = modelPresets(family, version).map { it.modelLabel }
        fun modelPreset(family: String, version: String, modelLabel: String): BuildPropPreset =
            modelPresets(family, version).firstOrNull { it.modelLabel == modelLabel }
                ?: modelPresets(family, version).first()
        fun renderSelection() {
            familyField.text = selected.family
            versionField.text = selected.androidVersionLabel
            modelField.text = selected.modelLabel
            preview.text = selected.fingerprint
        }
        fun selectFamily(family: String) {
            val version = versions(family).firstOrNull() ?: return
            selected = modelPresets(family, version).firstOrNull() ?: selected
            renderSelection()
        }
        fun selectVersion(version: String) {
            selected = modelPresets(selected.family, version).firstOrNull() ?: selected
            renderSelection()
        }
        fun showMenu(anchor: TextView, items: List<String>, onPick: (String) -> Unit) {
            openPopup?.dismiss()
            val list = ListView(context).apply {
                setBackgroundColor(panelBg)
                divider = android.graphics.drawable.ColorDrawable(line)
                dividerHeight = 1
                adapter = object : android.widget.BaseAdapter() {
                    override fun getCount(): Int = items.size
                    override fun getItem(position: Int): String = items[position]
                    override fun getItemId(position: Int): Long = position.toLong()
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                        return ((convertView as? TextView) ?: TextView(context)).apply {
                            text = items[position]
                            textSize = 15f
                            setTextColor(textColor)
                            gravity = Gravity.CENTER_VERTICAL
                            minHeight = 52
                            setPadding(14, 0, 14, 0)
                            setBackgroundColor(panelBg)
                        }
                    }
                }
                setOnItemClickListener { _, _, position, _ ->
                    onPick(items[position])
                    openPopup?.dismiss()
                }
            }
            openPopup = PopupWindow(list, anchor.width.coerceAtLeast(280), ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(panelBg))
                isOutsideTouchable = true
                elevation = 0f
                showAsDropDown(anchor, 0, 4)
            }
        }
        renderSelection()
        familyField.setOnClickListener {
            Log.d("BUILD_PROP_CHOOSER", "Family dropdown clicked")
            showMenu(familyField, families()) { selectFamily(it) }
        }
        versionField.setOnClickListener {
            Log.d("BUILD_PROP_CHOOSER", "Android dropdown clicked")
            Log.d("BUILD_PROP_CHOOSER", "expandedAndroid=true")
            showMenu(versionField, versions(selected.family)) { selectVersion(it) }
        }
        modelField.setOnClickListener {
            Log.d("BUILD_PROP_CHOOSER", "Model dropdown clicked")
            showMenu(modelField, models(selected.family, selected.androidVersionLabel)) {
                selected = modelPreset(selected.family, selected.androidVersionLabel, it)
                renderSelection()
            }
        }
        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 14, 0, 0)
        }
        buttons.addView(dialogButton("Random").apply {
            setOnClickListener {
                if (!allowBuildPropAction()) return@setOnClickListener
                selected = presets.random()
                renderSelection()
            }
        }, LinearLayout.LayoutParams(0, 48, 1f).apply { setMargins(0, 0, 6, 0) })
        buttons.addView(dialogButton("Cancel").apply {
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(0, 48, 1f).apply { setMargins(6, 0, 6, 0) })
        buttons.addView(dialogButton("Use This").apply {
            setOnClickListener {
                if (!allowBuildPropAction()) return@setOnClickListener
                applyBuildPropPreset(selected)
                dialog.dismiss()
            }
        }, LinearLayout.LayoutParams(0, 48, 1f).apply { setMargins(6, 0, 0, 0) })
        box.addView(buttons)
        dialog.setContentView(box)
        dialog.setOnDismissListener {
            openPopup?.dismiss()
            buildPropChooserOpen = false
        }
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(dialogBg))
        dialog.show()
    }

    private fun allowBuildPropAction(): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastBuildPropActionAt < 400L) return false
        lastBuildPropActionAt = now
        return true
    }

    private fun chooserField(): TextView = TextView(context).apply {
        textSize = 15f
        setTextColor(textColor)
        gravity = Gravity.CENTER_VERTICAL
        minHeight = 52
        setPadding(14, 0, 14, 0)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(panelBg)
            setStroke(1, line)
            cornerRadius = 5f
        }
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
        repeat(if (label == "IMEI" || label == "IMEI 1" || label == "IMEI 2") 5000 else 60) {
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
            "IMEI", "IMEI 1" -> generated.imei.value
            "IMEI 2" -> generated.imei2.value
            "MEID" -> generated.meid.value
            "Android ID" -> generated.androidId.value
            "Google Services Framework ID" -> generated.gsfId.value
            "Advertising ID" -> generated.advertisingId.value
            "Limit Ad Tracking" -> generated.limitAdTrackingEnabled.value
            "Google Account Email" -> generated.googleAccountEmail.value
            "Device Name" -> generated.deviceName.value
            "MediaDrm ID" -> generated.mediaDrmId.value
            "Serial" -> generated.serial.value
            "Hardware ID" -> generated.hardwareId.value
            "MAC Address" -> generated.macAddress.value
            "MAC BSSID" -> generated.macBssid.value
            "Bluetooth MAC" -> generated.bluetoothMac.value
            "Bluetooth Name" -> generated.bluetoothName.value
            "SIM Serial ID" -> generated.simSerialId.value
            "Subscriber ID / IMSI" -> generated.subscriberId.value
            "Mobile No" -> generated.mobileNo.value
            "SIM Operator" -> generated.simOperator.value
            "Network Operator" -> generated.networkOperator.value
            "SIM Operator Name" -> generated.simOperatorName.value
            "Network Operator Name" -> generated.networkOperatorName.value
            "SIM Country ISO" -> generated.simCountryIso.value
            "Network Country ISO" -> generated.networkCountryIso.value
            "Phone Type" -> generated.phoneType.value
            "Network Type" -> generated.networkType.value
            "Data Network Type" -> generated.dataNetworkType.value
            "Voice Network Type" -> generated.voiceNetworkType.value
            else -> null
        }
    }

    private fun currentProfile(): IdentityProfile {
        fun field(label: String): IdentityFieldState {
            val row = fieldRows[label]
            return IdentityFieldState(row?.value?.text?.toString()?.trim().orEmpty(), row?.enabled == true)
        }
        val parsedBuild = parseBuildProp(buildRow.value.text.toString(), buildProfile)
        val buildModel = parsedBuild.model.ifBlank { "Android" }
        fun fieldWithFallback(label: String, fallback: String): IdentityFieldState {
            val row = fieldRows[label]
            val raw = row?.value?.text?.toString()?.trim().orEmpty()
            val knownPresetModel = BuildPropPresetRepository.presets.any {
                it.model.equals(raw, ignoreCase = true) && !it.model.equals(fallback, ignoreCase = true)
            }
            return IdentityFieldState(if (raw.isBlank() || knownPresetModel) fallback else raw, row?.enabled == true)
        }
        val deviceName = fieldWithFallback("Device Name", buildModel)
        val bluetoothName = fieldWithFallback("Bluetooth Name", deviceName.value.ifBlank { buildModel })
        return IdentityProfile(
            name = viewModel.identityRepository.getProfile().name.ifBlank { "Default Profile" },
            imei = field("IMEI 1"),
            imei2 = field("IMEI 2"),
            meid = field("MEID"),
            deviceName = deviceName,
            serial = field("Serial"),
            hardwareId = field("Hardware ID"),
            macAddress = field("MAC Address"),
            macBssid = field("MAC BSSID"),
            macSsid = field("MAC SSID"),
            bluetoothMac = field("Bluetooth MAC"),
            bluetoothName = bluetoothName,
            androidId = field("Android ID"),
            subscriberId = field("Subscriber ID / IMSI"),
            subscriberId2 = field("Subscriber ID / IMSI"),
            simSerialId = field("SIM Serial ID"),
            simSubIds = field("SIM Sub IDs"),
            mobileNo = field("Mobile No"),
            mediaDrmId = field("MediaDrm ID"),
            simOperator = field("SIM Operator"),
            networkOperator = field("Network Operator"),
            simOperatorName = field("SIM Operator Name"),
            networkOperatorName = field("Network Operator Name"),
            simCountryIso = field("SIM Country ISO"),
            networkCountryIso = field("Network Country ISO"),
            phoneType = field("Phone Type"),
            networkType = field("Network Type"),
            dataNetworkType = field("Data Network Type"),
            voiceNetworkType = field("Voice Network Type"),
            gsfId = field("Google Services Framework ID"),
            advertisingId = field("Advertising ID"),
            limitAdTrackingEnabled = field("Limit Ad Tracking"),
            googleAccountEmail = field("Google Account Email"),
            buildProp = parsedBuild,
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
        layoutParams = LinearLayout.LayoutParams(38, 34).apply { setMargins(8, 0, 0, 0) }
        setPadding(7, 5, 7, 5)
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
            "IMEI", "IMEI 1", "IMEI 2" -> if (value.matches(Regex("[0-9]{15}")) && luhnValid(value)) null else "$label harus 15 digit dan lolos Luhn checksum."
            "MEID" -> if (value.matches(Regex("[0-9A-Fa-f]{14}"))) null else "MEID harus 14 karakter hex."
            "Android ID" -> if (value.matches(Regex("[0-9a-fA-F]{16}"))) null else "Android ID harus 16 karakter hex."
            "Google Services Framework ID" -> if (value.matches(Regex("[0-9a-fA-F]{16}"))) null else "Google Services Framework ID harus 16 karakter hex."
            "Advertising ID" -> if (value.matches(Regex("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))) null else "Advertising ID harus UUID valid."
            "Limit Ad Tracking" -> if (value.equals("true", true) || value.equals("false", true)) null else "Limit Ad Tracking harus true atau false."
            "Google Account Email" -> if (value.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) null else "Google Account Email harus format email valid."
            "Device Name" -> if (value.isNotBlank()) null else "Device Name tidak boleh kosong."
            "MediaDrm ID" -> if (value.matches(Regex("[0-9a-fA-F]{16,64}"))) null else "MediaDrm ID harus hex dan tidak kosong."
            "Serial" -> if (value.isNotBlank()) null else "Serial tidak boleh kosong."
            "Hardware ID" -> if (value.isNotBlank()) null else "Hardware ID tidak boleh kosong."
            "MAC Address", "MAC BSSID", "Bluetooth MAC" -> if (value.matches(Regex("(?i)^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))) null else "$label harus format XX:XX:XX:XX:XX:XX."
            "Bluetooth Name" -> if (value.trim().length in 1..64 && value.none { it.isISOControl() }) null else "Bluetooth Name tidak boleh kosong."
            "SIM Serial ID" -> if (value.matches(Regex("[0-9]{19,20}"))) null else "SIM Serial ID harus 19-20 digit."
            "Subscriber ID / IMSI", "SIM Sub IDs" -> if (value.matches(Regex("[0-9]{14,16}"))) null else "$label harus 14-16 digit numerik."
            "Mobile No" -> if (value.matches(Regex("\\+?[0-9]{8,15}"))) null else "Mobile No hanya boleh angka dan tanda + dengan panjang wajar."
            "SIM Operator", "Network Operator" -> if (value.matches(Regex("[0-9]{5,6}"))) null else "$label harus MCC+MNC numerik, contoh 51010."
            "SIM Operator Name", "Network Operator Name" -> if (value.isNotBlank()) null else "$label tidak boleh kosong."
            "SIM Country ISO", "Network Country ISO" -> if (value.matches(Regex("[a-z]{2}"))) null else "$label harus ISO 2 huruf kecil, contoh id."
            "Phone Type" -> if (value.toIntOrNull() in setOf(0, 1, 2)) null else "Phone Type harus integer valid."
            "Network Type", "Data Network Type", "Voice Network Type" -> if (value.toIntOrNull() in setOf(0, 1, 2, 3, 8, 9, 10, 13, 15, 20)) null else "$label harus integer network type valid."
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

    private fun parseBuildProp(raw: String, fallback: BuildPropProfile): BuildPropProfile {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return fallback
        BuildPropPresetRepository.findByFingerprint(trimmed)?.let { return it }
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
