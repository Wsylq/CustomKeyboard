package com.example.customkeyboard.model

import com.example.customkeyboard.model.Key.Action
import com.example.customkeyboard.model.Key.Symbol
import com.example.customkeyboard.model.ActionType.BACKSPACE
import com.example.customkeyboard.model.ActionType.SPACE
import com.example.customkeyboard.model.ActionType.SWITCH_FROM_EMOJI

/**
 * Emoji keyboard layer — a curated set of frequently used emojis arranged in rows of 8,
 * matching the Gboard-style emoji picker layout.
 *
 * Each emoji is stored as a [Key.Symbol] whose [Key.Symbol.char] is the first code point of
 * the emoji string. For multi-codepoint emojis the full string is committed via the
 * [emojiStrings] map keyed by that first code point.
 *
 * The bottom row mirrors the QWERTY bottom row: back-to-QWERTY, space, backspace.
 */
object EmojiLayout {

    /**
     * Full emoji strings indexed by the first code point of each emoji.
     * Used by [KeyboardController] / [KeyView] to commit the correct string.
     */
    val emojiStrings: Map<Int, String> = linkedMapOf(
        // Smileys & people
        0x1F600 to "😀", 0x1F603 to "😃", 0x1F604 to "😄", 0x1F601 to "😁",
        0x1F606 to "😆", 0x1F605 to "😅", 0x1F923 to "🤣", 0x1F602 to "😂",
        0x1F642 to "🙂", 0x1F643 to "🙃", 0x1F609 to "😉", 0x1F60A to "😊",
        0x1F607 to "😇", 0x1F970 to "🥰", 0x1F60D to "😍", 0x1F929 to "🤩",
        0x1F618 to "😘", 0x1F617 to "😗", 0x1F61A to "😚", 0x1F619 to "😙",
        0x1F972 to "🥲", 0x1F60B to "😋", 0x1F61B to "😛", 0x1F61C to "😜",
        0x1F92A to "🤪", 0x1F61D to "😝", 0x1F911 to "🤑", 0x1F917 to "🤗",
        0x1F92D to "🤭", 0x1F92B to "🤫", 0x1F914 to "🤔", 0x1F910 to "🤐",
        // Gestures & hands
        0x1F44D to "👍", 0x1F44E to "👎", 0x1F44F to "👏", 0x1F64C to "🙌",
        0x1F91D to "🤝", 0x1F44B to "👋", 0x270C to "✌️", 0x1F91E to "🤞",
        // Hearts
        0x2764 to "❤️", 0x1F9E1 to "🧡", 0x1F49B to "💛", 0x1F49A to "💚",
        0x1F499 to "💙", 0x1F49C to "💜", 0x1F5A4 to "🖤", 0x1F90D to "🤍",
        // Nature & animals
        0x1F436 to "🐶", 0x1F431 to "🐱", 0x1F42D to "🐭", 0x1F439 to "🐹",
        0x1F430 to "🐰", 0x1F98A to "🦊", 0x1F43B to "🐻", 0x1F43C to "🐼",
        // Food
        0x1F355 to "🍕", 0x1F354 to "🍔", 0x1F35F to "🍟", 0x1F32D to "🌭",
        0x1F96A to "🥪", 0x1F32E to "🌮", 0x1F32F to "🌯", 0x1F9C6 to "🧆",
        // Activities & objects
        0x26BD to "⚽", 0x1F3C0 to "🏀", 0x1F3C8 to "🏈", 0x26BE to "⚾",
        0x1F3BE to "🎾", 0x1F3B5 to "🎵", 0x1F3B6 to "🎶", 0x1F389 to "🎉",
        // Travel
        0x1F697 to "🚗", 0x2708 to "✈️", 0x1F680 to "🚀", 0x1F30D to "🌍",
        0x1F3E0 to "🏠", 0x1F4F1 to "📱", 0x1F4BB to "💻", 0x1F4A1 to "💡",
        // Symbols
        0x2705 to "✅", 0x274C to "❌", 0x2714 to "✔️", 0x2757 to "❗",
        0x2753 to "❓", 0x1F525 to "🔥", 0x1F4AF to "💯", 0x2B50 to "⭐"
    )

    private val emojiKeys: List<Key> = emojiStrings.keys.map { codePoint ->
        // Store the first char of the emoji as the Symbol char; full string is in emojiStrings
        Symbol(Character.toChars(codePoint)[0])
    }

    /** Rows of 8 emoji keys each, plus a bottom action row. */
    val rows: List<List<Key>> by lazy {
        val emojiRows = emojiKeys.chunked(8)
        emojiRows + listOf(
            listOf(Action(SWITCH_FROM_EMOJI), Action(SPACE), Action(BACKSPACE))
        )
    }
}
