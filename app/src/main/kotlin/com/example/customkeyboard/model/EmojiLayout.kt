package com.example.customkeyboard.model

import com.example.customkeyboard.model.Key.Action
import com.example.customkeyboard.model.Key.Emoji
import com.example.customkeyboard.model.ActionType.*

/**
 * Emoji keyboard layout matching the screenshot:
 *
 * - Category bar: 10 category icons + backspace (rendered separately in KeyboardView)
 * - 4 rows of 6 large emoji tiles
 * - Bottom row: ABC [SPACE] ABC
 *
 * The emojis shown in the screenshot (most-used / trending):
 * Row 1: 😭 😂 💔 ✌️ 😋 💀
 * Row 2: 🙏 👍 😔 😈 😍 😘
 * Row 3: 🤫 🤩 😏 😎 😁 😐
 * Row 4: 😊 😡 🧨 😕 🌍  (5 in last row)
 */
object EmojiLayout {

    /** Emojis displayed in the main grid — 6 per row, 4 rows = 23 shown. */
    val emojiRows: List<List<Key>> = listOf(
        listOf(Emoji("😭"), Emoji("😂"), Emoji("💔"), Emoji("✌️"), Emoji("😋"), Emoji("💀")),
        listOf(Emoji("🙏"), Emoji("👍"), Emoji("😔"), Emoji("😈"), Emoji("😍"), Emoji("😘")),
        listOf(Emoji("🤫"), Emoji("🤩"), Emoji("😏"), Emoji("😎"), Emoji("😁"), Emoji("😐")),
        listOf(Emoji("😊"), Emoji("😡"), Emoji("🧨"), Emoji("😕"), Emoji("🌍"), Emoji("🔥"))
    )

    /** Bottom action row: ABC [SPACE] ABC */
    val bottomRow: List<Key> = listOf(
        Action(SWITCH_FROM_EMOJI),
        Action(SPACE),
        Action(SWITCH_FROM_EMOJI)
    )

    /** Category bar icons (display only — tapping any switches to that category in future) */
    val categoryIcons: List<String> = listOf("🕐", "😊", "🌸", "👥", "🚗", "🏀", "👑", "▲", "🏳", ":-)")

    /** All rows including bottom — used by KeyboardView for layout. */
    val rows: List<List<Key>> = emojiRows + listOf(bottomRow)
}
