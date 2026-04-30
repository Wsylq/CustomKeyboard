package com.example.customkeyboard.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.example.customkeyboard.controller.KeyboardController
import com.example.customkeyboard.model.ActionType
import com.example.customkeyboard.model.Key
import com.example.customkeyboard.model.ShiftState

/**
 * Single key view matching the iOS-style dark keyboard in the screenshot.
 *
 * Colours:
 *   - Keyboard background: #1C1C1E
 *   - Regular key:         #2C2C2E  (pressed: #3A3A3C)
 *   - Action key (Shift, ?123, etc.): #3A3A3C  (slightly lighter)
 *   - Backspace icon tint: #FF6B8A (pink)
 *   - Hint text:           #8E8E93 (grey)
 *   - Main label:          #FFFFFF
 */
class KeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var key: Key? = null
        set(value) {
            field = value
            contentDescription = mainLabel(value)
            invalidate()
        }

    var controller: KeyboardController? = null

    var shiftState: ShiftState = ShiftState.OFF
        set(value) { field = value; invalidate() }

    /** True when this key is inside the emoji grid (larger emoji, no background tint). */
    var isEmojiGridKey: Boolean = false
        set(value) { field = value; invalidate() }

    private var isKeyPressed = false

    // ── Colours ──────────────────────────────────────────────────────────────
    private val bgNormal   = Color.parseColor("#2C2C2E")
    private val bgAction   = Color.parseColor("#3A3A3C")   // shift, ?123, ABC, etc.
    private val bgPressed  = Color.parseColor("#48484A")
    private val bgSpace    = Color.parseColor("#3A3A3C")
    private val colorPink  = Color.parseColor("#FF6B8A")
    private val colorHint  = Color.parseColor("#8E8E93")
    private val colorWhite = Color.WHITE

    // ── Paints ───────────────────────────────────────────────────────────────
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface  = android.graphics.Typeface.create(
            android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface  = android.graphics.Typeface.SANS_SERIF
    }
    private val bgRect = RectF()

    private val cornerPx: Float get() = dpToPx(10).toFloat()
    private val minTouchPx: Int  get() = dpToPx(44)

    // ── Measure ──────────────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            maxOf(measuredWidth,  minTouchPx),
            maxOf(measuredHeight, minTouchPx)
        )
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val k = key ?: return

        // ── Background ──
        bgPaint.color = resolveBackground(k)
        bgRect.set(3f, 3f, width - 3f, height - 3f)
        canvas.drawRoundRect(bgRect, cornerPx, cornerPx, bgPaint)

        when (k) {
            is Key.Emoji -> drawEmoji(canvas, k.emoji)
            is Key.Action -> drawAction(canvas, k)
            is Key.Letter -> drawLetter(canvas, k)
            is Key.Symbol -> drawSymbol(canvas, k)
        }
    }

    private fun resolveBackground(k: Key): Int {
        if (isKeyPressed) return bgPressed
        return when {
            k is Key.Emoji -> bgNormal
            k is Key.Action && k.type == ActionType.SPACE -> bgSpace
            k is Key.Action -> bgAction
            else -> bgNormal
        }
    }

    // ── Letter key: large label + small hint top-right ───────────────────────

    private fun drawLetter(canvas: Canvas, k: Key.Letter) {
        val labelSp = 20f
        mainPaint.textSize = sp(labelSp)
        mainPaint.color = colorWhite

        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText(k.char.uppercaseChar().toString(), cx, cy, mainPaint)

        // Hint top-right
        if (k.hint != null) {
            hintPaint.textSize = sp(9f)
            hintPaint.color = colorHint
            val hx = width - dpToPx(5).toFloat()
            val hy = dpToPx(13).toFloat()
            canvas.drawText(k.hint, hx, hy, hintPaint)
        }
    }

    // ── Symbol key ───────────────────────────────────────────────────────────

    private fun drawSymbol(canvas: Canvas, k: Key.Symbol) {
        mainPaint.textSize = sp(18f)
        mainPaint.color = colorWhite
        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText(k.char.toString(), cx, cy, mainPaint)
    }

    // ── Emoji key ────────────────────────────────────────────────────────────

    private fun drawEmoji(canvas: Canvas, emoji: String) {
        mainPaint.textSize = sp(if (isEmojiGridKey) 30f else 22f)
        mainPaint.color = colorWhite
        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText(emoji, cx, cy, mainPaint)
    }

    // ── Action key ───────────────────────────────────────────────────────────

    private fun drawAction(canvas: Canvas, k: Key.Action) {
        when (k.type) {
            ActionType.BACKSPACE -> drawBackspace(canvas)
            ActionType.SHIFT     -> drawShift(canvas)
            ActionType.SEARCH    -> drawSearch(canvas)
            ActionType.SWITCH_TO_EMOJI -> drawActionLabel(canvas, "😊", sp(22f))
            else -> {
                val label = actionLabel(k.type)
                val textSp = when (k.type) {
                    ActionType.SPACE -> 14f
                    ActionType.SWITCH_TO_SYMBOLS, ActionType.SWITCH_TO_QWERTY,
                    ActionType.SWITCH_FROM_EMOJI -> 15f
                    ActionType.COMMA, ActionType.PERIOD -> 20f
                    else -> 15f
                }
                drawActionLabel(canvas, label, sp(textSp))
            }
        }
    }

    private fun drawActionLabel(canvas: Canvas, label: String, textSize: Float) {
        mainPaint.textSize = textSize
        mainPaint.color = colorWhite
        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText(label, cx, cy, mainPaint)
    }

    /** Draws the pink ◁✕ backspace icon. */
    private fun drawBackspace(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        // Draw a pink "⌫" symbol at a comfortable size
        mainPaint.textSize = sp(20f)
        mainPaint.color = colorPink
        val label = "⌫"
        val ty = cy - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText(label, cx, ty, mainPaint)
    }

    /** Draws the shift arrow, tinted based on shift state. */
    private fun drawShift(canvas: Canvas) {
        val color = when (shiftState) {
            ShiftState.OFF       -> colorWhite
            ShiftState.SINGLE    -> colorWhite
            ShiftState.CAPS_LOCK -> colorPink
        }
        mainPaint.textSize = sp(20f)
        mainPaint.color = color
        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText("⇧", cx, cy, mainPaint)

        // Underline for CAPS_LOCK
        if (shiftState == ShiftState.CAPS_LOCK) {
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = colorPink
                strokeWidth = dpToPx(2).toFloat()
            }
            val lineY = height - dpToPx(8).toFloat()
            canvas.drawLine(width * 0.3f, lineY, width * 0.7f, lineY, linePaint)
        }
    }

    /** Draws a 🔍 search icon. */
    private fun drawSearch(canvas: Canvas) {
        mainPaint.textSize = sp(20f)
        mainPaint.color = colorWhite
        val cx = width / 2f
        val cy = height / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
        canvas.drawText("🔍", cx, cy, mainPaint)
    }

    private fun actionLabel(type: ActionType): String = when (type) {
        ActionType.SPACE            -> "space"
        ActionType.SWITCH_TO_SYMBOLS  -> "?123"
        ActionType.SWITCH_TO_QWERTY   -> "ABC"
        ActionType.SWITCH_FROM_EMOJI  -> "ABC"
        ActionType.COMMA            -> ","
        ActionType.PERIOD           -> "."
        ActionType.ENTER            -> "↵"
        else -> ""
    }

    private fun mainLabel(key: Key?): String? = when (key) {
        is Key.Letter -> key.char.uppercaseChar().toString()
        is Key.Symbol -> key.char.toString()
        is Key.Emoji  -> key.emoji
        is Key.Action -> actionLabel(key.type)
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
                    k is Key.Action && k.type == ActionType.BACKSPACE -> controller?.onBackspaceDown()
                    k is Key.Action && k.type == ActionType.SHIFT     -> controller?.onShiftTapped()
                    else -> controller?.onKeyTapped(k)
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                setPressed(false)
                if (k is Key.Action && k.type == ActionType.BACKSPACE) controller?.onBackspaceUp()
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
