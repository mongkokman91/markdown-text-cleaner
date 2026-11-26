package com.example.markdowntextcleaner

/** Flattens whitespace without changing punctuation, Markdown markers, or emoji. */
internal fun cleanMarkdownText(text: String): String = buildString(text.length) {
    var pendingSpace = false
    for (character in text) {
        if (character.isWhitespace() || character == '\u0085') {
            pendingSpace = isNotEmpty()
        } else {
            if (pendingSpace) append(' ')
            append(character)
            pendingSpace = false
        }
    }
}
