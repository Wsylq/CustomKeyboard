package com.example.customkeyboard.model

/**
 * Represents a single key on the keyboard.
 */
sealed class Key {
    /** A letter key that produces a character subject to shift state. */
    data class Letter(val char: Char, val hint: String? = null) : Key()

    /** A symbol key that always produces its character verbatim. */
    data class Symbol(val char: Char) : Key()

    /** An action key (Shift, Backspace, Space, Enter, layer switch, etc.). */
    data class Action(val type: ActionType) : Key()

    /** An emoji key — stores the full emoji string directly. */
    data class Emoji(val emoji: String) : Key()
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
    SWITCH_FROM_EMOJI,
    COMMA,      // bottom row comma key
    PERIOD,     // bottom row period key
    SEARCH      // bottom row search/enter key
}

/**
 * The current shift/caps-lock state of the keyboard.
 */
enum class ShiftState { OFF, SINGLE, CAPS_LOCK }

/**
 * The currently visible keyboard layer.
 */
enum class KeyboardLayer { QWERTY, SYMBOL, EMOJI }
