package com.example.customkeyboard.settings

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.customkeyboard.settings.KeyboardHeightManager.Preset

/**
 * Simple settings screen for keyboard height.
 *
 * Accessible from the Android app launcher (not from inside the IME itself,
 * since IME services cannot start activities directly).
 *
 * Layout (all programmatic — no XML needed):
 *   Title
 *   4 preset buttons (Short / Normal / Tall / Extra Tall)
 *   Fine-tune SeekBar  (±30% around the selected preset)
 *   Live preview label showing current row height in dp
 */
class KeyboardSettingsActivity : AppCompatActivity() {

    private val BASE_ROW_DP = 52f   // matches KeyboardView default

    private lateinit var seekBar: SeekBar
    private lateinit var previewLabel: TextView
    private val presetButtons = mutableListOf<Button>()

    // SeekBar maps 0..100 → scale 0.70..1.60
    private val SCALE_MIN = 0.70f
    private val SCALE_MAX = 1.60f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }
        root.addView(container)
        setContentView(root)

        // ── Title ──────────────────────────────────────────────────────────
        container.addView(TextView(this).apply {
            text = "Keyboard Height"
            textSize = 22f
            setTextColor(0xFF1C1C1E.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        })

        container.addView(TextView(this).apply {
            text = "Choose how tall the keyboard rows appear."
            textSize = 14f
            setTextColor(0xFF8E8E93.toInt())
            setPadding(0, 0, 0, dp(24))
        })

        // ── Preset buttons ─────────────────────────────────────────────────
        val presetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(24) }
        }

        Preset.entries.forEach { preset ->
            val btn = Button(this).apply {
                text = preset.label
                textSize = 13f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginEnd = dp(6)
                }
                setOnClickListener { applyPreset(preset) }
            }
            presetButtons.add(btn)
            presetRow.addView(btn)
        }
        container.addView(presetRow)

        // ── Fine-tune label ────────────────────────────────────────────────
        container.addView(TextView(this).apply {
            text = "Fine-tune"
            textSize = 16f
            setTextColor(0xFF1C1C1E.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        })

        // ── SeekBar ────────────────────────────────────────────────────────
        seekBar = SeekBar(this).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(12) }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val scale = progressToScale(progress)
                        KeyboardHeightManager.setScale(this@KeyboardSettingsActivity, scale)
                        updatePreviewLabel(scale)
                        highlightMatchingPreset(scale)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
        container.addView(seekBar)

        // ── Preview label ──────────────────────────────────────────────────
        previewLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFF8E8E93.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(32) }
        }
        container.addView(previewLabel)

        // ── Visual preview strip ───────────────────────────────────────────
        container.addView(TextView(this).apply {
            text = "Preview"
            textSize = 16f
            setTextColor(0xFF1C1C1E.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        })

        val previewStrip = buildPreviewStrip()
        container.addView(previewStrip)

        // ── Reset button ───────────────────────────────────────────────────
        container.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).also { it.topMargin = dp(32); it.bottomMargin = dp(16) }
            setBackgroundColor(0xFFE5E5EA.toInt())
        })

        container.addView(Button(this).apply {
            text = "Reset to Default"
            isAllCaps = false
            textSize = 15f
            setOnClickListener {
                applyPreset(Preset.NORMAL)
                Toast.makeText(this@KeyboardSettingsActivity,
                    "Reset to Normal", Toast.LENGTH_SHORT).show()
            }
        })

        // ── Load current value ─────────────────────────────────────────────
        val currentScale = KeyboardHeightManager.getScale(this)
        seekBar.progress = scaleToProgress(currentScale)
        updatePreviewLabel(currentScale)
        highlightMatchingPreset(currentScale)
    }

    private fun applyPreset(preset: Preset) {
        KeyboardHeightManager.setPreset(this, preset)
        seekBar.progress = scaleToProgress(preset.scale)
        updatePreviewLabel(preset.scale)
        highlightMatchingPreset(preset.scale)
        Toast.makeText(this, "${preset.label} applied — restart keyboard to see changes",
            Toast.LENGTH_SHORT).show()
    }

    private fun updatePreviewLabel(scale: Float) {
        val rowDp = (BASE_ROW_DP * scale).toInt()
        previewLabel.text = "Row height: ${rowDp}dp  (${(scale * 100).toInt()}%)"
    }

    private fun highlightMatchingPreset(scale: Float) {
        Preset.entries.forEachIndexed { i, preset ->
            val isMatch = kotlin.math.abs(preset.scale - scale) < 0.01f
            presetButtons.getOrNull(i)?.alpha = if (isMatch) 1.0f else 0.5f
        }
    }

    /** Builds a simple visual strip showing approximate key height. */
    private fun buildPreviewStrip(): View {
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1C1C1E.toInt())
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val scale = KeyboardHeightManager.getScale(this)
        val keyH = (BASE_ROW_DP * scale).toInt()
        listOf("Q", "W", "E", "R", "T", "Y").forEach { letter ->
            strip.addView(TextView(this).apply {
                text = letter
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
                setBackgroundColor(0xFF2C2C2E.toInt())
                layoutParams = LinearLayout.LayoutParams(0, dp(keyH), 1f).also {
                    it.marginEnd = dp(4)
                }
            })
        }
        return strip
    }

    private fun progressToScale(progress: Int): Float =
        SCALE_MIN + (progress / 100f) * (SCALE_MAX - SCALE_MIN)

    private fun scaleToProgress(scale: Float): Int =
        ((scale - SCALE_MIN) / (SCALE_MAX - SCALE_MIN) * 100).toInt().coerceIn(0, 100)

    private fun dp(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
