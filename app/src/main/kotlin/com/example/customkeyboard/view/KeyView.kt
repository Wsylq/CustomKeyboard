package com.example.customkeyboard.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.example.customkeyboard.controller.KeyboardController
import com.example.customkeyboard.model.ActionType
import com.example.customkeyboard.model.EmojiCategory
import com.example.customkeyboard.model.Key
import com.example.customkeyboard.model.ShiftState

class KeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var key: Key? = null
        set(value) { field = value; contentDescription = mainLabel(value); invalidate() }

    var controller: KeyboardController? = null

    /**
     * When non-null, letter and backspace taps are routed here instead of the controller.
     * Used by KeyboardView to feed typing into the GIF search bar.
     */
    var gifSearchInterceptor: ((Key) -> Boolean)? = null

    var shiftState: ShiftState = ShiftState.OFF
        set(value) { field = value; invalidate() }

    var isEmojiGridKey: Boolean = false
        set(value) { field = value; invalidate() }

    /** Highlighted when this category is the active one. */
    var isSelectedCategory: Boolean = false
        set(value) { field = value; invalidate() }

    /** Called when a CategoryIcon key is tapped — wired by KeyboardView. */
    private var onCategoryTap: ((EmojiCategory) -> Unit)? = null
    fun setOnCategoryTapListener(l: (EmojiCategory) -> Unit) { onCategoryTap = l }

    private var isKeyPressed = false

    // ── Colours ──────────────────────────────────────────────────────────────
    private val bgNormal      = Color.parseColor("#2C2C2E")
    private val bgAction      = Color.parseColor("#3A3A3C")
    private val bgPressed     = Color.parseColor("#48484A")
    private val bgCatSelected = Color.parseColor("#48484A")
    private val colorPink     = Color.parseColor("#FF6B8A")
    private val colorHint     = Color.parseColor("#8E8E93")
    private val colorWhite    = Color.WHITE

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface  = android.graphics.Typeface.SANS_SERIF
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface  = android.graphics.Typeface.SANS_SERIF
    }
    private val bgRect = RectF()
    private val cornerPx: Float get() = dpToPx(10).toFloat()

    // ── Measure ──────────────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Width fully controlled by parent; only enforce min height
        setMeasuredDimension(measuredWidth, maxOf(measuredHeight, dpToPx(44)))
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val k = key ?: return

        bgPaint.color = resolveBackground(k)
        bgRect.set(3f, 3f, width - 3f, height - 3f)
        canvas.drawRoundRect(bgRect, cornerPx, cornerPx, bgPaint)

        when (k) {
            is Key.Emoji        -> drawEmoji(canvas, k.emoji)
            is Key.CategoryIcon -> drawCategoryIcon(canvas, k.category)
            is Key.Action       -> drawAction(canvas, k)
            is Key.Letter       -> drawLetter(canvas, k)
            is Key.Symbol       -> drawSymbol(canvas, k)
        }
    }

    private fun resolveBackground(k: Key): Int {
        if (isKeyPressed) return bgPressed
        if (k is Key.CategoryIcon && isSelectedCategory) return bgCatSelected
        return when {
            k is Key.Emoji      -> bgNormal
            k is Key.CategoryIcon -> bgNormal
            k is Key.Action && k.type == ActionType.SPACE -> bgAction
            k is Key.Action     -> bgAction
            else                -> bgNormal
        }
    }

    // ── Letter ───────────────────────────────────────────────────────────────

    private fun drawLetter(canvas: Canvas, k: Key.Letter) {
        mainPaint.textSize = sp(20f)
        mainPaint.color = colorWhite
        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText(k.char.uppercaseChar().toString(), cx, cy, mainPaint)

        if (k.hint != null) {
            hintPaint.textSize = sp(9f)
            hintPaint.color = colorHint
            canvas.drawText(k.hint, width - dpToPx(5).toFloat(), dpToPx(13).toFloat(), hintPaint)
        }
    }

    // ── Symbol ───────────────────────────────────────────────────────────────

    private fun drawSymbol(canvas: Canvas, k: Key.Symbol) {
        mainPaint.textSize = sp(20f)
        mainPaint.color = colorWhite
        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText(k.char.toString(), cx, cy, mainPaint)
    }

    // ── Category icon (Unicode, never emoji) ─────────────────────────────────

    private fun drawCategoryIcon(canvas: Canvas, category: EmojiCategory) {
        mainPaint.textSize = sp(15f)
        mainPaint.color = if (isSelectedCategory) colorWhite else colorHint
        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText(category.symbol, cx, cy, mainPaint)

        // Active indicator underline
        if (isSelectedCategory) {
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorPink
                strokeWidth = dpToPx(2).toFloat()
            }
            val lineY = height - dpToPx(4).toFloat()
            canvas.drawLine(width * 0.2f, lineY, width * 0.8f, lineY, linePaint)
        }
    }

    // ── Emoji ────────────────────────────────────────────────────────────────

    private fun drawEmoji(canvas: Canvas, emoji: String) {
        mainPaint.textSize = sp(if (isEmojiGridKey) 30f else 18f)
        mainPaint.color = colorWhite
        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText(emoji, cx, cy, mainPaint)
    }

    // ── Action ───────────────────────────────────────────────────────────────

    private fun drawAction(canvas: Canvas, k: Key.Action) {
        when (k.type) {
            ActionType.BACKSPACE       -> drawLabel(canvas, "⌫", sp(20f), colorPink)
            ActionType.SHIFT           -> drawShift(canvas)
            ActionType.SEARCH          -> drawLabel(canvas, "⌕", sp(22f), colorWhite)
            ActionType.DONE            -> drawLabel(canvas, "✓", sp(20f), colorPink)
            ActionType.SWITCH_TO_EMOJI -> drawLabel(canvas, "☺", sp(20f), colorWhite)
            ActionType.SPACE           -> drawLabel(canvas, "space", sp(14f), colorHint)
            ActionType.SWITCH_TO_SYMBOLS,
            ActionType.SWITCH_TO_QWERTY,
            ActionType.SWITCH_FROM_EMOJI,
            ActionType.SWITCH_TO_MORE_SYMBOLS -> drawLabel(canvas, actionLabel(k.type), sp(15f), colorWhite)
            ActionType.COMMA           -> drawLabel(canvas, ",", sp(22f), colorWhite)
            ActionType.PERIOD          -> drawLabel(canvas, ".", sp(22f), colorWhite)
            ActionType.UNDERSCORE      -> drawLabel(canvas, "_", sp(22f), colorWhite)
            ActionType.SLASH           -> drawLabel(canvas, "/", sp(22f), colorWhite)
            else                       -> drawLabel(canvas, actionLabel(k.type), sp(15f), colorWhite)
        }
    }

    private fun drawLabel(canvas: Canvas, label: String, textSize: Float, color: Int) {
        mainPaint.textSize = textSize
        mainPaint.color = color
        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText(label, cx, cy, mainPaint)
    }

    private fun drawShift(canvas: Canvas) {
        val color = if (shiftState == ShiftState.CAPS_LOCK) colorPink else colorWhite
        drawLabel(canvas, "⇧", sp(20f), color)
        if (shiftState == ShiftState.CAPS_LOCK) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = colorPink; strokeWidth = dpToPx(2).toFloat()
            }
            canvas.drawLine(width * 0.3f, height - dpToPx(8).toFloat(),
                width * 0.7f, height - dpToPx(8).toFloat(), p)
        }
    }

    private fun actionLabel(type: ActionType): String = when (type) {
        ActionType.SPACE                  -> "space"
        ActionType.SWITCH_TO_SYMBOLS      -> "?123"
        ActionType.SWITCH_TO_QWERTY       -> "ABC"
        ActionType.SWITCH_FROM_EMOJI      -> "ABC"
        ActionType.SWITCH_TO_MORE_SYMBOLS -> "=\\<"
        ActionType.COMMA                  -> ","
        ActionType.PERIOD                 -> "."
        ActionType.UNDERSCORE             -> "_"
        ActionType.SLASH                  -> "/"
        ActionType.ENTER                  -> "↵"
        ActionType.DONE                   -> "✓"
        else -> ""
    }

    private fun mainLabel(key: Key?): String? = when (key) {
        is Key.Letter       -> key.char.uppercaseChar().toString()
        is Key.Symbol       -> key.char.toString()
        is Key.Emoji        -> key.emoji
        is Key.CategoryIcon -> key.category.symbol
        is Key.Action       -> actionLabel(key.type)
        null -> null
    }

    // ── Touch ────────────────────────────────────────────────────────────────

    override fun setPressed(pressed: Boolean) {
        if (isKeyPressed != pressed) { isKeyPressed = pressed; invalidate() }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val k = key ?: return false
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                setPressed(true)
                when {
                    k is Key.CategoryIcon -> onCategoryTap?.invoke(k.category)
                    k is Key.Action && k.type == ActionType.BACKSPACE -> {
                        // Route backspace to GIF search if interceptor is active
                        if (gifSearchInterceptor?.invoke(k) != true) {
                            controller?.onBackspaceDown()
                        }
                    }
                    k is Key.Action && k.type == ActionType.SHIFT -> controller?.onShiftTapped()
                    else -> {
                        // Route letter/symbol to GIF search if interceptor is active
                        if (gifSearchInterceptor?.invoke(k) != true) {
                            controller?.onKeyTapped(k)
                        }
                    }
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                setPressed(false)
                if (k is Key.Action && k.type == ActionType.BACKSPACE) {
                    // Only cancel repeat if not intercepted
                    if (gifSearchInterceptor == null) controller?.onBackspaceUp()
                }
                true
            }
            else -> false
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun sp(sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
}
