package com.example.customkeyboard.view

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import com.example.customkeyboard.controller.KeyboardController
import com.example.customkeyboard.controller.ViewActions
import com.example.customkeyboard.gif.GifPanelView
import com.example.customkeyboard.model.ActionType
import com.example.customkeyboard.model.EmojiCategory
import com.example.customkeyboard.model.EmojiLayout
import com.example.customkeyboard.model.Key
import com.example.customkeyboard.model.KeyboardLayer
import com.example.customkeyboard.model.QwertyLayout
import com.example.customkeyboard.model.ShiftState
import com.example.customkeyboard.model.SymbolLayout
import com.example.customkeyboard.settings.KeyboardHeightManager

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), ViewActions {

    private var controller: KeyboardController? = null
    private var currentLayer: KeyboardLayer = KeyboardLayer.QWERTY
    private var currentEmojiCategory: EmojiCategory = EmojiCategory.RECENT

    // Regular key rows (QWERTY / SYMBOL)
    private var keyRows: List<List<KeyView>> = emptyList()

    // Emoji layer components
    private var categoryBarViews: List<KeyView> = emptyList()
    private var emojiScrollView: ScrollView? = null
    private var emojiGridContainer: LinearLayout? = null
    private var emojiBottomRowViews: List<KeyView> = emptyList()
    private var gifPanelView: GifPanelView? = null
    private var hasCategoryBar = false
    private var showingGifPanel = false

    private val handler = Handler(Looper.getMainLooper())
    private var previewPopup: PopupWindow? = null
    private val dismissPreviewRunnable = Runnable { dismissKeyPreview() }

    // ── Dimensions ───────────────────────────────────────────────────────────
    // Scale is read fresh on every layout pass — no caching, no stale values.
    private val heightScale: Float get() = KeyboardHeightManager.getScale(context)
    private val rowHeightPx:       Int get() = (dpToPx(52) * heightScale).toInt()
    private val emojiRowHeightPx:  Int get() = (dpToPx(72) * heightScale).toInt()
    private val catBarHeightPx:    Int get() = (dpToPx(44) * heightScale).toInt()
    private val emojiGridHeightPx: Int get() = emojiRowHeightPx * 4 + rowGapPx * 3
    private val bottomRowHeightPx: Int get() = (dpToPx(56) * heightScale).toInt()
    private val keyGapPx:          Int get() = dpToPx(6)
    private val rowGapPx:          Int get() = dpToPx(4)
    private val sidePadPx:         Int get() = dpToPx(4)

    // Track last-applied scale so we can detect changes and re-measure
    private var appliedScale: Float = 1.0f

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
        emojiScrollView = null
        emojiGridContainer = null
        emojiBottomRowViews = emptyList()
        gifPanelView = null
        showingGifPanel = false
        keyRows = emptyList()

        when (layer) {
            KeyboardLayer.QWERTY -> inflateQwerty()
            KeyboardLayer.SYMBOL -> inflateSymbol()
            KeyboardLayer.EMOJI  -> inflateEmoji()
        }
    }

    private fun inflateQwerty() {
        keyRows = QwertyLayout.rows.map { row -> row.map { makeKeyView(it) } }
        keyRows.flatten().forEach { addView(it) }
    }

    private fun inflateSymbol() {
        keyRows = SymbolLayout.rows.map { row -> row.map { makeKeyView(it) } }
        keyRows.flatten().forEach { addView(it) }
    }

    private fun inflateEmoji() {
        hasCategoryBar = true

        // 1. Category bar
        val catViews = EmojiLayout.categoryBar.map { key ->
            makeKeyView(key).also { kv ->
                if (key is Key.CategoryIcon) {
                    kv.setOnCategoryTapListener { cat -> switchEmojiCategory(cat) }
                }
            }
        }
        categoryBarViews = catViews
        catViews.forEach { addView(it) }

        // 2. Scrollable emoji grid
        val scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
        }
        val gridContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(gridContainer)
        addView(scrollView)
        emojiScrollView = scrollView
        emojiGridContainer = gridContainer

        // 3. Bottom row
        val bottomViews = EmojiLayout.bottomRow.map { makeKeyView(it) }
        emojiBottomRowViews = bottomViews
        bottomViews.forEach { addView(it) }

        // Populate grid with current category
        populateEmojiGrid(currentEmojiCategory)
    }

    private fun populateEmojiGrid(category: EmojiCategory) {
        val container = emojiGridContainer ?: return
        container.removeAllViews()

        val rows = EmojiLayout.rowsForCategory(category)
        rows.forEach { row ->
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    emojiRowHeightPx
                ).also { it.bottomMargin = rowGapPx }
            }
            val totalWeight = row.size.toFloat()
            row.forEach { key ->
                val kv = makeKeyView(key).also { it.isEmojiGridKey = true }
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                lp.setMargins(keyGapPx / 2, 0, keyGapPx / 2, 0)
                kv.layoutParams = lp
                rowLayout.addView(kv)
            }
            container.addView(rowLayout)
        }

        // Update category bar highlight
        categoryBarViews.forEach { kv ->
            val k = kv.key
            if (k is Key.CategoryIcon) {
                kv.isSelectedCategory = (k.category == category)
            }
        }

        emojiScrollView?.scrollTo(0, 0)
    }

    fun switchEmojiCategory(category: EmojiCategory) {
        currentEmojiCategory = category

        if (category == EmojiCategory.GIF) {
            // Show GIF panel, hide emoji scroll view
            showingGifPanel = true
            emojiScrollView?.visibility = View.GONE

            if (gifPanelView == null) {
                val panel = GifPanelView(context).also { panel ->
                    panel.onGifSelected = { url ->
                        // Commit the GIF URL as text to the input connection
                        controller?.onKeyTapped(Key.Emoji(url))
                    }
                }
                gifPanelView = panel
                addView(panel)
            }
            gifPanelView?.visibility = View.VISIBLE
        } else {
            // Show emoji scroll view, hide GIF panel
            showingGifPanel = false
            gifPanelView?.visibility = View.GONE
            emojiScrollView?.visibility = View.VISIBLE
            populateEmojiGrid(category)
        }

        requestLayout()
    }

    private fun makeKeyView(key: Key): KeyView = KeyView(context).also { kv ->
        kv.key = key
        kv.controller = controller
    }

    private fun rewireController() {
        categoryBarViews.forEach { it.controller = controller }
        emojiBottomRowViews.forEach { it.controller = controller }
        keyRows.flatten().forEach { it.controller = controller }
        // Rewire emoji grid views inside the scroll container
        emojiGridContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val row = container.getChildAt(i) as? LinearLayout ?: continue
                for (j in 0 until row.childCount) {
                    (row.getChildAt(j) as? KeyView)?.controller = controller
                }
            }
        }
    }

    // ── Measure ──────────────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // If the user changed the height setting since last layout, re-inflate
        // so the emoji grid rows (which use LinearLayout.LayoutParams) also update.
        val currentScale = heightScale
        if (currentScale != appliedScale) {
            appliedScale = currentScale
            // Re-inflate emoji grid rows with new heights (QWERTY/Symbol rows are
            // laid out directly in onLayout so they pick up the new value automatically)
            if (currentLayer == KeyboardLayer.EMOJI) {
                populateEmojiGrid(currentEmojiCategory)
            }
        }
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = computeTotalHeight()
        setMeasuredDimension(w, h)
    }

    private fun computeTotalHeight(): Int = when (currentLayer) {
        KeyboardLayer.EMOJI -> {
            val gridH = if (showingGifPanel)
                (dpToPx(72) * 4 + rowGapPx * 3) // same height as emoji grid
            else
                emojiGridHeightPx
            catBarHeightPx + rowGapPx + gridH + rowGapPx + bottomRowHeightPx + rowGapPx
        }
        else -> {
            val rowCount = keyRows.size
            rowCount * rowHeightPx + (rowCount - 1) * rowGapPx + rowGapPx * 2
        }
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val totalWidth = r - l

        when (currentLayer) {
            KeyboardLayer.EMOJI -> layoutEmoji(totalWidth)
            else -> layoutRegular(totalWidth)
        }
    }

    private fun layoutRegular(totalWidth: Int) {
        var top = rowGapPx
        keyRows.forEachIndexed { index, row ->
            layoutRow(row, totalWidth, top, rowHeightPx, index)
            top += rowHeightPx + rowGapPx
        }
    }

    private fun layoutEmoji(totalWidth: Int) {
        var top = rowGapPx

        // Category bar
        layoutCategoryBar(totalWidth, top)
        top += catBarHeightPx + rowGapPx

        val gridH = if (showingGifPanel)
            (dpToPx(72) * 4 + rowGapPx * 3)
        else
            emojiGridHeightPx

        if (showingGifPanel) {
            // GIF panel
            gifPanelView?.let { panel ->
                panel.measure(
                    MeasureSpec.makeMeasureSpec(totalWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(gridH, MeasureSpec.EXACTLY)
                )
                panel.layout(0, top, totalWidth, top + gridH)
            }
        } else {
            // Emoji scroll view
            val scrollView = emojiScrollView ?: return
            scrollView.measure(
                MeasureSpec.makeMeasureSpec(totalWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(gridH, MeasureSpec.EXACTLY)
            )
            scrollView.layout(0, top, totalWidth, top + gridH)
        }
        top += gridH + rowGapPx

        // Bottom row
        layoutBottomRow(emojiBottomRowViews, totalWidth, top, bottomRowHeightPx)
    }

    private fun layoutCategoryBar(totalWidth: Int, top: Int) {
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

    private fun layoutBottomRow(views: List<KeyView>, totalWidth: Int, top: Int, rowH: Int) {
        val weights = views.map { kv -> keyWeight(kv.key) }
        val totalWeight = weights.sum()
        val available = totalWidth - keyGapPx * (views.size + 1)
        var left = keyGapPx
        views.forEachIndexed { i, kv ->
            val keyW = ((available * weights[i]) / totalWeight).toInt()
            kv.measure(
                MeasureSpec.makeMeasureSpec(keyW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(rowH, MeasureSpec.EXACTLY)
            )
            kv.layout(left, top, left + keyW, top + rowH)
            left += keyW + keyGapPx
        }
    }

    private fun layoutRow(row: List<KeyView>, totalWidth: Int, top: Int, rowH: Int, rowIndex: Int) {
        val weights = row.map { kv -> keyWeight(kv.key) }
        val totalWeight = weights.sum()

        // Home row (row 1 in QWERTY, row 1 in SYMBOL) — centred
        val isHomeRow = (currentLayer == KeyboardLayer.QWERTY && rowIndex == 1) ||
                        (currentLayer == KeyboardLayer.SYMBOL && rowIndex == 1)
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

    private fun keyWeight(key: Key?): Float {
        if (key !is Key.Action) return 1f
        return when (key.type) {
            ActionType.SHIFT                -> 1.5f
            ActionType.BACKSPACE            -> 1.5f
            ActionType.SPACE                -> 4.0f
            ActionType.SWITCH_TO_SYMBOLS    -> 1.5f
            ActionType.SWITCH_TO_QWERTY     -> 1.5f
            ActionType.SWITCH_FROM_EMOJI    -> 1.5f
            ActionType.SWITCH_TO_MORE_SYMBOLS -> 1.5f
            ActionType.COMMA                -> 1.0f
            ActionType.PERIOD               -> 1.0f
            ActionType.SWITCH_TO_EMOJI      -> 1.0f
            ActionType.SEARCH               -> 1.5f
            ActionType.UNDERSCORE           -> 1.0f
            ActionType.SLASH                -> 1.0f
            ActionType.DONE                 -> 1.5f
            else -> 1f
        }
    }

    // ── ViewActions ──────────────────────────────────────────────────────────

    override fun switchLayer(layer: KeyboardLayer) {
        dismissKeyPreview()
        if (layer == KeyboardLayer.EMOJI) currentEmojiCategory = EmojiCategory.RECENT
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
            .firstOrNull { kv ->
                kv.key is Key.Action && ((kv.key as Key.Action).type == ActionType.ENTER ||
                        (kv.key as Key.Action).type == ActionType.SEARCH ||
                        (kv.key as Key.Action).type == ActionType.DONE)
            }?.let { kv -> kv.tag = imeOptions; kv.invalidate() }
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

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
}
