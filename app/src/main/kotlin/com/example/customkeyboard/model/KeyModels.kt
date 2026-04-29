package com.example.customkeyboard.model

/**
 * Represents a single key on the keyboard.
 */
sealed class Key {
    /** A letter key that produces a character subject to shift state. */
    data class Letter(val char: Char) : Key()

    /** A symbol key that always produces its character verbatim. */
    data class Symbol(val char: Char) : Key()

    /** An action key (Shift, Backspace, Space, Enter, layer switch, etc.). */
    data class Action(val type: ActionType) : Key()
}

/**
 * The type of action performed by a [Key.Action] key.
 */
enum class ActionType {
    SHIFT,
    BACKSPACE,
    SPACE,
    ENTER,
    SWITCH_TO_SYMBOLS,
    SWITCH_TO_QWERTY,
    SWITCH_TO_EMOJI,
    SWITCH_FROM_EMOJI
}

/**
 * The current shift/caps-lock state of the keyboard.
 *
 * - [OFF]       — no shift active; letters are lower-case.
 * - [SINGLE]    — shift active for the next letter only; reverts to [OFF] after one commit.
 * - [CAPS_LOCK] — all letters are upper-case until explicitly toggled off.
 */
enum class ShiftState {
    OFF,
    SINGLE,
    CAPS_LOCK
}

/**
 * The currently visible keyboard layer.
 *
 * - [QWERTY]  — the standard alphabetic layout.
 * - [SYMBOL]  — the numbers and punctuation layout.
 * - [EMOJI]   — the emoji picker layout.
 */
enum class KeyboardLayer {
    QWERTY,
    SYMBOL,
    EMOJI
}
