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
import com.example.customkeyboard.model.Key
import com.example.customkeyboard.model.ShiftState

/**
 * Custom [View] for a single keyboard key.
 *
 * - Draws a rounded-rectangle background: [normalColor] at rest, [pressedColor] when pressed.
 * - Draws the key label centred in white, sans-serif, 14sp minimum.
 * - Enforces a minimum touch target of 44dp × 44dp.
 * - For emoji keys, [emojiString] overrides the single-char label so multi-codepoint emojis
 *   are committed and displayed correctly.
 * - Exposes [shiftState] so [KeyboardView] can update the Shift key indicator without
 *   re-inflating the whole row.
 */
class KeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var key: Key? = null
        set(value) {
            field = value
            contentDescription = labelFor(value)
            invalidate()
        }

    var controller: KeyboardController? = null

    /** Full emoji string for multi-codepoint emojis; null for all other keys. */
    var emojiString: String? = null
        set(value) {
            field = value
            invalidate()
        }

    /** Current shift state — only meaningful when [key] is [Key.Action] SHIFT. */
    var shiftState: ShiftState = ShiftState.OFF
        set(value) {
            field = value
            invalidate()
        }

    private var isKeyPressed = false

    private val normalColor  = Color.parseColor("#37474F")
    private val pressedColor = Color.parseColor("#1C272B")
    private val textColor    = Color.WHITE

    private val bgPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = textColor
        textAlign = Paint.Align.CENTER
        typeface  = android.graphics.Typeface.SANS_SERIF
    }
    private val bgRect = RectF()

    private val cornerRadiusPx: Float
        get() = dpToPx(6).toFloat()

    private val minTouchTargetPx: Int
        get() = dpToPx(44)

    // -------------------------------------------------------------------------
    // Measure
    // -------------------------------------------------------------------------

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = maxOf(measuredWidth,  minTouchTargetPx)
        val h = maxOf(measuredHeight, minTouchTargetPx)
        setMeasuredDimension(w, h)
    }

    // -------------------------------------------------------------------------
    // Draw
    // -------------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background
        bgPaint.color = if (isKeyPressed) pressedColor else normalColor

        // Shift key: tint background to indicate state
        if (key is Key.Action && (key as Key.Action).type == ActionType.SHIFT) {
            bgPaint.color = when (shiftState) {
                ShiftState.OFF       -> if (isKeyPressed) pressedColor else normalColor
                ShiftState.SINGLE    -> Color.parseColor("#546E7A")
                ShiftState.CAPS_LOCK -> Color.parseColor("#80CBC4")
            }
        }

        bgRect.set(2f, 2f, width - 2f, height - 2f)
        canvas.drawRoundRect(bgRect, cornerRadiusPx, cornerRadiusPx, bgPaint)

        // Label
        val label = emojiString ?: labelFor(key) ?: return
        val isEmoji = emojiString != null

        val textSizeSp = if (isEmoji) 22f else 14f
        textPaint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, textSizeSp, resources.displayMetrics
        )
        textPaint.color = textColor

        val cx = width  / 2f
        val cy = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, cx, cy, textPaint)
    }

    // -------------------------------------------------------------------------
    // Touch
    // -------------------------------------------------------------------------

    override fun setPressed(pressed: Boolean) {
        if (isKeyPressed != pressed) {
            isKeyPressed = pressed
            invalidate()
        }
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
                    else -> {
                        // For emoji keys, commit the full string via a synthetic Symbol key
                        val commitKey = if (emojiString != null && k is Key.Symbol) {
                            // We'll handle this in the controller via onEmojiTapped
                            null
                        } else k
                        if (commitKey != null) {
                            controller?.onKeyTapped(commitKey)
                        } else {
                            // Emoji: commit directly
                            emojiString?.let { controller?.onEmojiTapped(it) }
                        }
                    }
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                setPressed(false)
                if (k is Key.Action && k.type == ActionType.BACKSPACE) {
                    controller?.onBackspaceUp()
                }
                true
            }
            else -> false
        }
    }

    // -------------------------------------------------------------------------
    // Label helpers
    // -------------------------------------------------------------------------

    private fun labelFor(key: Key?): String? = when (key) {
        is Key.Letter -> key.char.uppercaseChar().toString()
        is Key.Symbol -> key.char.toString()
        is Key.Action -> when (key.type) {
            ActionType.SHIFT            -> "⇧"
            ActionType.BACKSPACE        -> "⌫"
            ActionType.SPACE            -> "space"
            ActionType.ENTER            -> "↵"
            ActionType.SWITCH_TO_SYMBOLS  -> "?123"
            ActionType.SWITCH_TO_QWERTY   -> "ABC"
            ActionType.SWITCH_TO_EMOJI    -> "😊"
            ActionType.SWITCH_FROM_EMOJI  -> "ABC"
        }
        null -> null
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
}
