package com.example.customkeyboard.settings

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.customkeyboard.settings.KeyboardHeightManager.Preset

/**
 * Keyboard settings screen — launched from the app icon.
 *
 * Uses plain [Activity] (no AppCompat dependency) to avoid theme crashes.
 * All layout is built programmatically.
 */
class KeyboardSettingsActivity : Activity() {

    private val BASE_ROW_DP = 52f
    private val SCALE_MIN   = 0.70f
    private val SCALE_MAX   = 1.60f

    private lateinit var seekBar: SeekBar
    private lateinit var previewLabel: TextView
    private lateinit var previewStrip: LinearLayout
    private val presetButtons = mutableListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Color.WHITE)

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F2F2F7"))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(20), px(16), px(20), px(40))
        }
        root.addView(container)
        setContentView(root)

        buildToolbar(container)
        buildSection(container, "Keyboard Height Preset") { buildPresetRow(it) }
        buildSection(container, "Fine-tune") { buildSeekBar(it) }
        buildSection(container, "Preview") { buildPreviewArea(it) }
        buildResetButton(container)

        // Load saved value
        val scale = KeyboardHeightManager.getScale(this)
        seekBar.progress = scaleToProgress(scale)
        updateAll(scale)
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────

    private fun buildToolbar(container: LinearLayout) {
        container.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, px(20))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            addView(TextView(this@KeyboardSettingsActivity).apply {
                text = "⌨"
                textSize = 28f
                setPadding(0, 0, px(12), 0)
            })
            addView(LinearLayout(this@KeyboardSettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@KeyboardSettingsActivity).apply {
                    text = "Custom Keyboard"
                    textSize = 20f
                    setTextColor(Color.parseColor("#1C1C1E"))
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(this@KeyboardSettingsActivity).apply {
                    text = "Settings"
                    textSize = 13f
                    setTextColor(Color.parseColor("#8E8E93"))
                })
            })
        })
    }

    // ── Section wrapper ───────────────────────────────────────────────────────

    private fun buildSection(
        container: LinearLayout,
        title: String,
        content: (LinearLayout) -> Unit
    ) {
        // Section title
        container.addView(TextView(this).apply {
            text = title.uppercase()
            textSize = 11f
            setTextColor(Color.parseColor("#8E8E93"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(px(4), px(16), 0, px(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })

        // Card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(px(16), px(16), px(16), px(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Rounded card via outline
            elevation = px(2).toFloat()
        }
        content(card)
        container.addView(card)
    }

    // ── Preset buttons ────────────────────────────────────────────────────────

    private fun buildPresetRow(card: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        Preset.entries.forEach { preset ->
            val btn = Button(this).apply {
                text = preset.label
                textSize = 12f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#2C2C2E"))
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginEnd = px(6)
                }
                setOnClickListener { applyPreset(preset) }
            }
            presetButtons.add(btn)
            row.addView(btn)
        }
        card.addView(row)
    }

    // ── SeekBar ───────────────────────────────────────────────────────────────

    private fun buildSeekBar(card: LinearLayout) {
        previewLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#1C1C1E"))
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = px(12) }
        }
        card.addView(previewLabel)

        seekBar = SeekBar(this).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val scale = progressToScale(progress)
                        KeyboardHeightManager.setScale(this@KeyboardSettingsActivity, scale)
                        updateAll(scale)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
        card.addView(seekBar)

        // Min/Max labels
        card.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = px(4) }
            addView(TextView(this@KeyboardSettingsActivity).apply {
                text = "Shorter"
                textSize = 11f
                setTextColor(Color.parseColor("#8E8E93"))
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@KeyboardSettingsActivity).apply {
                text = "Taller"
                textSize = 11f
                setTextColor(Color.parseColor("#8E8E93"))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        })
    }

    // ── Preview area ──────────────────────────────────────────────────────────

    private fun buildPreviewArea(card: LinearLayout) {
        previewStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1C1C1E"))
            setPadding(px(8), px(8), px(8), px(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        card.addView(previewStrip)
        refreshPreviewStrip(KeyboardHeightManager.getScale(this))
    }

    private fun refreshPreviewStrip(scale: Float) {
        previewStrip.removeAllViews()
        // keyH is in dp — convert to px for the view height
        val keyHPx = (BASE_ROW_DP * scale * resources.displayMetrics.density).toInt()
        listOf("Q", "W", "E", "R", "T", "Y").forEach { letter ->
            previewStrip.addView(TextView(this).apply {
                text = letter
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#2C2C2E"))
                layoutParams = LinearLayout.LayoutParams(0, keyHPx, 1f).also {
                    it.marginEnd = px(4)
                }
            })
        }
    }

    // ── Reset button ──────────────────────────────────────────────────────────

    private fun buildResetButton(container: LinearLayout) {
        container.addView(Button(this).apply {
            text = "Reset to Default (Normal)"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.parseColor("#FF3B30"))
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = px(24) }
            setOnClickListener {
                applyPreset(Preset.NORMAL)
                Toast.makeText(this@KeyboardSettingsActivity,
                    "Reset to Normal", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyPreset(preset: Preset) {
        KeyboardHeightManager.setPreset(this, preset)
        seekBar.progress = scaleToProgress(preset.scale)
        updateAll(preset.scale)
        Toast.makeText(this, "${preset.label} — switch to a text field to see the change",
            Toast.LENGTH_SHORT).show()
    }

    private fun updateAll(scale: Float) {
        val rowDp = (BASE_ROW_DP * scale).toInt()
        previewLabel.text = "Row height: ${rowDp}dp  (${(scale * 100).toInt()}%)"
        refreshPreviewStrip(scale)
        highlightPresets(scale)
    }

    private fun highlightPresets(scale: Float) {
        Preset.entries.forEachIndexed { i, preset ->
            val active = kotlin.math.abs(preset.scale - scale) < 0.01f
            presetButtons.getOrNull(i)?.apply {
                setBackgroundColor(
                    if (active) Color.parseColor("#007AFF")
                    else Color.parseColor("#2C2C2E")
                )
            }
        }
    }

    private fun progressToScale(progress: Int): Float =
        SCALE_MIN + (progress / 100f) * (SCALE_MAX - SCALE_MIN)

    private fun scaleToProgress(scale: Float): Int =
        ((scale - SCALE_MIN) / (SCALE_MAX - SCALE_MIN) * 100).toInt().coerceIn(0, 100)

    private fun px(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
