package com.example.customkeyboard.view

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.example.customkeyboard.controller.KeyboardController
import com.example.customkeyboard.controller.ViewActions
import com.example.customkeyboard.model.ActionType
import com.example.customkeyboard.model.EmojiLayout
import com.example.customkeyboard.model.Key
import com.example.customkeyboard.model.KeyboardLayer
import com.example.customkeyboard.model.QwertyLayout
import com.example.customkeyboard.model.ShiftState
import com.example.customkeyboard.model.SymbolLayout

/**
 * Custom [ViewGroup] that renders the full keyboard panel.
 *
 * Layout strategy
 * ---------------
 * Each row is measured independently. Every key in a row gets an equal share of the available
 * width, except [ActionType.SPACE] which gets 3× the share. This is computed in [onLayout]
 * rather than relying on LinearLayout weights, which avoids the rounding errors that cause
 * edge keys to overflow on narrow screens.
 *
 * Background colour: #263238 (Material Blue Grey 900).
 */
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), ViewActions {

    private var controller: KeyboardController? = null
    private var currentLayer: KeyboardLayer = KeyboardLayer.QWERTY

    // Rows of KeyView lists — rebuilt on every layer switch
    private var keyRows: List<List<KeyView>> = emptyList()

    private val handler = Handler(Looper.getMainLooper())
    private var previewPopup: PopupWindow? = null
    private val dismissPreviewRunnable = Runnable { dismissKeyPreview() }

    /** Height of each key row in pixels. */
    private val rowHeightPx: Int get() = dpToPx(52)

    /** Horizontal gap between keys in pixels. */
    private val keyGapPx: Int get() = dpToPx(3)

    /** Vertical gap between rows in pixels. */
    private val rowGapPx: Int get() = dpToPx(2)

    init {
        setBackgroundColor(Color.parseColor("#263238"))
        inflateLayer(KeyboardLayer.QWERTY)
    }

    fun setController(controller: KeyboardController) {
        this.controller = controller
        rewireController()
    }

    // -------------------------------------------------------------------------
    // Layer inflation
    // -------------------------------------------------------------------------

    private fun inflateLayer(layer: KeyboardLayer) {
        removeAllViews()
        currentLayer = layer

        val rows: List<List<Key>> = when (layer) {
            KeyboardLayer.QWERTY  -> QwertyLayout.rows
            KeyboardLayer.SYMBOL  -> SymbolLayout.rows
            KeyboardLayer.EMOJI   -> EmojiLayout.rows
        }

        keyRows = rows.map { row ->
            row.map { key ->
                KeyView(context).also { kv ->
                    kv.key = key
                    kv.controller = this.controller
                    // Emoji keys need the full string from EmojiLayout
                    if (key is Key.Symbol && layer == KeyboardLayer.EMOJI) {
                        val cp = key.char.code
                        kv.emojiString = EmojiLayout.emojiStrings[cp]
                    }
                    addView(kv)
                }
            }
        }
    }

    private fun rewireController() {
        keyRows.forEach { row -> row.forEach { kv -> kv.controller = controller } }
    }

    // -------------------------------------------------------------------------
    // Measure & Layout — proportional key widths, no overflow
    // -------------------------------------------------------------------------

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val rowCount = keyRows.size
        val totalHeight = rowCount * rowHeightPx + (rowCount - 1).coerceAtLeast(0) * rowGapPx
        setMeasuredDimension(w, totalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val totalWidth = r - l
        var top = 0

        for (row in keyRows) {
            // Calculate total weight units for this row
            val totalWeight = row.sumOf { kv -> keyWeight(kv.key) }
            val availableWidth = totalWidth - keyGapPx * (row.size + 1)
            var left = keyGapPx

            for (kv in row) {
                val weight = keyWeight(kv.key)
                val keyWidth = ((availableWidth * weight) / totalWeight).toInt()

                val wSpec = MeasureSpec.makeMeasureSpec(keyWidth, MeasureSpec.EXACTLY)
                val hSpec = MeasureSpec.makeMeasureSpec(rowHeightPx, MeasureSpec.EXACTLY)
                kv.measure(wSpec, hSpec)
                kv.layout(left, top, left + keyWidth, top + rowHeightPx)

                left += keyWidth + keyGapPx
            }

            top += rowHeightPx + rowGapPx
        }
    }

    /** Weight units for a key — Space gets 3×, all others get 1×. */
    private fun keyWeight(key: Key?): Int =
        if (key is Key.Action && key.type == ActionType.SPACE) 3 else 1

    // -------------------------------------------------------------------------
    // ViewActions
    // -------------------------------------------------------------------------

    override fun switchLayer(layer: KeyboardLayer) {
        dismissKeyPreview()
        inflateLayer(layer)
        rewireController()
        requestLayout()
        invalidate()
    }

    override fun updateShiftIndicator(state: ShiftState) {
        findKeyView { it is Key.Action && it.type == ActionType.SHIFT }?.let { kv ->
            kv.shiftState = state
            kv.invalidate()
        }
    }

    override fun updateEnterLabel(imeOptions: Int) {
        findKeyView { it is Key.Action && it.type == ActionType.ENTER }?.let { kv ->
            kv.tag = imeOptions
            kv.invalidate()
        }
    }

    override fun showKeyPreview(key: Key) {
        handler.removeCallbacks(dismissPreviewRunnable)

        val label = when (key) {
            is Key.Letter -> key.char.uppercaseChar().toString()
            is Key.Symbol -> key.char.toString()
            else -> return
        }

        val tv = TextView(context).apply {
            text = label
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#37474F"))
            gravity = Gravity.CENTER
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
        }

        previewPopup?.dismiss()
        previewPopup = PopupWindow(tv, dpToPx(44), dpToPx(44), false).apply {
            showAtLocation(this@KeyboardView, Gravity.TOP or Gravity.START, 0, 0)
        }

        handler.postDelayed(dismissPreviewRunnable, 800L)
    }

    override fun dismissKeyPreview() {
        handler.removeCallbacks(dismissPreviewRunnable)
        previewPopup?.dismiss()
        previewPopup = null
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun findKeyView(predicate: (Key) -> Boolean): KeyView? =
        keyRows.flatten().firstOrNull { kv -> kv.key?.let(predicate) == true }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
}
