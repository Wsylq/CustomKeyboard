package com.example.customkeyboard.model

import com.example.customkeyboard.model.Key.Action
import com.example.customkeyboard.model.Key.CategoryIcon
import com.example.customkeyboard.model.Key.Emoji
import com.example.customkeyboard.model.ActionType.*

/**
 * Emoji keyboard layout matching the screenshot:
 *
 * Category bar: Unicode-drawn icons (no actual emoji) + backspace
 * Grid: 4 rows × 6 emoji tiles
 * Bottom row: ABC [SPACE] ABC
 */
object EmojiLayout {

    /** Category bar — Unicode symbols drawn via Canvas, plus backspace at the end. */
    val categoryBar: List<Key> = EmojiCategory.entries.map { CategoryIcon(it) } +
            listOf(Action(BACKSPACE))

    /** Main emoji grid — 6 per row, 4 rows. */
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

    /** All grid rows + bottom row — used by KeyboardView for layout. */
    val rows: List<List<Key>> = emojiRows + listOf(bottomRow)
}
