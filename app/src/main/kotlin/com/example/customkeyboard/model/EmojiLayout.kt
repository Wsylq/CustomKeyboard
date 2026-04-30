package com.example.customkeyboard.model

import com.example.customkeyboard.model.Key.Action
import com.example.customkeyboard.model.Key.CategoryIcon
import com.example.customkeyboard.model.Key.Emoji
import com.example.customkeyboard.model.ActionType.*

object EmojiLayout {

    /** Category bar — Unicode symbols + backspace at end. */
    val categoryBar: List<Key> = EmojiCategory.entries.map { CategoryIcon(it) } +
            listOf(Action(BACKSPACE))

    /** All emojis grouped by category. */
    val emojisByCategory: Map<EmojiCategory, List<String>> = mapOf(
        EmojiCategory.RECENT to listOf(
            "😭", "😂", "💔", "✌️", "😋", "💀",
            "🙏", "👍", "😔", "😈", "😍", "😘",
            "🤫", "🤩", "😏", "😎", "😁", "😐",
            "😊", "😡", "🧨", "😕", "🌍", "🔥"
        ),
        EmojiCategory.SMILEYS to listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
            "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
            "😘", "😗", "😚", "😙", "🥲", "😋", "😛", "😜",
            "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐",
            "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "🤥",
            "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕",
            "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯",
            "🤠", "🥳", "🥸", "😎", "🤓", "🧐", "😕", "😟",
            "🙁", "😮", "😯", "😲", "😳", "🥺", "😦", "😧",
            "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣",
            "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠",
            "🤬", "😈", "👿", "💀", "☠️", "💩", "🤡", "👹"
        ),
        EmojiCategory.NATURE to listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
            "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🙈",
            "🌸", "🌺", "🌻", "🌹", "🌷", "🌿", "🍀", "🌱",
            "🌲", "🌳", "🌴", "🌵", "🎋", "🎍", "🍁", "🍂",
            "🌊", "🌈", "⛅", "🌤", "🌦", "🌧", "⛈", "🌩",
            "❄️", "☃️", "⛄", "🌬", "💨", "🌪", "🌫", "🌊"
        ),
        EmojiCategory.PEOPLE to listOf(
            "👋", "🤚", "🖐", "✋", "🖖", "👌", "🤌", "🤏",
            "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆",
            "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛",
            "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️",
            "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂",
            "🦻", "👃", "🫀", "🫁", "🧠", "🦷", "🦴", "👀"
        ),
        EmojiCategory.TRAVEL to listOf(
            "🚗", "🚕", "🚙", "🚌", "🚎", "🏎", "🚓", "🚑",
            "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🏍", "🛵",
            "✈️", "🛫", "🛬", "🛩", "💺", "🚀", "🛸", "🚁",
            "🛶", "⛵", "🚤", "🛥", "🛳", "⛴", "🚢", "⚓",
            "🗺", "🧭", "🏔", "⛰", "🌋", "🗻", "🏕", "🏖",
            "🏜", "🏝", "🏞", "🏟", "🏛", "🏗", "🧱", "🏘"
        ),
        EmojiCategory.ACTIVITIES to listOf(
            "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉",
            "🥏", "🎱", "🏓", "🏸", "🏒", "🏑", "🥍", "🏏",
            "🎿", "🛷", "🥌", "🎯", "🪃", "🏹", "🎣", "🤿",
            "🥊", "🥋", "🎽", "🛹", "🛼", "🛺", "🏋️", "🤼",
            "🤸", "🤺", "🏇", "⛷", "🏂", "🪂", "🏄", "🚣",
            "🧗", "🚵", "🚴", "🏆", "🥇", "🥈", "🥉", "🏅"
        ),
        EmojiCategory.OBJECTS to listOf(
            "💡", "🔦", "🕯", "🪔", "🧯", "🛢", "💰", "💵",
            "💴", "💶", "💷", "💸", "💳", "🪙", "💎", "⚖️",
            "📱", "💻", "⌨️", "🖥", "🖨", "🖱", "🖲", "💾",
            "📷", "📸", "📹", "🎥", "📽", "🎞", "📞", "☎️",
            "📟", "📠", "📺", "📻", "🧭", "⏱", "⏰", "🕰",
            "⌚", "📡", "🔋", "🔌", "💡", "🔦", "🕯", "🪔"
        ),
        EmojiCategory.SYMBOLS to listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
            "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖",
            "💘", "💝", "💟", "☮️", "✝️", "☪️", "🕉", "☸️",
            "✡️", "🔯", "🕎", "☯️", "☦️", "🛐", "⛎", "♈",
            "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐",
            "✅", "❌", "❗", "❓", "💯", "🔥", "⭐", "✨"
        ),
        EmojiCategory.FLAGS to listOf(
            "🏳️", "🏴", "🚩", "🏁", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️",
            "🇺🇸", "🇬🇧", "🇨🇦", "🇦🇺", "🇮🇳", "🇩🇪", "🇫🇷",
            "🇯🇵", "🇨🇳", "🇧🇷", "🇲🇽", "🇰🇷", "🇷🇺", "🇮🇹",
            "🇪🇸", "🇵🇹", "🇳🇱", "🇸🇦", "🇦🇪", "🇹🇷", "🇵🇰",
            "🇿🇦", "🇳🇬", "🇪🇬", "🇦🇷", "🇨🇱", "🇨🇴", "🇵🇪"
        ),
        EmojiCategory.EMOTICONS to listOf(
            ":-)", ":-(", ";-)", ":-D", ":-P", ":-O", ":-|", ":-/",
            "B-)", ":-*", ":'(", ">:-)", ":-$", "O:-)", ":-X", ":-#",
            "(^_^)", "(>_<)", "(T_T)", "(*_*)", "(o_o)", "(-_-)", "(^o^)", "(._.)","(¬_¬)", "ʕ•ᴥ•ʔ", "(ง'̀-'́)ง", "¯\\_(ツ)_/¯"
        )
    )

    /** Get emoji rows for a given category, 6 per row. */
    fun rowsForCategory(category: EmojiCategory): List<List<Key>> {
        val emojis = emojisByCategory[category] ?: emptyList()
        return emojis.chunked(6).map { chunk -> chunk.map { Emoji(it) } }
    }

    /** Bottom action row: ABC [SPACE] ABC */
    val bottomRow: List<Key> = listOf(
        Action(SWITCH_FROM_EMOJI),
        Action(SPACE),
        Action(SWITCH_FROM_EMOJI)
    )
}
