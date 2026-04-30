package com.example.customkeyboard.view

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
 * Full keyboard panel matching the screenshot layout exactly.
 *
 * QWERTY weights per row:
 *   Row 0 (q-p):        10 × 1.0  = 10 units
 *   Row 1 (a-l):         9 × 1.0  =  9 units  (centred via side padding)
 *   Row 2 (shift/bksp): shift=1.5, 7×letter=1.0, bksp=1.5  = 10 units
 *   Row 3 (bottom):     ?123=1.5, comma=0.5, emoji=0.5, space=4.0, period=0.5, search=1.5 = 8.5 units
 *
 * EMOJI layout:
 *   Category bar: 10 icons + backspace (fixed height 40dp)
 *   4 rows of 6 emoji tiles
 *   Bottom row: ABC(1.5) SPACE(4.0) ABC(1.5)
 *
 * Background: #1C1C1E
 */
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), ViewActions {

    private var controller: KeyboardController? = null
    private var currentLayer: KeyboardLayer = KeyboardLayer.QWERTY

    // All key views in row order
    private var keyRows: List<List<KeyView>> = emptyList()

    // Emoji category bar views (only present in EMOJI layer)
    private var categoryBarViews: List<KeyView> = emptyList()
    private var hasCategoryBar = false

    private val handler = Handler(Looper.getMainLooper())
    private var previewPopup: PopupWindow? = null
    private val dismissPreviewRunnable = Runnable { dismissKeyPreview() }

    // ── Dimensions ───────────────────────────────────────────────────────────
    private val rowHeightPx:      Int get() = dpToPx(52)
    private val emojiRowHeightPx: Int get() = dpToPx(68)
    private val catBarHeightPx:   Int get() = dpToPx(44)
    private val keyGapPx:         Int get() = dpToPx(6)
    private val rowGapPx:         Int get() = dpToPx(4)
    private val sidePadPx:        Int get() = dpToPx(4)

    init {
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        inflateLayer(KeyboardLayer.QWERTY)
    }

    fun setController(c: KeyboardController) {
        controller = c
        rewireController()
    }

    // ── Layer inflation ───────────────────────────────────────────────────────

    private fun inflateLayer(layer: KeyboardLayer) {
        removeAllViews()
        currentLayer = layer
        hasCategoryBar = false
        categoryBarViews = emptyList()

        when (layer) {
            KeyboardLayer.QWERTY -> inflateQwerty()
            KeyboardLayer.SYMBOL -> inflateSymbol()
            KeyboardLayer.EMOJI  -> inflateEmoji()
        }
    }

    private fun inflateQwerty() {
        keyRows = QwertyLayout.rows.map { row ->
            row.map { key -> makeKeyView(key) }
        }
        keyRows.flatten().forEach { addView(it) }
    }

    private fun inflateSymbol() {
        keyRows = SymbolLayout.rows.map { row ->
            row.map { key -> makeKeyView(key) }
        }
        keyRows.flatten().forEach { addView(it) }
    }

    private fun inflateEmoji() {
        hasCategoryBar = true

        // Category bar: icons + backspace
        val catViews = EmojiLayout.categoryIcons.map { icon ->
            makeKeyView(Key.Emoji(icon)).also { it.isEmojiGridKey = false }
        } + listOf(makeKeyView(Key.Action(ActionType.BACKSPACE)))
        categoryBarViews = catViews
        catViews.forEach { addView(it) }

        // Emoji grid rows
        keyRows = EmojiLayout.rows.map { row ->
            row.map { key ->
                makeKeyView(key).also { kv ->
                    if (key is Key.Emoji) kv.isEmojiGridKey = true
                }
            }
        }
        keyRows.flatten().forEach { addView(it) }
    }

    private fun makeKeyView(key: Key): KeyView = KeyView(context).also { kv ->
        kv.key = key
        kv.controller = controller
    }

    private fun rewireController() {
        categoryBarViews.forEach { it.controller = controller }
        keyRows.flatten().forEach { it.controller = controller }
    }

    // ── Measure ──────────────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = computeTotalHeight()
        setMeasuredDimension(w, h)
    }

    private fun computeTotalHeight(): Int {
        return when (currentLayer) {
            KeyboardLayer.EMOJI -> {
                val catBar = if (hasCategoryBar) catBarHeightPx + rowGapPx else 0
                val gridRows = EmojiLayout.emojiRows.size
                val bottomRow = 1
                val totalRows = gridRows + bottomRow
                catBar + totalRows * emojiRowHeightPx + (totalRows - 1) * rowGapPx + rowGapPx
            }
            else -> {
                val rowCount = keyRows.size
                rowCount * rowHeightPx + (rowCount - 1) * rowGapPx + rowGapPx * 2
            }
        }
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val totalWidth = r - l
        var top = rowGapPx

        if (hasCategoryBar) {
            layoutCategoryBar(totalWidth, top)
            top += catBarHeightPx + rowGapPx
        }

        val rows = keyRows
        rows.forEachIndexed { index, row ->
            val rowH = if (currentLayer == KeyboardLayer.EMOJI) emojiRowHeightPx else rowHeightPx
            layoutRow(row, totalWidth, top, rowH, index)
            top += rowH + rowGapPx
        }
    }

    private fun layoutCategoryBar(totalWidth: Int, top: Int) {
        // 11 equal slots
        val count = categoryBarViews.size
        val available = totalWidth - keyGapPx * (count + 1)
        val slotW = available / count
        var left = keyGapPx
        categoryBarViews.forEach { kv ->
            kv.measure(
                MeasureSpec.makeMeasureSpec(slotW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(catBarHeightPx, MeasureSpec.EXACTLY)
            )
            kv.layout(left, top, left + slotW, top + catBarHeightPx)
            left += slotW + keyGapPx
        }
    }

    private fun layoutRow(
        row: List<KeyView>,
        totalWidth: Int,
        top: Int,
        rowH: Int,
        rowIndex: Int
    ) {
        val weights = row.map { kv -> keyWeight(kv.key, rowIndex) }
        val totalWeight = weights.sum()

        // Row 1 (home row, 9 keys) is centred — add side padding
        val isHomeRow = currentLayer == KeyboardLayer.QWERTY && rowIndex == 1
        val sidePad = if (isHomeRow) dpToPx(18) else sidePadPx

        val available = totalWidth - keyGapPx * (row.size + 1) - sidePad * 2
        var left = sidePad + keyGapPx

        row.forEachIndexed { i, kv ->
            val keyW = ((available * weights[i]) / totalWeight).toInt()
            kv.measure(
                MeasureSpec.makeMeasureSpec(keyW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(rowH, MeasureSpec.EXACTLY)
            )
            kv.layout(left, top, left + keyW, top + rowH)
            left += keyW + keyGapPx
        }
    }

    /**
     * Weight for each key type, per row.
     *
     * QWERTY row 2 (shift row): Shift=1.5, letters=1.0, Backspace=1.5
     * QWERTY row 3 (bottom):    ?123=1.5, comma=0.5, emoji=0.5, space=4.0, period=0.5, search=1.5
     * Emoji bottom row:         ABC=1.5, space=4.0, ABC=1.5
     * Everything else:          1.0
     */
    private fun keyWeight(key: Key?, rowIndex: Int): Float {
        if (key !is Key.Action) return 1f
        return when (key.type) {
            ActionType.SHIFT            -> 1.5f
            ActionType.BACKSPACE        -> 1.5f
            ActionType.SPACE            -> 4.0f
            ActionType.SWITCH_TO_SYMBOLS  -> 1.5f
            ActionType.SWITCH_TO_QWERTY   -> 1.5f
            ActionType.SWITCH_FROM_EMOJI  -> 1.5f
            ActionType.COMMA            -> 0.5f
            ActionType.PERIOD           -> 0.5f
            ActionType.SWITCH_TO_EMOJI  -> 0.5f
            ActionType.SEARCH           -> 1.5f
            else -> 1f
        }
    }

    // ── ViewActions ──────────────────────────────────────────────────────────

    override fun switchLayer(layer: KeyboardLayer) {
        dismissKeyPreview()
        inflateLayer(layer)
        rewireController()
        requestLayout()
        invalidate()
    }

    override fun updateShiftIndicator(state: ShiftState) {
        keyRows.flatten()
            .firstOrNull { kv -> kv.key is Key.Action && (kv.key as Key.Action).type == ActionType.SHIFT }
            ?.let { kv -> kv.shiftState = state }
    }

    override fun updateEnterLabel(imeOptions: Int) {
        keyRows.flatten()
            .firstOrNull { kv -> kv.key is Key.Action &&
                    ((kv.key as Key.Action).type == ActionType.ENTER ||
                     (kv.key as Key.Action).type == ActionType.SEARCH) }
            ?.let { kv -> kv.tag = imeOptions; kv.invalidate() }
    }

    override fun showKeyPreview(key: Key) {
        handler.removeCallbacks(dismissPreviewRunnable)
        val label = when (key) {
            is Key.Letter -> key.char.uppercaseChar().toString()
            is Key.Symbol -> key.char.toString()
            else -> return
        }
        val tv = TextView(context).apply {
            text = label; textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#3A3A3C"))
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

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
}
