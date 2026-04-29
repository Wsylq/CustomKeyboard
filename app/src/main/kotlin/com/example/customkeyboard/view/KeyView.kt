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

    private var isKeyPressed = false

    private val normalColor = Color.parseColor("#37474F")
    private val pressedColor = Color.parseColor("#263238")
    private val textColor = Color.WHITE

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.SANS_SERIF
    }
    private val bgRect = RectF()

    private val cornerRadiusPx: Float
        get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics)

    private val minTouchTargetPx: Int
        get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 44f, resources.displayMetrics).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = maxOf(measuredWidth, minTouchTargetPx)
        val h = maxOf(measuredHeight, minTouchTargetPx)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bgPaint.color = if (isKeyPressed) pressedColor else normalColor
        bgRect.set(4f, 4f, width - 4f, height - 4f)
        canvas.drawRoundRect(bgRect, cornerRadiusPx, cornerRadiusPx, bgPaint)

        val label = labelFor(key)
        if (label != null) {
            val textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
            textPaint.textSize = textSizePx
            textPaint.color = textColor
            val cx = width / 2f
            val cy = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, cx, cy, textPaint)
        }
    }

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
                    k is Key.Action && k.type == ActionType.SHIFT -> controller?.onShiftTapped()
                    else -> controller?.onKeyTapped(k)
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

    private fun labelFor(key: Key?): String? = when (key) {
        is Key.Letter -> key.char.uppercaseChar().toString()
        is Key.Symbol -> key.char.toString()
        is Key.Action -> when (key.type) {
            ActionType.SHIFT -> "⇧"
            ActionType.BACKSPACE -> "⌫"
            ActionType.SPACE -> "space"
            ActionType.ENTER -> "↵"
            ActionType.SWITCH_TO_SYMBOLS -> "?123"
            ActionType.SWITCH_TO_QWERTY -> "ABC"
        }
        null -> null
    }
}
