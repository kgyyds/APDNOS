package com.apdnos.clang

import android.content.Context
import java.io.File

object ClangInstaller {
    private const val ASSET_ROOT = "clang"
    private const val VERSION_FILE = "llvm.version"

    fun clangPath(context: Context): File {
        return File(context.filesDir, "clang/bin/clang")
    }

    fun installIfNeeded(context: Context): File {
        val installDir = File(context.filesDir, "clang")
        val assetVersion = readAssetText(context, "$ASSET_ROOT/$VERSION_FILE")
        val versionFile = File(installDir, VERSION_FILE)
        val installedVersion = if (versionFile.exists()) versionFile.readText() else ""

        if (installDir.exists() && assetVersion.isNotBlank() && assetVersion == installedVersion) {
            return clangPath(context)
        }

        if (installDir.exists()) {
            installDir.deleteRecursively()
        }
        installDir.mkdirs()

        copyAssetDir(context, ASSET_ROOT, installDir)
        versionFile.writeText(assetVersion)
        val clangBin = clangPath(context)
        if (clangBin.exists()) {
            clangBin.setExecutable(true, true)
        }
        return clangBin
    }

    private fun copyAssetDir(context: Context, assetPath: String, targetDir: File) {
        val assets = context.assets
        val entries = assets.list(assetPath).orEmpty()
        if (entries.isEmpty()) {
            assets.open(assetPath).use { input ->
                val outFile = File(targetDir, assetPath.substringAfterLast('/'))
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }

        entries.forEach { entry ->
            val childAssetPath = "$assetPath/$entry"
            val childTarget = File(targetDir, entry)
            val childEntries = assets.list(childAssetPath).orEmpty()
            if (childEntries.isEmpty()) {
                assets.open(childAssetPath).use { input ->
                    childTarget.parentFile?.mkdirs()
                    childTarget.outputStream().use { output -> input.copyTo(output) }
                }
            } else {
                childTarget.mkdirs()
                copyAssetDir(context, childAssetPath, childTarget)
            }
        }
    }

    private fun readAssetText(context: Context, name: String): String {
        return try {
            context.assets.open(name).bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            ""
        }
    }
}
