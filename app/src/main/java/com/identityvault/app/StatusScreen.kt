package com.identityvault.app

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.identityvault.app.data.BuildPropProfile
import com.identityvault.app.data.IdentityFieldState
import com.identityvault.app.data.IdentityProfile
import com.identityvault.app.detector.DetectorBottomSheet
import com.identityvault.app.identity.IdentityValidator
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

    private val bg = Color.rgb(12, 16, 24)
    private val panelBg = Color.rgb(18, 24, 34)
    private val rowStroke = Color.rgb(39, 48, 62)
    private val textColor = Color.rgb(226, 232, 240)
    private val muted = Color.rgb(145, 158, 176)
    private val quiet = Color.rgb(96, 110, 128)
    private val accent = Color.rgb(45, 190, 176)
    private val disabled = Color.rgb(86, 98, 116)

    private lateinit var root: LinearLayout
    private val fieldRows = linkedMapOf<String, FieldRow>()
    private lateinit var profileName: EditText
    private var buildProfile: BuildPropProfile = BuildPropProfile.default()
    private var buildEnabled: Boolean = true

    fun create(): View {
        val scroll = ScrollView(context).apply { setBackgroundColor(bg) }
        root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 22, 24)
            setBackgroundColor(bg)
        }
        scroll.addView(root)
        render()
        return scroll
    }

    fun render() {
        root.removeAllViews()
        fieldRows.clear()
        topBar()
        profilePanel(viewModel.identityRepository.getProfile())
    }

    private fun topBar() {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(TextView(context).apply {
            text = "IdentityVault"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        titleBox.addView(TextView(context).apply {
            text = "Identity profile testing module"
            textSize = 12f
            setTextColor(muted)
        })
        row.addView(titleBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(outlineButton("Detector").apply { setOnClickListener { DetectorBottomSheet(context).show() } })
        row.addView(outlineButton("...").apply { setOnClickListener { callbacks.onShowMenu(this) } })
        root.addView(row)
    }

    private fun profilePanel(profile: IdentityProfile) {
        buildProfile = profile.buildProp
        buildEnabled = profile.buildPropEnabled
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 16)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(panelBg)
                cornerRadius = 8f
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 16, 0, 0)
            }
        }
        profileName = EditText(context).apply {
            hint = "Profile name"
            setText(profile.name)
            setSingleLine(true)
            textSize = 14f
            setTextColor(textColor)
            setHintTextColor(quiet)
            backgroundTintList = android.content.res.ColorStateList.valueOf(rowStroke)
        }
        panel.addView(profileName)

        addField(panel, "IMEI", profile.imei)
        addField(panel, "Android ID", profile.androidId)
        addField(panel, "GSF ID", profile.gsfId)
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
        addBuildPropRow(panel)
        addBottomButtons(panel)
        root.addView(panel)
    }

    private fun addField(parent: LinearLayout, label: String, field: IdentityFieldState) {
        val box = fieldBox()
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(context).apply {
            text = label
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val random = iconButton("R", true).apply { setOnClickListener { randomizeField(label) } }
        val enable = iconButton("P", field.enabled)
        var enabled = field.enabled
        enable.setOnClickListener {
            enabled = !enabled
            styleIcon(enable, enabled)
            fieldRows[label] = fieldRows[label]!!.copy(enabled = enabled)
        }
        header.addView(random)
        header.addView(enable)
        val value = valueText(field.value)
        box.addView(header)
        box.addView(value)
        parent.addView(box)
        fieldRows[label] = FieldRow(label, value, enabled, enable)
    }

    private fun addBuildPropRow(parent: LinearLayout) {
        val box = fieldBox()
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(context).apply {
            text = "Build Prop"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(iconButton("D", true).apply { setOnClickListener { showBuildPropDialog(openPreset = true) } })
        header.addView(iconButton("T", true).apply { setOnClickListener { showBuildPropDialog(openPreset = false) } })
        val enable = iconButton("P", buildEnabled).apply {
            setOnClickListener {
                buildEnabled = !buildEnabled
                styleIcon(this, buildEnabled)
            }
        }
        header.addView(enable)
        val summary = valueText("${buildProfile.brand} ${buildProfile.model} / Android ${buildProfile.versionRelease}")
        box.setOnClickListener { showBuildPropDialog(openPreset = true) }
        box.addView(header)
        box.addView(summary)
        parent.addView(box)
    }

    private fun addBottomButtons(parent: LinearLayout) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 14, 0, 0)
        }
        row.addView(primaryButton("Generate All").apply { setOnClickListener { generateAllActive() } }, LinearLayout.LayoutParams(0, 42, 1f))
        row.addView(primaryButton("Save").apply { setOnClickListener { saveProfile() } }, LinearLayout.LayoutParams(0, 42, 1f))
        row.addView(primaryButton("Apply").apply { setOnClickListener { applyProfile() } }, LinearLayout.LayoutParams(0, 42, 1f))
        parent.addView(row)
    }

    private fun randomizeField(label: String) {
        val generated = viewModel.generateProfileObject()
        val state = when (label) {
            "IMEI" -> generated.imei
            "Android ID" -> generated.androidId
            "GSF ID" -> generated.gsfId
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
        fieldRows[label]?.value?.text = state.value
    }

    private fun generateAllActive() {
        val generated = viewModel.generateProfileObject()
        fun set(label: String, state: IdentityFieldState) {
            val row = fieldRows[label] ?: return
            if (row.enabled) row.value.text = state.value
        }
        set("IMEI", generated.imei)
        set("Android ID", generated.androidId)
        set("GSF ID", generated.gsfId)
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
        if (buildEnabled) buildProfile = generated.buildProp
        saveProfile(showToast = false)
        toast("Active fields generated")
        render()
    }

    private fun showBuildPropDialog(openPreset: Boolean) {
        var selected = buildProfile
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(26, 8, 26, 0)
        }
        val presetTitle = TextView(context).apply {
            text = "Preset"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 41, 59))
        }
        container.addView(presetTitle)
        presets().forEach { preset ->
            container.addView(Button(context).apply {
                text = "${preset.brand} ${preset.model} Android ${preset.versionRelease}"
                setAllCaps(false)
                setOnClickListener {
                    selected = preset
                    toast("Preset selected")
                }
            })
        }
        val pasteTitle = TextView(context).apply {
            text = "Paste Manual"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 41, 59))
            setPadding(0, 12, 0, 0)
        }
        val paste = EditText(context).apply {
            minLines = 7
            maxLines = 10
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hint = "ro.build.fingerprint=...\nro.product.brand=...\nro.product.model=..."
            setText(if (openPreset) "" else buildToText(buildProfile))
        }
        container.addView(pasteTitle)
        container.addView(paste)
        AlertDialog.Builder(context)
            .setTitle("Build Prop")
            .setView(ScrollView(context).apply { addView(container) })
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val parsed = parseBuildProp(paste.text.toString(), selected)
                buildProfile = parsed
                saveProfile(showToast = false)
                render()
            }
            .show()
    }

    private fun saveProfile(showToast: Boolean = true): Boolean {
        val profile = currentProfile()
        val errors = IdentityValidator().validate(profile)
        if (errors.isNotEmpty()) {
            AlertDialog.Builder(context)
                .setTitle("Profile invalid")
                .setMessage(errors.entries.joinToString("\n") { "${it.key}: ${it.value}" })
                .setPositiveButton("OK", null)
                .show()
            return false
        }
        viewModel.identityRepository.saveProfile(profile)
        viewModel.markProfileDirty()
        if (showToast) toast("Profile saved")
        return true
    }

    private fun applyProfile() {
        if (!saveProfile(showToast = false)) return
        viewModel.applyProfile()
        toast("Profile applied. Force stop dan buka ulang app target jika perlu.")
        render()
    }

    private fun currentProfile(): IdentityProfile {
        fun field(label: String): IdentityFieldState {
            val row = fieldRows[label]
            return IdentityFieldState(row?.value?.text?.toString()?.trim().orEmpty(), row?.enabled == true)
        }
        return IdentityProfile(
            name = profileName.text.toString().trim().ifBlank { "Default Profile" },
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
            gsfId = field("GSF ID"),
            buildProp = buildProfile,
            buildPropEnabled = buildEnabled,
            draft = false
        )
    }

    fun showLogs() {
        val logs = viewModel.logRepository.getLogs()
        AlertDialog.Builder(context)
            .setTitle("Log")
            .setMessage(if (logs.isEmpty()) "Belum ada log" else logs.joinToString("\n\n"))
            .setNegativeButton("Clear") { _, _ -> viewModel.logRepository.clear() }
            .setPositiveButton("Close", null)
            .show()
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

    private fun fieldBox(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 10, 0, 10)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(1, rowStroke)
            cornerRadius = 6f
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 8, 0, 0)
        }
        setPadding(12, 9, 12, 9)
    }

    private fun valueText(value: String): TextView = TextView(context).apply {
        text = value
        textSize = 13f
        setTextColor(muted)
        setSingleLine(true)
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 4, 0, 0)
    }

    private fun iconButton(label: String, active: Boolean): TextView = TextView(context).apply {
        text = label
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        styleIcon(this, active)
        layoutParams = LinearLayout.LayoutParams(34, 30).apply { setMargins(6, 0, 0, 0) }
    }

    private fun styleIcon(view: TextView, active: Boolean) {
        view.setTextColor(if (active) accent else disabled)
        view.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(15, 20, 30))
            setStroke(1, if (active) Color.rgb(35, 118, 110) else Color.rgb(48, 58, 72))
            cornerRadius = 6f
        }
    }

    private fun outlineButton(label: String): Button = Button(context).apply {
        text = label
        textSize = 12f
        setAllCaps(false)
        minHeight = 0
        minWidth = 0
        setPadding(12, 5, 12, 5)
        setTextColor(accent)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(1, Color.rgb(35, 92, 91))
            cornerRadius = 6f
        }
    }

    private fun primaryButton(label: String): Button = Button(context).apply {
        text = label
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setAllCaps(false)
        setTextColor(Color.rgb(5, 18, 22))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(accent)
            cornerRadius = 7f
        }
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

    private fun buildToText(build: BuildPropProfile): String = listOf(
        "ro.build.fingerprint=${build.fingerprint}",
        "ro.product.brand=${build.brand}",
        "ro.product.model=${build.model}",
        "ro.product.manufacturer=${build.manufacturer}",
        "ro.product.device=${build.device}",
        "ro.product.name=${build.name}",
        "ro.product.board=${build.board}",
        "ro.hardware=${build.hardware}",
        "ro.build.id=${build.buildId}",
        "ro.build.display.id=${build.displayId}",
        "ro.build.version.release=${build.versionRelease}",
        "ro.build.version.sdk=${build.versionSdk}",
        "ro.build.version.security_patch=${build.securityPatch}"
    ).joinToString("\n")

    private fun parseBuildProp(raw: String, fallback: BuildPropProfile): BuildPropProfile {
        if (raw.isBlank()) return fallback
        val map = raw.lineSequence()
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

    private data class FieldRow(
        val label: String,
        val value: TextView,
        val enabled: Boolean,
        val enableView: TextView
    )
}
