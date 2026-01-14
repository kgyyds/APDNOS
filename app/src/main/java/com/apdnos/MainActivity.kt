package com.apdnos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.apdnos.ui.AppTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdosLogger.i("MainActivity onCreate")
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootLabScreen()
                }
            }
        }
    }
}

private data class AsmFile(val id: String, val name: String, val content: String)

@Composable
private fun RootLabScreen() {
    val defaultClang = "/data/user/0/aidepro.top/no_backup/ndksupport-1710240003/android-ndk-aide/toolchains/llvm/prebuilt/linux-aarch64/bin/clang"
    var clangPath by remember { mutableStateOf(defaultClang) }
    var rootStatus by remember { mutableStateOf("检测中...") }
    var outputStatus by remember { mutableStateOf("等待执行") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val asmFiles = remember { mutableStateListOf<AsmFile>() }
    var activeFileId by remember { mutableStateOf<String?>(null) }
    val activeFile = asmFiles.firstOrNull { it.id == activeFileId }

    LaunchedEffect(Unit) {
        val seedContent = readAssetText(context, "main.S")
        val initialFile = AsmFile(
            id = UUID.randomUUID().toString(),
            name = "main.S",
            content = seedContent
        )
        asmFiles.add(initialFile)
        activeFileId = initialFile.id
        rootStatus = checkRoot()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "汇编文件",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭侧栏")
                    }
                }
                Button(
                    onClick = {
                        val index = asmFiles.size + 1
                        val newFile = AsmFile(
                            id = UUID.randomUUID().toString(),
                            name = "asm_$index.S",
                            content = ""
                        )
                        asmFiles.add(newFile)
                        activeFileId = newFile.id
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "新建汇编文件")
                }
                if (asmFiles.isEmpty()) {
                    Text(
                        text = "暂无文件",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    asmFiles.forEach { file ->
                        val isActive = file.id == activeFileId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeFileId = file.id
                                    scope.launch { drawerState.close() }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (file.content.isBlank()) "空文件" else "包含 ${file.content.length} 字符",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                HackerTopBar(
                    title = activeFile?.name ?: "汇编实验室",
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onCompileClick = {
                        val file = activeFile
                        if (file == null) {
                            outputStatus = "请先创建汇编文件"
                        } else {
                            scope.launch {
                                outputStatus = compileAndRun(clangPath, file.content, file.name)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Root 检测与汇编执行",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "进入应用时会自动检查 Root 权限，并使用 su 编译执行 ARM64 汇编。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Root 状态: $rootStatus",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { scope.launch { rootStatus = checkRoot() } }) {
                        Text(text = "重新检查")
                    }
                }
                OutlinedTextField(
                    value = clangPath,
                    onValueChange = { clangPath = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("clang 路径") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                OutlinedTextField(
                    value = activeFile?.content ?: "",
                    onValueChange = { updated ->
                        val index = asmFiles.indexOfFirst { it.id == activeFileId }
                        if (index != -1) {
                            asmFiles[index] = asmFiles[index].copy(content = updated)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    label = { Text("ARM64 汇编源码") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    singleLine = false
                )
                Text(
                    text = outputStatus,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HackerTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onCompileClick: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "打开侧栏")
            }
        },
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
        },
        actions = {
            TextButton(onClick = onCompileClick) {
                Text(text = "编译并运行")
            }
        }
    )
}

private suspend fun checkRoot(): String = withContext(Dispatchers.IO) {
    AdosLogger.d("Running root check")
    val result = runCommand(listOf("su", "-c", "id"))
    if (result.exitCode == 0 && result.stdout.contains("uid=0")) {
        "已获取 root 权限"
    } else {
        "需要 root 权限"
    }
}

private suspend fun compileAndRun(clangPath: String, asmSource: String, fileName: String): String =
    withContext(Dispatchers.IO) {
        AdosLogger.d("Starting compileAndRun")
        val workDir = File("/data/local/tmp/APDNOS")
        try {
            if (!workDir.exists()) {
                workDir.mkdirs()
            }
            val safeBaseName = sanitizeFileBase(fileName)
            val sourceFile = File(workDir, "$safeBaseName.S")
            val outputFile = File(workDir, "$safeBaseName.out")
            sourceFile.writeText(asmSource)

            val compileCommand = buildString {
                append(clangPath)
                append(" -fPIE -pie -nostdlib -Wl,-e,_start ")
                append("-Wl,--dynamic-linker=/system/bin/linker64 ")
                append(sourceFile.absolutePath)
                append(" -o ")
                append(outputFile.absolutePath)
            }
            AdosLogger.d("Compile command: $compileCommand")
            val compileResult = runCommand(listOf("su", "-c", compileCommand))
            if (compileResult.exitCode != 0) {
                return@withContext if (isRootDenied(compileResult)) {
                    "需要 root 权限"
                } else {
                    formatResult("编译失败", compileResult)
                }
            }

            runCommand(listOf("su", "-c", "chmod 755 ${outputFile.absolutePath}"))
            val execResult = runCommand(listOf("su", "-c", outputFile.absolutePath))
            if (execResult.exitCode != 0 && isRootDenied(execResult)) {
                "需要 root 权限"
            } else {
                formatResult("执行完成", execResult)
            }
        } catch (ex: Exception) {
            AdosLogger.e("compileAndRun failed", ex)
            "执行失败: ${ex.message}"
        }
    }

private fun readAssetText(context: android.content.Context, name: String): String {
    return try {
        context.assets.open(name).bufferedReader().use { it.readText() }
    } catch (ex: Exception) {
        AdosLogger.w("Failed to read asset $name", ex)
        ""
    }
}

private data class CommandResult(val stdout: String, val stderr: String, val exitCode: Int)

private fun formatResult(prefix: String, result: CommandResult): String {
    return buildString {
        appendLine(prefix)
        appendLine("exitCode=${result.exitCode}")
        if (result.stdout.isNotBlank()) {
            appendLine("stdout:\n${result.stdout}")
        }
        if (result.stderr.isNotBlank()) {
            appendLine("stderr:\n${result.stderr}")
        }
    }
}

private fun sanitizeFileBase(fileName: String): String {
    val rawBase = fileName.substringBeforeLast('.', fileName)
    val sanitized = rawBase.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return sanitized.ifBlank { "main" }
}

private fun runCommand(command: List<String>): CommandResult {
    return try {
        AdosLogger.d("Run command: ${command.joinToString(" ")}")
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        CommandResult(stdout, stderr, exitCode)
    } catch (ex: Exception) {
        AdosLogger.e("Command failed", ex)
        CommandResult("", ex.message ?: "command failed", -1)
    }
}

private fun isRootDenied(result: CommandResult): Boolean {
    val combined = "${result.stdout}\n${result.stderr}"
    return combined.contains("Permission denied", ignoreCase = true) ||
        combined.contains("permission denied", ignoreCase = true) ||
        combined.contains("su: not found", ignoreCase = true) ||
        combined.contains("not permitted", ignoreCase = true)
}

private object AdosLogger {
    private const val TAG = "ADOS"

    fun d(message: String) = log("d", message, null)

    fun i(message: String) = log("i", message, null)

    fun w(message: String, throwable: Throwable? = null) = log("w", message, throwable)

    fun e(message: String, throwable: Throwable? = null) = log("e", message, throwable)

    private fun log(level: String, message: String, throwable: Throwable?) {
        try {
            val logClass = Class.forName("android.util.Log")
            val method = if (throwable == null) {
                logClass.getMethod(level, String::class.java, String::class.java)
            } else {
                logClass.getMethod(level, String::class.java, String::class.java, Throwable::class.java)
            }
            if (throwable == null) {
                method.invoke(null, TAG, message)
            } else {
                method.invoke(null, TAG, message, throwable)
            }
        } catch (_: Exception) {
            val suffix = if (throwable == null) "" else " - ${throwable.message}"
            println("$TAG [$level] $message$suffix")
        }
    }
}
