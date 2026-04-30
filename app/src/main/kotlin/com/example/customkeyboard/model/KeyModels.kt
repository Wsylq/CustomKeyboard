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

    /** An emoji category bar icon — drawn via Canvas, no actual emoji rendering. */
    data class CategoryIcon(val category: EmojiCategory) : Key()
}

/**
 * Emoji category bar icons — drawn as Unicode symbols or simple Canvas shapes,
 * never as actual emoji glyphs.
 */
enum class EmojiCategory(
    /** Unicode character(s) to draw for this category icon. */
    val symbol: String
) {
    RECENT("⏱"),        // clock outline — U+23F1
    SMILEYS("☺"),       // white smiling face — U+263A
    NATURE("✿"),        // flower — U+273F
    PEOPLE("⚇"),        // people silhouette — U+2687 (circled dot)
    TRAVEL("⬡"),        // hexagon (car placeholder) — U+2B21
    ACTIVITIES("◉"),    // bullseye (sports) — U+25C9
    OBJECTS("✦"),       // star (objects) — U+2726
    SYMBOLS("▲"),       // triangle — U+25B2
    FLAGS("⚑"),         // flag — U+2691
    EMOTICONS(":-)")    // text emoticon
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
