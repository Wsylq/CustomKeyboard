package com.example.customkeyboard.view

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.customkeyboard.controller.KeyboardController
import com.example.customkeyboard.controller.ViewActions
import com.example.customkeyboard.model.ActionType
import com.example.customkeyboard.model.Key
import com.example.customkeyboard.model.KeyboardLayer
import com.example.customkeyboard.model.QwertyLayout
import com.example.customkeyboard.model.ShiftState
import com.example.customkeyboard.model.SymbolLayout

/**
 * Custom [LinearLayout] that renders the full keyboard panel.
 *
 * Responsibilities:
 * - Inflate [KeyView] instances from the active layer's row/key data and lay them out in rows.
 * - Scale key widths proportionally to available display width via LinearLayout weights.
 * - Implement [ViewActions] so [KeyboardController] can drive layer switches, shift indicator
 *   updates, enter label updates, and key popup previews.
 *
 * Background colour: #263238 (Material Blue Grey 900).
 */
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ViewActions {

    private var controller: KeyboardController? = null
    private var currentLayer: KeyboardLayer = KeyboardLayer.QWERTY

    private val handler = Handler(Looper.getMainLooper())
    private var previewPopup: PopupWindow? = null
    private val dismissPreviewRunnable = Runnable { dismissKeyPreview() }

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#263238"))
        inflateLayer(KeyboardLayer.QWERTY)
    }

    /**
     * Attaches a [KeyboardController] and wires it to every [KeyView] currently in the hierarchy.
     */
    fun setController(controller: KeyboardController) {
        this.controller = controller
        rewireController()
    }

    // -------------------------------------------------------------------------
    // Layer inflation — proportional key widths via weight
    // -------------------------------------------------------------------------

    private fun inflateLayer(layer: KeyboardLayer) {
        removeAllViews()
        currentLayer = layer

        val rows = if (layer == KeyboardLayer.QWERTY) QwertyLayout.rows else SymbolLayout.rows

        for (row in rows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }

            for (key in row) {
                val weight = if (key is Key.Action && key.type == ActionType.SPACE) 3f else 1f
                val keyView = KeyView(context).apply {
                    this.key = key
                    this.controller = this@KeyboardView.controller
                    layoutParams = LayoutParams(0, dpToPx(52), weight).also { lp ->
                        val margin = dpToPx(2)
                        lp.setMargins(margin, margin, margin, margin)
                    }
                }
                rowLayout.addView(keyView)
            }

            addView(rowLayout)
        }
    }

    private fun rewireController() {
        for (i in 0 until childCount) {
            val row = getChildAt(i) as? LinearLayout ?: continue
            for (j in 0 until row.childCount) {
                (row.getChildAt(j) as? KeyView)?.controller = controller
            }
        }
    }

    // -------------------------------------------------------------------------
    // ViewActions — layer switching and indicator updates
    // -------------------------------------------------------------------------

    override fun switchLayer(layer: KeyboardLayer) {
        dismissKeyPreview()
        inflateLayer(layer)
        rewireController()
    }

    override fun updateShiftIndicator(state: ShiftState) {
        val shiftView = findKeyView { it is Key.Action && it.type == ActionType.SHIFT } ?: return
        shiftView.alpha = when (state) {
            ShiftState.OFF -> 1.0f
            ShiftState.SINGLE -> 0.7f
            ShiftState.CAPS_LOCK -> 0.4f
        }
        shiftView.invalidate()
    }

    override fun updateEnterLabel(imeOptions: Int) {
        val enterView = findKeyView { it is Key.Action && it.type == ActionType.ENTER } ?: return
        enterView.tag = imeOptions
        enterView.invalidate()
    }

    // -------------------------------------------------------------------------
    // ViewActions — key popup preview overlay
    // -------------------------------------------------------------------------

    override fun showKeyPreview(key: Key) {
        handler.removeCallbacks(dismissPreviewRunnable)

        val label = when (key) {
            is Key.Letter -> key.char.uppercaseChar().toString()
            is Key.Symbol -> key.char.toString()
            else -> return // no preview for action keys
        }

        val previewTextView = TextView(context).apply {
            text = label
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#37474F"))
            gravity = Gravity.CENTER
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
        }

        previewPopup?.dismiss()
        previewPopup = PopupWindow(
            previewTextView,
            dpToPx(44),
            dpToPx(44),
            false
        ).apply {
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

    private fun findKeyView(predicate: (Key) -> Boolean): KeyView? {
        for (i in 0 until childCount) {
            val row = getChildAt(i) as? LinearLayout ?: continue
            for (j in 0 until row.childCount) {
                val kv = row.getChildAt(j) as? KeyView ?: continue
                val k = kv.key ?: continue
                if (predicate(k)) return kv
            }
        }
        return null
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
}
