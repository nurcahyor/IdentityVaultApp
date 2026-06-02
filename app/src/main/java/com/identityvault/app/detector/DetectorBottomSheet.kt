package com.identityvault.app.detector

import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class DetectorBottomSheet(private val context: Context) {
    private val viewModel = DetectorViewModel(context)

    fun show() {
        val dialog = Dialog(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 20)
            setBackgroundColor(Color.rgb(11, 15, 25))
        }
        val title = TextView(context).apply {
            text = "Environment / Root Detector"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(226, 232, 240))
        }
        val desc = TextView(context).apply {
            text = "Hijau berarti check terlihat bersih. Merah berarti root/modifikasi terdeteksi."
            textSize = 13f
            setTextColor(Color.rgb(148, 163, 184))
            setPadding(0, 6, 0, 12)
        }
        val summary = TextView(context).apply {
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 10)
        }
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        fun render(result: DetectorResult) {
            summary.text = result.summary
            summary.setTextColor(if (result.redCount > 0) Color.rgb(248, 113, 113) else Color.rgb(74, 222, 128))
            list.removeAllViews()
            result.checks.forEach { check ->
                list.addView(checkRow(check))
            }
        }

        render(viewModel.result)
        val scroll = ScrollView(context).apply { addView(list) }
        val actions = LinearLayout(context).apply {
            gravity = Gravity.END
            setPadding(0, 12, 0, 0)
        }
        val refresh = Button(context).apply {
            text = "Refresh"
            setOnClickListener { render(viewModel.refresh()) }
        }
        val copy = Button(context).apply {
            text = "Copy Report"
            setOnClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("IdentityVault detector", viewModel.report()))
                Toast.makeText(context, "Report disalin", Toast.LENGTH_SHORT).show()
            }
        }
        val close = Button(context).apply {
            text = "Close"
            setOnClickListener { dialog.dismiss() }
        }
        actions.addView(refresh)
        actions.addView(copy)
        actions.addView(close)

        root.addView(title)
        root.addView(desc)
        root.addView(summary)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(actions)
        dialog.setContentView(root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun checkRow(check: DetectorCheck): LinearLayout {
        val color = when (check.status) {
            DetectorStatus.CLEAN -> Color.rgb(74, 222, 128)
            DetectorStatus.DETECTED -> Color.rgb(248, 113, 113)
            DetectorStatus.WARNING -> Color.rgb(251, 191, 36)
            DetectorStatus.UNKNOWN -> Color.rgb(148, 163, 184)
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(17, 24, 39))
                cornerRadius = 8f
                setStroke(1, Color.rgb(45, 55, 72))
            }
            val heading = TextView(context).apply {
                text = "${symbol(check.status)} ${check.title}"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color)
            }
            val detail = TextView(context).apply {
                text = check.detail.ifBlank { "No detail" }
                textSize = 13f
                setTextColor(Color.rgb(203, 213, 225))
                setPadding(0, 4, 0, 0)
            }
            addView(heading)
            addView(detail)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 10)
            layoutParams = params
        }
    }

    private fun symbol(status: DetectorStatus): String = when (status) {
        DetectorStatus.CLEAN -> "OK"
        DetectorStatus.DETECTED -> "!"
        DetectorStatus.WARNING -> "?"
        DetectorStatus.UNKNOWN -> "-"
    }
}
