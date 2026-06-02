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
import android.widget.Switch
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

    private val bg = Color.rgb(11, 15, 25)
    private val panelBg = Color.rgb(17, 24, 39)
    private val panelStroke = Color.rgb(45, 55, 72)
    private val textColor = Color.rgb(226, 232, 240)
    private val muted = Color.rgb(148, 163, 184)
    private val accent = Color.rgb(45, 212, 191)
    private val darkAccent = Color.rgb(15, 118, 110)

    private lateinit var root: LinearLayout
    private val switches = linkedMapOf<String, Switch>()
    private val values = linkedMapOf<String, TextView>()
    private val buildInputs = linkedMapOf<String, EditText>()
    private lateinit var buildSwitch: Switch
    private var buildProfile: BuildPropProfile = BuildPropProfile.default()

    fun create(): View {
        val scroll = ScrollView(context).apply { setBackgroundColor(bg) }
        root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 32)
            setBackgroundColor(bg)
        }
        scroll.addView(root)
        render()
        return scroll
    }

    fun render() {
        root.removeAllViews()
        switches.clear()
        values.clear()
        topBar()
        statusStrip()
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
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        titleBox.addView(TextView(context).apply {
            text = "Identity profile testing module"
            textSize = 13f
            setTextColor(muted)
        })
        row.addView(titleBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(smallButton("Detector").apply { setOnClickListener { DetectorBottomSheet(context).show() } })
        row.addView(smallButton("...").apply { setOnClickListener { callbacks.onShowMenu(this) } })
        root.addView(row)
    }

    private fun statusStrip() {
        viewModel.refresh()
        val rootStatus = if (viewModel.rootStatus.granted) "Root: Granted" else "Root: ${if (viewModel.rootStatus.available) "Available" else "Clean"}"
        val lsposed = if (viewModel.lsposedStatus.hookActive) "LSPosed: Active" else if (viewModel.lsposedStatus.installed) "LSPosed: Ready" else "LSPosed: Missing"
        val applied = if (viewModel.profileApplied) "Profile: Applied" else "Profile: Not applied"
        root.addView(TextView(context).apply {
            text = "$rootStatus   /   $lsposed   /   $applied"
            textSize = 12f
            setTextColor(muted)
            setPadding(0, 16, 0, 0)
        })
    }

    private fun profilePanel(profile: IdentityProfile) {
        buildProfile = profile.buildProp
        val panel = panel()
        addName(panel, profile.name)
        addField(panel, "IMEI", profile.imei)
        addField(panel, "Serial", profile.serial)
        addField(panel, "Hardware ID", profile.hardwareId)
        addField(panel, "MAC Address", profile.macAddress)
        addField(panel, "MAC BSSID", profile.macBssid)
        addField(panel, "MAC SSID", profile.macSsid)
        addField(panel, "Bluetooth MAC", profile.bluetoothMac)
        addField(panel, "Android ID", profile.androidId)
        addField(panel, "SIM Serial ID", profile.simSerialId)
        addField(panel, "SIM Sub IDs", profile.simSubIds)
        addField(panel, "Mobile No", profile.mobileNo)
        addField(panel, "MediaDrm ID", profile.mediaDrmId)
        addField(panel, "SIM Operator", profile.simOperator)
        addField(panel, "GSF ID", profile.gsfId)
        addBuildSummary(panel, profile)
        addBottomButtons(panel)
        root.addView(panel)
    }

    private fun addName(parent: LinearLayout, name: String) {
        val input = EditText(context).apply {
            hint = "Profile name"
            setText(name)
            setSingleLine(true)
            textSize = 15f
            setTextColor(textColor)
            setHintTextColor(Color.rgb(100, 116, 139))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(71, 85, 105))
        }
        parent.addView(input)
        buildInputs["profileName"] = input
    }

    private fun addField(parent: LinearLayout, label: String, field: IdentityFieldState) {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 14, 0, 0)
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(context).apply {
            text = label
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val sw = Switch(context).apply {
            isChecked = field.enabled
            showText = false
        }
        row.addView(sw)
        val value = TextView(context).apply {
            text = field.value
            textSize = 14f
            setTextColor(muted)
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 4, 0, 0)
        }
        box.addView(row)
        box.addView(value)
        parent.addView(box)
        switches[label] = sw
        values[label] = value
    }

    private fun addBuildSummary(parent: LinearLayout, profile: IdentityProfile) {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 18, 0, 0)
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(context).apply {
            text = "Build Prop"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        buildSwitch = Switch(context).apply {
            isChecked = profile.buildPropEnabled
            showText = false
        }
        row.addView(buildSwitch)
        val summary = TextView(context).apply {
            text = "${profile.buildProp.brand} ${profile.buildProp.model} / Android ${profile.buildProp.versionRelease}"
            textSize = 14f
            setTextColor(muted)
            setPadding(0, 4, 0, 0)
        }
        val fp = TextView(context).apply {
            text = "Fingerprint: ${profile.buildProp.fingerprint}"
            textSize = 13f
            setTextColor(Color.rgb(100, 116, 139))
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 2, 0, 8)
        }
        box.addView(row)
        box.addView(summary)
        box.addView(fp)
        box.addView(secondaryButton("Edit Build Prop").apply { setOnClickListener { showBuildEditor() } })
        parent.addView(box)
    }

    private fun addBottomButtons(parent: LinearLayout) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 22, 0, 0)
        }
        row.addView(primaryButton("GENERATE").apply { setOnClickListener { generateActiveFields() } }, LinearLayout.LayoutParams(0, 48, 1f))
        row.addView(primaryButton("SAVE").apply { setOnClickListener { saveProfile(false) } }, LinearLayout.LayoutParams(0, 48, 1f))
        row.addView(primaryButton("APPLY").apply { setOnClickListener { applyProfile() } }, LinearLayout.LayoutParams(0, 48, 1f))
        parent.addView(row)
    }

    private fun generateActiveFields() {
        val generated = viewModel.generateProfileObject()
        fun set(label: String, state: IdentityFieldState) {
            if (switches[label]?.isChecked == true) values[label]?.text = state.value
        }
        set("IMEI", generated.imei)
        set("Serial", generated.serial)
        set("Hardware ID", generated.hardwareId)
        set("MAC Address", generated.macAddress)
        set("MAC BSSID", generated.macBssid)
        set("MAC SSID", generated.macSsid)
        set("Bluetooth MAC", generated.bluetoothMac)
        set("Android ID", generated.androidId)
        set("SIM Serial ID", generated.simSerialId)
        set("SIM Sub IDs", generated.simSubIds)
        set("Mobile No", generated.mobileNo)
        set("MediaDrm ID", generated.mediaDrmId)
        set("SIM Operator", generated.simOperator)
        set("GSF ID", generated.gsfId)
        if (buildSwitch.isChecked) buildProfile = generated.buildProp
        toast("Field aktif digenerate")
        renderGeneratedWithoutLosingValues()
    }

    private fun renderGeneratedWithoutLosingValues() {
        val profile = currentProfile(false)
        viewModel.identityRepository.saveProfile(profile)
        render()
    }

    private fun showBuildEditor() {
        buildInputs.clear()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 10, 32, 0)
        }
        addBuild(container, "ro.build.fingerprint", buildProfile.fingerprint)
        addBuild(container, "ro.product.brand", buildProfile.brand)
        addBuild(container, "ro.product.model", buildProfile.model)
        addBuild(container, "ro.product.manufacturer", buildProfile.manufacturer)
        addBuild(container, "ro.product.device", buildProfile.device)
        addBuild(container, "ro.product.name", buildProfile.name)
        addBuild(container, "ro.product.board", buildProfile.board)
        addBuild(container, "ro.hardware", buildProfile.hardware)
        addBuild(container, "ro.build.id", buildProfile.buildId)
        addBuild(container, "ro.build.display.id", buildProfile.displayId)
        addBuild(container, "ro.build.version.release", buildProfile.versionRelease)
        addBuild(container, "ro.build.version.sdk", buildProfile.versionSdk)
        addBuild(container, "ro.build.version.security_patch", buildProfile.securityPatch)
        AlertDialog.Builder(context)
            .setTitle("Edit Build Prop")
            .setView(ScrollView(context).apply { addView(container) })
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                buildProfile = buildFromInputs()
                saveProfile(false)
                render()
            }
            .show()
    }

    private fun addBuild(parent: LinearLayout, key: String, value: String) {
        val input = EditText(context).apply {
            hint = key
            setText(value)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        parent.addView(input)
        buildInputs[key] = input
    }

    private fun saveProfile(draft: Boolean) {
        val profile = currentProfile(draft)
        val errors = IdentityValidator().validate(profile)
        if (errors.isNotEmpty()) {
            AlertDialog.Builder(context)
                .setTitle("Profile invalid")
                .setMessage(errors.entries.joinToString("\n") { "${it.key}: ${it.value}" })
                .setPositiveButton("OK", null)
                .show()
            return
        }
        viewModel.identityRepository.saveProfile(profile)
        viewModel.markProfileDirty()
        toast("Profile disimpan")
        render()
    }

    private fun applyProfile() {
        saveProfile(false)
        viewModel.applyProfile()
        toast("Profile diterapkan. Force stop dan buka ulang aplikasi target jika perlu.")
        render()
    }

    private fun currentProfile(draft: Boolean): IdentityProfile {
        fun field(label: String): IdentityFieldState = IdentityFieldState(
            value = values[label]?.text?.toString()?.trim().orEmpty(),
            enabled = switches[label]?.isChecked == true
        )
        return IdentityProfile(
            name = (buildInputs["profileName"]?.text?.toString()?.trim().orEmpty()).ifBlank { viewModel.identityRepository.getProfile().name },
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
            buildPropEnabled = buildSwitch.isChecked,
            draft = draft
        )
    }

    private fun buildFromInputs(): BuildPropProfile = BuildPropProfile(
        fingerprint = buildText("ro.build.fingerprint"),
        brand = buildText("ro.product.brand"),
        model = buildText("ro.product.model"),
        manufacturer = buildText("ro.product.manufacturer"),
        device = buildText("ro.product.device"),
        name = buildText("ro.product.name"),
        board = buildText("ro.product.board"),
        hardware = buildText("ro.hardware"),
        buildId = buildText("ro.build.id"),
        displayId = buildText("ro.build.display.id"),
        versionRelease = buildText("ro.build.version.release"),
        versionSdk = buildText("ro.build.version.sdk"),
        securityPatch = buildText("ro.build.version.security_patch")
    )

    private fun buildText(key: String): String = buildInputs[key]?.text?.toString()?.trim().orEmpty()

    fun showLogs() {
        val logs = viewModel.logRepository.getLogs()
        AlertDialog.Builder(context)
            .setTitle("Log")
            .setMessage(if (logs.isEmpty()) "Belum ada log" else logs.joinToString("\n\n"))
            .setNegativeButton("Clear") { _, _ -> viewModel.logRepository.clear() }
            .setPositiveButton("Close", null)
            .show()
    }

    fun resetProfile() {
        viewModel.generateProfile()
        toast("Profile direset")
        render()
    }

    private fun panel(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(22, 18, 22, 18)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(panelBg)
            cornerRadius = 12f
            setStroke(1, panelStroke)
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 18, 0, 0)
        }
    }

    private fun smallButton(label: String): Button = button(label, transparent = true)
    private fun secondaryButton(label: String): Button = button(label, transparent = true)
    private fun primaryButton(label: String): Button = button(label, transparent = false)

    private fun button(label: String, transparent: Boolean): Button = Button(context).apply {
        text = label
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        minHeight = 0
        minWidth = 0
        setPadding(14, 6, 14, 6)
        setTextColor(if (transparent) accent else Color.rgb(5, 15, 20))
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 18f
            setColor(if (transparent) Color.TRANSPARENT else accent)
            setStroke(1, if (transparent) darkAccent else accent)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
