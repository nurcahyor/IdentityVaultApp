package com.identityvault.app

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.text.TextUtils
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

    private val bg = Color.rgb(13, 17, 24)
    private val panelBg = Color.rgb(19, 24, 32)
    private val bottomBg = Color.rgb(15, 20, 28)
    private val line = Color.rgb(36, 44, 56)
    private val textColor = Color.rgb(225, 231, 239)
    private val muted = Color.rgb(142, 153, 168)
    private val quiet = Color.rgb(96, 108, 124)
    private val accent = Color.rgb(69, 178, 166)
    private val iconIdle = Color.rgb(118, 130, 146)

    private lateinit var content: LinearLayout
    private val fieldRows = linkedMapOf<String, FieldRow>()
    private var buildProfile: BuildPropProfile = BuildPropProfile.default()
    private var buildEnabled = true

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
            setPadding(18, 16, 18, 16)
            setBackgroundColor(bg)
        }
        scroll.addView(content)
        shell.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        shell.addView(bottomActions())
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
            text = "Identity profile testing module"
            textSize = 11f
            setTextColor(muted)
        })
        row.addView(titleBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(topButton("Detector").apply { setOnClickListener { DetectorBottomSheet(context).show() } })
        row.addView(topButton("...").apply { setOnClickListener { callbacks.onShowMenu(this) } })
        content.addView(row)
    }

    private fun fieldList(profile: IdentityProfile) {
        buildProfile = profile.buildProp
        buildEnabled = profile.buildPropEnabled
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
        addBuildProp(panel)
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
        var enabled = field.enabled
        val toggle = iconButton(R.drawable.ic_power, enabled)
        toggle.setOnClickListener {
            enabled = !enabled
            styleIcon(toggle, enabled)
            fieldRows[label] = fieldRows[label]!!.copy(enabled = enabled)
        }
        header.addView(toggle)
        val value = valueView(field.value)
        box.addView(header)
        box.addView(value)
        parent.addView(box)
        fieldRows[label] = FieldRow(value, enabled)
    }

    private fun addBuildProp(parent: LinearLayout) {
        val box = rowBox().apply { setOnClickListener { showBuildPropDialog() } }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(labelView("Build Prop"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(iconButton(R.drawable.ic_preset, true).apply { setOnClickListener { showBuildPropDialog() } })
        header.addView(iconButton(R.drawable.ic_paste, true).apply { setOnClickListener { showBuildPropDialog() } })
        val toggle = iconButton(R.drawable.ic_power, buildEnabled)
        toggle.setOnClickListener {
            buildEnabled = !buildEnabled
            styleIcon(toggle, buildEnabled)
        }
        header.addView(toggle)
        box.addView(header)
        box.addView(valueView(buildProfile.fingerprint))
        box.addView(TextView(context).apply {
            text = "Android ${buildProfile.versionRelease}"
            textSize = 12f
            setTextColor(quiet)
            setPadding(0, 2, 0, 0)
        })
        parent.addView(box)
    }

    private fun bottomActions(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(14, 10, 14, 12)
        setBackgroundColor(bottomBg)
        addView(actionButton("Generate All").apply { setOnClickListener { generateAllActive() } }, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(0, 0, 7, 0) })
        addView(actionButton("Save").apply { setOnClickListener { saveProfile() } }, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(7, 0, 7, 0) })
        addView(actionButton("Apply").apply { setOnClickListener { applyProfile() } }, LinearLayout.LayoutParams(0, 42, 1f).apply { setMargins(7, 0, 0, 0) })
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

    private fun showBuildPropDialog() {
        var selected = buildProfile
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 4, 24, 0)
        }
        container.addView(dialogTitle("Preset"))
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
        container.addView(dialogTitle("Paste Manual").apply { setPadding(0, 12, 0, 4) })
        val paste = EditText(context).apply {
            minLines = 5
            maxLines = 8
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hint = "Paste fingerprint or build.prop block"
        }
        container.addView(paste)
        AlertDialog.Builder(context)
            .setTitle("Build Prop")
            .setView(ScrollView(context).apply { addView(container) })
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                buildProfile = parseBuildProp(paste.text.toString(), selected)
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

    private fun rowBox(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(10, 8, 10, 8)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(1, line)
            cornerRadius = 5f
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

    private fun valueView(value: String): TextView = TextView(context).apply {
        text = value
        textSize = 12.5f
        setTextColor(muted)
        setSingleLine(true)
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 3, 0, 0)
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

    private fun actionButton(label: String): Button = Button(context).apply {
        text = label
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setAllCaps(false)
        setTextColor(textColor)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(25, 33, 44))
            setStroke(1, Color.rgb(47, 59, 73))
            cornerRadius = 6f
        }
    }

    private fun dialogTitle(text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(30, 41, 59))
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

    private data class FieldRow(val value: TextView, val enabled: Boolean)
}
