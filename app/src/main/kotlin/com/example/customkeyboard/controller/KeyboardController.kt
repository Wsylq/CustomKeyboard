package com.example.customkeyboard.controller

import com.example.customkeyboard.model.ActionType
import com.example.customkeyboard.model.Key
import com.example.customkeyboard.model.KeyboardLayer
import com.example.customkeyboard.model.ShiftState

/**
 * Pure Kotlin controller for keyboard state management.
 *
 * Has no Android UI dependencies, making it independently testable on the JVM.
 *
 * @param inputActions Abstraction over [android.view.inputmethod.InputConnection] operations.
 * @param viewActions  Abstraction over view-update operations.
 * @param clock        Returns the current uptime in milliseconds. Defaults to
 *                     [android.os.SystemClock.uptimeMillis] in production; can be replaced
 *                     with a fake clock in tests.
 * @param handler      Abstraction over [android.os.Handler] for scheduling backspace repeat.
 *                     Defaults to [DefaultHandlerWrapper] (main-looper handler) in production;
 *                     can be replaced with a fake in tests.
 */
class KeyboardController(
    private val inputActions: InputActions,
    private val viewActions: ViewActions,
    private val clock: () -> Long = { android.os.SystemClock.uptimeMillis() },
    private val handler: HandlerWrapper = DefaultHandlerWrapper()
) {
    companion object {
        const val IME_ACTION_NONE = 1
        const val IME_ACTION_DONE = 2
        const val IME_ACTION_SEARCH = 3
        const val IME_ACTION_SEND = 4
        const val IME_ACTION_GO = 5
        const val IME_ACTION_NEXT = 6

        /** Delay before backspace repeat begins after the key is held down. */
        const val BACKSPACE_INITIAL_DELAY_MS = 400L

        /** Interval between successive backspace deletions during a hold. */
        const val BACKSPACE_REPEAT_INTERVAL_MS = 50L
    }

    /** Current shift / caps-lock state. */
    var shiftState: ShiftState = ShiftState.OFF
        private set

    /** Currently visible keyboard layer. */
    var currentLayer: KeyboardLayer = KeyboardLayer.QWERTY
        private set

    /** Timestamp of the most recent shift tap, used for double-tap detection. */
    private var lastShiftTapTime: Long = 0L

    /** Current IME options, used to determine the Enter key action. */
    private var currentImeOptions: Int = IME_ACTION_NONE

    /** The currently scheduled backspace repeat runnable, or null if no repeat is pending. */
    private var repeatRunnable: Runnable? = null

    /**
     * Called when a new input session starts. Stores the current IME options and
     * updates the Enter key label accordingly.
     *
     * @param imeOptions The IME options from [android.view.inputmethod.EditorInfo.imeOptions].
     */
    fun onStartInput(imeOptions: Int) {
        currentImeOptions = imeOptions
        viewActions.updateEnterLabel(imeOptions)
    }

    // -------------------------------------------------------------------------
    // Shift state machine
    // -------------------------------------------------------------------------

    /**
     * Handles a tap on the Shift key.
     *
     * Transition table:
     * - OFF       + single tap           → SINGLE
     * - SINGLE    + single tap           → OFF
     * - OFF/SINGLE + double tap (≤400ms) → CAPS_LOCK
     * - CAPS_LOCK + single tap           → OFF
     */
    fun onShiftTapped() {
        val now = clock()
        val isDoubleTap = (now - lastShiftTapTime) <= 400L
        lastShiftTapTime = now

        val nextState = when {
            // Double-tap from OFF or SINGLE → CAPS_LOCK
            isDoubleTap && shiftState != ShiftState.CAPS_LOCK -> ShiftState.CAPS_LOCK
            // Normal single-tap transitions
            shiftState == ShiftState.OFF -> ShiftState.SINGLE
            shiftState == ShiftState.SINGLE -> ShiftState.OFF
            shiftState == ShiftState.CAPS_LOCK -> ShiftState.OFF
            else -> ShiftState.OFF
        }

        shiftState = nextState
        viewActions.updateShiftIndicator(shiftState)
    }

    // -------------------------------------------------------------------------
    // Key tapped — stubs (implemented in Tasks 4 and 5)
    // -------------------------------------------------------------------------

    /**
     * Handles a tap on any non-shift, non-backspace key.
     *
     * - [Key.Letter]: commits the character in the correct case based on [shiftState].
     *   If [shiftState] is [ShiftState.SINGLE], transitions back to [ShiftState.OFF] after commit.
     * - [Key.Symbol]: commits the character verbatim with no case transformation.
     * - [Key.Action]: handles layer switching, shift, space, enter, and backspace actions.
     */
    fun onKeyTapped(key: Key) {
        when (key) {
            is Key.CategoryIcon -> { /* category bar tap — no text output */ }
            is Key.Letter -> {
                val char = if (shiftState == ShiftState.OFF) key.char.lowercaseChar() else key.char.uppercaseChar()
                inputActions.commitText(char.toString())
                if (shiftState == ShiftState.SINGLE) {
                    shiftState = ShiftState.OFF
                    viewActions.updateShiftIndicator(ShiftState.OFF)
                }
            }
            is Key.Symbol -> {
                inputActions.commitText(key.char.toString())
            }
            is Key.Emoji -> {
                inputActions.commitText(key.emoji)
            }
            is Key.Action -> when (key.type) {
                ActionType.SWITCH_TO_SYMBOLS -> {
                    viewActions.dismissKeyPreview()
                    currentLayer = KeyboardLayer.SYMBOL
                    viewActions.switchLayer(KeyboardLayer.SYMBOL)
                }
                ActionType.SWITCH_TO_QWERTY -> {
                    viewActions.dismissKeyPreview()
                    currentLayer = KeyboardLayer.QWERTY
                    viewActions.switchLayer(KeyboardLayer.QWERTY)
                }
                ActionType.SWITCH_TO_EMOJI -> {
                    viewActions.dismissKeyPreview()
                    currentLayer = KeyboardLayer.EMOJI
                    viewActions.switchLayer(KeyboardLayer.EMOJI)
                }
                ActionType.SWITCH_FROM_EMOJI -> {
                    viewActions.dismissKeyPreview()
                    currentLayer = KeyboardLayer.QWERTY
                    viewActions.switchLayer(KeyboardLayer.QWERTY)
                }
                ActionType.SHIFT -> onShiftTapped()
                ActionType.SPACE -> inputActions.commitText(" ")
                ActionType.COMMA -> inputActions.commitText(",")
                ActionType.PERIOD -> inputActions.commitText(".")
                ActionType.SEARCH, ActionType.ENTER -> {
                    val actionCode = when (currentImeOptions and 0xFF) {
                        IME_ACTION_DONE -> IME_ACTION_DONE
                        IME_ACTION_SEARCH -> IME_ACTION_SEARCH
                        IME_ACTION_SEND -> IME_ACTION_SEND
                        IME_ACTION_GO -> IME_ACTION_GO
                        IME_ACTION_NEXT -> IME_ACTION_NEXT
                        else -> IME_ACTION_NONE
                    }
                    inputActions.performEditorAction(actionCode)
                }
                ActionType.BACKSPACE -> { /* handled via onBackspaceDown/Up */ }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Backspace — stubs (implemented in Task 5)
    // -------------------------------------------------------------------------

    /**
     * Called when the Backspace key is pressed down.
     *
     * Immediately deletes the character before the cursor, then schedules a repeat runnable
     * that fires after [BACKSPACE_INITIAL_DELAY_MS] ms and continues every
     * [BACKSPACE_REPEAT_INTERVAL_MS] ms until [onBackspaceUp] is called.
     */
    fun onBackspaceDown() {
        inputActions.deleteCharBefore()
        val repeat = object : Runnable {
            override fun run() {
                inputActions.deleteCharBefore()
                handler.postDelayed(this, BACKSPACE_REPEAT_INTERVAL_MS)
            }
        }
        repeatRunnable = repeat
        handler.postDelayed(repeat, BACKSPACE_INITIAL_DELAY_MS)
    }

    /**
     * Called when the Backspace key is released.
     *
     * Cancels any pending repeat callbacks scheduled by [onBackspaceDown].
     */
    fun onBackspaceUp() {
        repeatRunnable?.let { handler.removeCallbacks(it) }
        repeatRunnable = null
    }
}
