package com.identityvault.app

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
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
import com.identityvault.app.identity.IdentityProfileGenerator
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
    private val accent = Color.rgb(56, 189, 248)

    private lateinit var root: LinearLayout
    private val fieldInputs = linkedMapOf<String, Pair<Switch, EditText>>()
    private val buildInputs = linkedMapOf<String, EditText>()
    private lateinit var buildSwitch: Switch
    private val generator = IdentityProfileGenerator()

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
        fieldInputs.clear()
        buildInputs.clear()
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
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        titleBox.addView(TextView(context).apply {
            text = "Identity profile testing module"
            textSize = 13f
            setTextColor(muted)
        })
        val detector = smallButton("Detector").apply {
            setOnClickListener { DetectorBottomSheet(context).show() }
        }
        val menu = smallButton("...").apply {
            textSize = 18f
            setOnClickListener { callbacks.onShowMenu(this) }
        }
        row.addView(titleBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(detector)
        row.addView(menu)
        root.addView(row)
    }

    private fun profilePanel(profile: IdentityProfile) {
        val panel = panel()
        panel.addView(sectionTitle("Identity Profile"))
        val nameInput = edit("Profile name", profile.name)
        buildInputs["profileName"] = nameInput
        panel.addView(nameInput)

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

        val buildHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 18, 0, 6)
        }
        buildHeader.addView(TextView(context).apply {
            text = "Build Prop"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        buildHeader.addView(smallButton("Gen").apply {
            setOnClickListener { applyBuild(generator.buildProp()) }
        })
        panel.addView(buildHeader)

        buildSwitch = Switch(context).apply {
            text = if (profile.buildPropEnabled) "Build Prop ON" else "Build Prop OFF"
            isChecked = profile.buildPropEnabled
            setTextColor(muted)
            setOnCheckedChangeListener { button, checked -> button.text = if (checked) "Build Prop ON" else "Build Prop OFF" }
        }
        panel.addView(buildSwitch)
        addBuild(panel, "ro.build.fingerprint", profile.buildProp.fingerprint)
        addBuild(panel, "ro.product.brand", profile.buildProp.brand)
        addBuild(panel, "ro.product.model", profile.buildProp.model)
        addBuild(panel, "ro.product.manufacturer", profile.buildProp.manufacturer)
        addBuild(panel, "ro.product.device", profile.buildProp.device)
        addBuild(panel, "ro.product.name", profile.buildProp.name)
        addBuild(panel, "ro.product.board", profile.buildProp.board)
        addBuild(panel, "ro.hardware", profile.buildProp.hardware)
        addBuild(panel, "ro.build.id", profile.buildProp.buildId)
        addBuild(panel, "ro.build.display.id", profile.buildProp.displayId)
        addBuild(panel, "ro.build.version.release", profile.buildProp.versionRelease)
        addBuild(panel, "ro.build.version.sdk", profile.buildProp.versionSdk)
        addBuild(panel, "ro.build.version.security_patch", profile.buildProp.securityPatch)

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 18, 0, 0)
        }
        buttons.addView(primaryButton("Generate All").apply {
            setOnClickListener {
                viewModel.generateProfile()
                render()
                toast("Profile baru dibuat")
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        buttons.addView(primaryButton("Save").apply {
            setOnClickListener { saveProfile(false) }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        buttons.addView(ghostButton("Draft").apply {
            setOnClickListener { saveProfile(true) }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        panel.addView(buttons)
        root.addView(panel)
    }

    private fun addField(parent: LinearLayout, label: String, field: IdentityFieldState) {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 10, 0, 0)
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val sw = Switch(context).apply {
            text = "$label ${if (field.enabled) "ON" else "OFF"}"
            isChecked = field.enabled
            setTextColor(muted)
            setOnCheckedChangeListener { button: CompoundButton, checked: Boolean ->
                button.text = "$label ${if (checked) "ON" else "OFF"}"
            }
        }
        val gen = smallButton("Gen").apply {
            setOnClickListener {
                fieldInputs[label]?.second?.setText(generator.fieldValue(label))
            }
        }
        row.addView(sw, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(gen)
        val input = edit(label, field.value)
        box.addView(row)
        box.addView(input)
        parent.addView(box)
        fieldInputs[label] = sw to input
    }

    private fun addBuild(parent: LinearLayout, label: String, value: String) {
        val input = edit(label, value).apply { textSize = 13f }
        parent.addView(input)
        buildInputs[label] = input
    }

    private fun applyBuild(build: BuildPropProfile) {
        buildInputs["ro.build.fingerprint"]?.setText(build.fingerprint)
        buildInputs["ro.product.brand"]?.setText(build.brand)
        buildInputs["ro.product.model"]?.setText(build.model)
        buildInputs["ro.product.manufacturer"]?.setText(build.manufacturer)
        buildInputs["ro.product.device"]?.setText(build.device)
        buildInputs["ro.product.name"]?.setText(build.name)
        buildInputs["ro.product.board"]?.setText(build.board)
        buildInputs["ro.hardware"]?.setText(build.hardware)
        buildInputs["ro.build.id"]?.setText(build.buildId)
        buildInputs["ro.build.display.id"]?.setText(build.displayId)
        buildInputs["ro.build.version.release"]?.setText(build.versionRelease)
        buildInputs["ro.build.version.sdk"]?.setText(build.versionSdk)
        buildInputs["ro.build.version.security_patch"]?.setText(build.securityPatch)
    }

    private fun saveProfile(draft: Boolean) {
        val profile = currentProfile(draft)
        val errors = IdentityValidator().validate(profile)
        if (errors.isNotEmpty() && !draft) {
            AlertDialog.Builder(context)
                .setTitle("Profile invalid")
                .setMessage(errors.entries.joinToString("\n") { "${it.key}: ${it.value}" })
                .setPositiveButton("OK", null)
                .show()
            return
        }
        viewModel.identityRepository.saveProfile(profile)
        viewModel.logRepository.add(if (draft) "Profile saved as draft" else "Profile saved")
        toast(if (draft) "Draft disimpan" else "Profile disimpan")
    }

    private fun currentProfile(draft: Boolean): IdentityProfile {
        fun field(label: String): IdentityFieldState {
            val pair = fieldInputs[label] ?: return IdentityFieldState("")
            return IdentityFieldState(pair.second.text.toString().trim(), pair.first.isChecked)
        }
        return IdentityProfile(
            name = buildInputs["profileName"]?.text?.toString()?.trim().orEmpty(),
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
            buildProp = BuildPropProfile(
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
            ),
            buildPropEnabled = buildSwitch.isChecked,
            draft = draft
        )
    }

    private fun buildText(key: String): String = buildInputs[key]?.text?.toString()?.trim().orEmpty()

    fun showLogs() {
        val logs = viewModel.logRepository.getLogs()
        AlertDialog.Builder(context)
            .setTitle("Log")
            .setMessage(if (logs.isEmpty()) "Belum ada log" else logs.joinToString("\n\n"))
            .setNegativeButton("Clear") { _, _ ->
                viewModel.logRepository.clear()
                toast("Log dibersihkan")
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun edit(hintValue: String, value: String): EditText = EditText(context).apply {
        hint = hintValue
        setText(value)
        setSingleLine(true)
        inputType = InputType.TYPE_CLASS_TEXT
        setTextColor(textColor)
        setHintTextColor(Color.rgb(100, 116, 139))
        backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(71, 85, 105))
    }

    private fun panel(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(22, 18, 22, 18)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(panelBg)
            cornerRadius = 10f
            setStroke(1, panelStroke)
        }
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 18, 0, 0)
        layoutParams = params
    }

    private fun sectionTitle(textValue: String): TextView = TextView(context).apply {
        text = textValue
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(textColor)
        setPadding(0, 0, 0, 10)
    }

    private fun smallButton(label: String): Button = Button(context).apply {
        text = label
        minWidth = 0
        minHeight = 0
        setPadding(18, 8, 18, 8)
        setTextColor(textColor)
    }

    private fun primaryButton(label: String): Button = Button(context).apply {
        text = label
        setTextColor(Color.rgb(3, 7, 18))
    }

    private fun ghostButton(label: String): Button = Button(context).apply {
        text = label
        setTextColor(textColor)
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
