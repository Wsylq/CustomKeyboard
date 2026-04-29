package com.example.customkeyboard.model

import com.example.customkeyboard.model.Key.Action
import com.example.customkeyboard.model.Key.Symbol
import com.example.customkeyboard.model.ActionType.BACKSPACE
import com.example.customkeyboard.model.ActionType.SPACE
import com.example.customkeyboard.model.ActionType.SWITCH_TO_QWERTY

object SymbolLayout {
    val rows: List<List<Key>> = listOf(
        // Row 0: digits
        listOf(
            Symbol('1'), Symbol('2'), Symbol('3'), Symbol('4'), Symbol('5'),
            Symbol('6'), Symbol('7'), Symbol('8'), Symbol('9'), Symbol('0')
        ),
        // Row 1: punctuation 1
        listOf(
            Symbol('!'), Symbol('@'), Symbol('#'), Symbol('$'), Symbol('%'),
            Symbol('^'), Symbol('&'), Symbol('*'), Symbol('('), Symbol(')')
        ),
        // Row 2: punctuation 2
        listOf(
            Symbol('-'), Symbol('_'), Symbol('='), Symbol('+'), Symbol('['),
            Symbol(']'), Symbol('{'), Symbol('}'), Symbol(';'), Symbol(':')
        ),
        // Row 3: punctuation 3
        listOf(
            Symbol('\''), Symbol('"'), Symbol(','), Symbol('.'), Symbol('<'),
            Symbol('>'), Symbol('/'), Symbol('?'), Symbol('\\'), Symbol('|')
        ),
        // Row 4: bottom action row
        listOf(
            Action(SWITCH_TO_QWERTY), Action(SPACE), Action(BACKSPACE)
        )
    )

    /** Flat list of all Symbol keys across all rows — used by property tests. */
    val allSymbolKeys: List<Key.Symbol> = rows
        .flatten()
        .filterIsInstance<Key.Symbol>()
}
