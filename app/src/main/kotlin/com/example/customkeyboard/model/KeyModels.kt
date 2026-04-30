package com.example.customkeyboard.model

sealed class Key {
    data class Letter(val char: Char, val hint: String? = null) : Key()
    data class Symbol(val char: Char) : Key()
    data class Action(val type: ActionType) : Key()
    data class Emoji(val emoji: String) : Key()
    data class CategoryIcon(val category: EmojiCategory) : Key()
}

enum class EmojiCategory(val symbol: String) {
    RECENT("⏱"),
    SMILEYS("☺"),
    NATURE("✿"),
    PEOPLE("⚇"),
    TRAVEL("⬡"),
    ACTIVITIES("◉"),
    OBJECTS("✦"),
    SYMBOLS("▲"),
    FLAGS("⚑"),
    EMOTICONS(":-)"),
    GIF("GIF")   // Klipy GIF tab
}

enum class ActionType {
    SHIFT,
    BACKSPACE,
    SPACE,
    ENTER,
    SWITCH_TO_SYMBOLS,
    SWITCH_TO_QWERTY,
    SWITCH_TO_EMOJI,
    SWITCH_FROM_EMOJI,
    SWITCH_TO_MORE_SYMBOLS,
    COMMA,
    PERIOD,
    SEARCH,
    UNDERSCORE,
    SLASH,
    DONE
}

enum class ShiftState { OFF, SINGLE, CAPS_LOCK }
enum class KeyboardLayer { QWERTY, SYMBOL, EMOJI }
