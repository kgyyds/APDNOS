package com.apdnos.editor

data class RegisterUsage(
    val x: List<String>,
    val w: List<String>,
    val special: List<String>
)

object RegisterScanner {
    private val registerRegex = Regex(
        "(?i)(?<![A-Za-z0-9_])(x([0-9]|[12][0-9]|30)|w([0-9]|[12][0-9]|30)|sp|xzr|wzr|fp|lr)(?![A-Za-z0-9_])"
    )

    fun scan(text: String): RegisterUsage {
        val cleaned = text.lineSequence()
            .map { stripComment(it) }
            .joinToString("\n")
        val xRegisters = mutableSetOf<Int>()
        val wRegisters = mutableSetOf<Int>()
        val specials = mutableSetOf<String>()

        registerRegex.findAll(cleaned).forEach { match ->
            val token = match.value.lowercase()
            when {
                token.startsWith("x") && token.length > 1 && token != "xzr" -> {
                    token.drop(1).toIntOrNull()?.let { xRegisters.add(it) }
                }
                token.startsWith("w") && token.length > 1 && token != "wzr" -> {
                    token.drop(1).toIntOrNull()?.let { wRegisters.add(it) }
                }
                token == "sp" || token == "xzr" || token == "wzr" || token == "fp" || token == "lr" -> {
                    specials.add(token)
                }
            }
        }

        val xSorted = xRegisters.toList().sorted().map { "x$it" }
        val wSorted = wRegisters.toList().sorted().map { "w$it" }
        val specialOrder = listOf("sp", "fp", "lr", "xzr", "wzr")
        val specialSorted = specialOrder.filter { specials.contains(it) }

        return RegisterUsage(x = xSorted, w = wSorted, special = specialSorted)
    }

    fun emptyUsage(): RegisterUsage = RegisterUsage(emptyList(), emptyList(), emptyList())

    private fun stripComment(line: String): String {
        val hashIndex = line.indexOf('#').takeIf { it >= 0 } ?: Int.MAX_VALUE
        val slashIndex = line.indexOf("//").takeIf { it >= 0 } ?: Int.MAX_VALUE
        val cutIndex = minOf(hashIndex, slashIndex)
        return if (cutIndex == Int.MAX_VALUE) line else line.substring(0, cutIndex)
    }
}
