package com.example.customkeyboard.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists and retrieves the user's keyboard height preference.
 *
 * Height is stored as a scale factor relative to the default row height (52dp).
 * Scale 1.0 = default, 1.15 = tall, 0.85 = short.
 *
 * The value is written to SharedPreferences so it survives process restarts
 * and is immediately available when the IME service creates its input view.
 */
object KeyboardHeightManager {

    private const val PREFS_NAME  = "keyboard_prefs"
    private const val KEY_SCALE   = "row_height_scale"
    private const val DEFAULT_SCALE = 1.0f

    /** Preset options shown in the settings UI. */
    enum class Preset(val label: String, val scale: Float) {
        SHORT("Short",   0.82f),
        NORMAL("Normal", 1.00f),
        TALL("Tall",     1.18f),
        EXTRA_TALL("Extra Tall", 1.35f)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the current scale factor (default 1.0). */
    fun getScale(context: Context): Float =
        prefs(context).getFloat(KEY_SCALE, DEFAULT_SCALE)

    /** Saves a scale factor. */
    fun setScale(context: Context, scale: Float) {
        prefs(context).edit().putFloat(KEY_SCALE, scale.coerceIn(0.7f, 1.6f)).apply()
    }

    /** Saves a preset. */
    fun setPreset(context: Context, preset: Preset) = setScale(context, preset.scale)

    /** Returns the preset that best matches the current scale, or null if custom. */
    fun currentPreset(context: Context): Preset? {
        val scale = getScale(context)
        return Preset.entries.firstOrNull { kotlin.math.abs(it.scale - scale) < 0.01f }
    }
}
