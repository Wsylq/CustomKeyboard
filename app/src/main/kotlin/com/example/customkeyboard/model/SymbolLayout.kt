package com.example.customkeyboard.model

import com.example.customkeyboard.model.Key.Action
import com.example.customkeyboard.model.Key.Symbol
import com.example.customkeyboard.model.ActionType.*

/**
 * Symbol layout matching the screenshot exactly:
 *
 * Row 0: 1 2 3 4 5 6 7 8 9 0
 * Row 1: @ # ₹ % & - + ( )          (9 keys, centred)
 * Row 2: =\< * " ' : ; ! ? ⌫        (=\< is wide action, 8 symbol keys, backspace)
 * Row 3: ABC , _ [SPACE] / . ✓
 */
object SymbolLayout {

    val rows: List<List<Key>> = listOf(
        // Row 0: digits — 10 equal keys
        listOf(
            Symbol('1'), Symbol('2'), Symbol('3'), Symbol('4'), Symbol('5'),
            Symbol('6'), Symbol('7'), Symbol('8'), Symbol('9'), Symbol('0')
        ),
        // Row 1: 9 symbol keys, centred (same as home row treatment)
        listOf(
            Symbol('@'), Symbol('#'), Symbol('₹'), Symbol('%'), Symbol('&'),
            Symbol('-'), Symbol('+'), Symbol('('), Symbol(')')
        ),
        // Row 2: =\< (wide), 8 symbols, backspace (wide)
        listOf(
            Action(SWITCH_TO_MORE_SYMBOLS),
            Symbol('*'), Symbol('"'), Symbol('\''), Symbol(':'),
            Symbol(';'), Symbol('!'), Symbol('?'),
            Action(BACKSPACE)
        ),
        // Row 3: ABC, comma, underscore, space, slash, period, checkmark
        listOf(
            Action(SWITCH_TO_QWERTY),
            Action(COMMA),
            Action(UNDERSCORE),
            Action(SPACE),
            Action(SLASH),
            Action(PERIOD),
            Action(DONE)
        )
    )

    val allSymbolKeys: List<Key.Symbol> = rows.flatten().filterIsInstance<Key.Symbol>()
}
