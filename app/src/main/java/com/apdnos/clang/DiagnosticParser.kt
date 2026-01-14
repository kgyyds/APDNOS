package com.apdnos.clang

object DiagnosticParser {
    private val pattern = Regex(":(\\d+):(\\d+):\\s*(error|warning|note):\\s*(.*)")

    fun parse(stderr: String): List<Diagnostic> {
        return stderr.lineSequence().mapNotNull { line ->
            val match = pattern.find(line) ?: return@mapNotNull null
            val lineNumber = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val columnNumber = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val severity = when (match.groupValues[3].lowercase()) {
                "warning" -> DiagnosticSeverity.WARNING
                "note" -> DiagnosticSeverity.NOTE
                else -> DiagnosticSeverity.ERROR
            }
            val message = match.groupValues[4].trim()
            Diagnostic(line = lineNumber, column = columnNumber, severity = severity, message = message)
        }.toList()
    }
}
