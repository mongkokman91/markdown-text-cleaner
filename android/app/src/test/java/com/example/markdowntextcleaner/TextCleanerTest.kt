package com.example.markdowntextcleaner

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCleanerTest {
    @Test fun flattensWhitespace() {
        assertEquals("one two three four five", cleanMarkdownText(" \t one\r\n two\u00a0three\u2028four\u0085five  "))
    }

    @Test fun keepsMarkdownAndUnicodeContent() {
        val text = "**Title** [link](https://example.com) 中文 👩‍💻"
        assertEquals(text, cleanMarkdownText(text))
        assertEquals("", cleanMarkdownText(" \u00a0\r\n"))
    }

    @Test fun isIdempotent() {
        val clean = cleanMarkdownText("  one\t  two\nthree")
        assertEquals(clean, cleanMarkdownText(clean))
    }
}
