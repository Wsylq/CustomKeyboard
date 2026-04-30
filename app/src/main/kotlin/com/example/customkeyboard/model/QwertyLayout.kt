package com.example.customkeyboard.model

import com.example.customkeyboard.model.Key.Action
import com.example.customkeyboard.model.Key.Letter
import com.example.customkeyboard.model.ActionType.*

/**
 * QWERTY layout matching the screenshot exactly:
 *
 * Row 0: q(1) w(2) e(3) r(4) t(5) y(6) u(7) i(8) o(9) p(0)  — 10 equal keys
 * Row 1:  a(@) s(#) d($) f(%) g(&) h(-) j(+) k(() l())       — 9 keys, centered
 * Row 2: [Shift] z(*) x(") c(') v(:) b(;) n(!) m(?) [⌫]      — wider action keys
 * Row 3: [?123] [,] [😊] [   SPACE   ] [.] [🔍]              — bottom bar
 */
object QwertyLayout {

    val rows: List<List<Key>> = listOf(
        // Row 0 — top row with number hints
        listOf(
            Letter('q', "1"), Letter('w', "2"), Letter('e', "3"),
            Letter('r', "4"), Letter('t', "5"), Letter('y', "6"),
            Letter('u', "7"), Letter('i', "8"), Letter('o', "9"), Letter('p', "0")
        ),
        // Row 1 — home row with symbol hints
        listOf(
            Letter('a', "@"), Letter('s', "#"), Letter('d', "$"),
            Letter('f', "%"), Letter('g', "&"), Letter('h', "-"),
            Letter('j', "+"), Letter('k', "("), Letter('l', ")")
        ),
        // Row 2 — shift row
        listOf(
            Action(SHIFT),
            Letter('z', "*"), Letter('x', "\""), Letter('c', "'"),
            Letter('v', ":"), Letter('b', ";"), Letter('n', "!"), Letter('m', "?"),
            Action(BACKSPACE)
        ),
        // Row 3 — bottom bar: ?123, comma, emoji, space, period, search
        listOf(
            Action(SWITCH_TO_SYMBOLS),
            Action(COMMA),
            Action(SWITCH_TO_EMOJI),
            Action(SPACE),
            Action(PERIOD),
            Action(SEARCH)
        )
    )

    val allLetterKeys: List<Letter> = rows.flatten().filterIsInstance<Letter>()
}
