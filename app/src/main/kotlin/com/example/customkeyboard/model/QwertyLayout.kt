package com.example.customkeyboard.model

import com.example.customkeyboard.model.Key.Action
import com.example.customkeyboard.model.Key.Letter
import com.example.customkeyboard.model.ActionType.BACKSPACE
import com.example.customkeyboard.model.ActionType.ENTER
import com.example.customkeyboard.model.ActionType.SHIFT
import com.example.customkeyboard.model.ActionType.SPACE
import com.example.customkeyboard.model.ActionType.SWITCH_TO_SYMBOLS

object QwertyLayout {
    val rows: List<List<Key>> = listOf(
        // Row 0 — top row: Q–P
        listOf(
            Letter('q'), Letter('w'), Letter('e'), Letter('r'), Letter('t'),
            Letter('y'), Letter('u'), Letter('i'), Letter('o'), Letter('p')
        ),
        // Row 1 — home row: A–L
        listOf(
            Letter('a'), Letter('s'), Letter('d'), Letter('f'), Letter('g'),
            Letter('h'), Letter('j'), Letter('k'), Letter('l')
        ),
        // Row 2 — shift row: Shift, Z–M, Backspace
        listOf(
            Action(SHIFT),
            Letter('z'), Letter('x'), Letter('c'), Letter('v'),
            Letter('b'), Letter('n'), Letter('m'),
            Action(BACKSPACE)
        ),
        // Row 3 — bottom row: SwitchToSymbols, Space, Enter
        listOf(
            Action(SWITCH_TO_SYMBOLS),
            Action(SPACE),
            Action(ENTER)
        )
    )

    /** Flat list of all 26 Letter keys derived from [rows], used by property tests. */
    val allLetterKeys: List<Letter> = rows
        .flatten()
        .filterIsInstance<Letter>()
}
