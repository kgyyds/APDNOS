package com.apdnos.editor

data class CompletionItem(
    val label: String,
    val insertText: String = label,
    val trailingSpace: Boolean = false,
    val priority: Int = 100
)

data class CompletionResult(
    val items: List<CompletionItem>,
    val tokenRange: IntRange
)

object CompletionEngine {
    private val instructions = listOf(
        "mov",
        "ldr",
        "str",
        "add",
        "sub",
        "bl",
        "b",
        "ret",
        "svc",
        "adr",
        "adrp",
        "stp",
        "ldp",
        "cbz",
        "ccmp",
        "cmp",
        "tst",
        "and",
        "orr",
        "eor",
        "lsl",
        "lsr",
        "asr",
        "nop"
    )

    private val directives = listOf(
        ".section",
        ".global",
        ".text",
        ".rodata",
        ".align",
        ".ascii",
        ".asciz",
        ".byte",
        ".word",
        ".quad",
        ".equ"
    )

    private val registers = buildList {
        (0..30).forEach { index -> add("x$index") }
        (0..30).forEach { index -> add("w$index") }
        add("sp")
        add("xzr")
        add("wzr")
        add("fp")
        add("lr")
    }

    private val snippets = listOf(
        "#0",
        "#1",
        "#64",
        "#93",
        "[sp, #imm]",
        "[xN, #imm]",
        "lsl #N"
    )

    private val completionItems = buildList {
        instructions.forEachIndexed { index, item ->
            add(CompletionItem(item, trailingSpace = true, priority = index))
        }
        directives.forEachIndexed { index, item ->
            add(CompletionItem(item, trailingSpace = true, priority = 50 + index))
        }
        registers.forEachIndexed { index, item ->
            add(CompletionItem(item, trailingSpace = false, priority = 100 + index))
        }
        snippets.forEachIndexed { index, item ->
            add(CompletionItem(item, trailingSpace = true, priority = 200 + index))
        }
    }

    fun compute(text: String, cursor: Int): CompletionResult? {
        if (cursor <= 0 || cursor > text.length) return null
        val previousChar = text.getOrNull(cursor - 1)
        if (previousChar != null && shouldHideOnChar(previousChar)) return null

        val range = findTokenRange(text, cursor) ?: return null
        val token = text.substring(range).trim()
        if (token.isEmpty()) return null

        val normalized = token.lowercase()
        val matches = completionItems.mapNotNull { item ->
            val label = item.label.lowercase()
            val score = when {
                label.startsWith(normalized) -> 2
                label.contains(normalized) -> 1
                else -> 0
            }
            if (score == 0) return@mapNotNull null
            Candidate(item, score)
        }.sortedWith(
            compareByDescending<Candidate> { it.score }
                .thenBy { it.item.priority }
                .thenBy { it.item.label }
        )
            .map { it.item }
            .take(10)

        if (matches.isEmpty()) return null
        return CompletionResult(matches, range)
    }

    fun detectInsertedChar(previous: String, current: String): Char? {
        if (current.length != previous.length + 1) return null
        var mismatchIndex = 0
        while (mismatchIndex < previous.length && previous[mismatchIndex] == current[mismatchIndex]) {
            mismatchIndex += 1
        }
        return current.getOrNull(mismatchIndex)
    }

    fun shouldHideOnChar(char: Char): Boolean {
        return char == ' ' || char == '\n' || char == '\t' || char == ','
    }

    private fun findTokenRange(text: String, cursor: Int): IntRange? {
        val start = findTokenStart(text, cursor - 1)
        val end = findTokenEnd(text, cursor)
        if (start > end) return null
        return start until end
    }

    private fun findTokenStart(text: String, index: Int): Int {
        var current = index
        while (current >= 0 && isTokenChar(text[current])) {
            current -= 1
        }
        return current + 1
    }

    private fun findTokenEnd(text: String, index: Int): Int {
        var current = index
        while (current < text.length && isTokenChar(text[current])) {
            current += 1
        }
        return current
    }

    private fun isTokenChar(char: Char): Boolean {
        return char.isLetterOrDigit() || char == '_' || char == '.'
    }

    private data class Candidate(val item: CompletionItem, val score: Int)
}
