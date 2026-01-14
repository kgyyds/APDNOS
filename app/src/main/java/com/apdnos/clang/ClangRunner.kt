package com.apdnos.clang

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID

data class ClangResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)

object ClangRunner {
    suspend fun runSyntaxCheck(
        context: Context,
        clangPath: File,
        asmSource: String,
        apiLevel: Int = 24,
        timeoutMs: Long = 6_000
    ): ClangResult = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "clang").apply { mkdirs() }
        val tempFile = File(tempDir, "diag-${UUID.randomUUID()}.S")
        tempFile.writeText(asmSource)
        var process: Process? = null
        try {
            withTimeout(timeoutMs) {
                val command = listOf(
                    clangPath.absolutePath,
                    "-x",
                    "assembler",
                    "-fsyntax-only",
                    "-fno-color-diagnostics",
                    "-target",
                    "aarch64-linux-android$apiLevel",
                    tempFile.absolutePath
                )
                process = ProcessBuilder(command)
                    .redirectErrorStream(false)
                    .start()
                val stdout = process?.inputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val stderr = process?.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val exitCode = process?.waitFor() ?: -1
                ClangResult(stdout, stderr, exitCode)
            }
        } catch (_: Exception) {
            process?.destroy()
            ClangResult("", "clang execution failed", -1)
        } finally {
            tempFile.delete()
        }
    }
}
