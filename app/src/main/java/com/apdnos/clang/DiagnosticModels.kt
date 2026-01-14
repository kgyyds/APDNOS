package com.apdnos.clang

enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    NOTE
}

data class Diagnostic(
    val line: Int,
    val column: Int,
    val severity: DiagnosticSeverity,
    val message: String
)
