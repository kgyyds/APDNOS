package com.apdnos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apdnos.ui.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdosLogger.i("MainActivity onCreate")
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
            }
        }
    }
}

private sealed class Screen(val route: String, val title: String, val description: String) {
    data object Home : Screen("home", "APDNOS", "Hacker-style control hub")
    data object RootLab : Screen("root_lab", "Root + ASM", "Root check, compile, and execute ELF")
    data object Status : Screen("status", "System Status", "Quick overview and tips")
    data object About : Screen("about", "About", "Project mission briefing")
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.RootLab.route) {
            RootLabScreen(navController)
        }
        composable(Screen.Status.route) {
            InfoScreen(navController, Screen.Status)
        }
        composable(Screen.About.route) {
            InfoScreen(navController, Screen.About)
        }
    }
}

@Composable
private fun HomeScreen(navController: NavHostController) {
    val cards = listOf(Screen.RootLab, Screen.Status, Screen.About)
    Scaffold(
        topBar = {
            HackerTopBar(title = "APDNOS")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Hacker Console",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Navigate the ops deck and launch root tasks.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            cards.forEach { screen ->
                HackerCard(
                    title = screen.title,
                    description = screen.description,
                    onClick = { navController.navigate(screen.route) }
                )
            }
        }
    }
}

@Composable
private fun HackerCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InfoScreen(navController: NavHostController, screen: Screen) {
    Scaffold(
        topBar = { HackerTopBar(title = screen.title) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = screen.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = screen.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(onClick = { navController.popBackStack() }) {
                Text(text = "返回")
            }
        }
    }
}

@Composable
private fun RootLabScreen(navController: NavHostController) {
    val defaultClang = "/data/user/0/aidepro.top/no_backup/ndksupport-1710240003/android-ndk-aide/toolchains/llvm/prebuilt/linux-aarch64/bin/clang"
    var clangPath by remember { mutableStateOf(defaultClang) }
    var asmSource by remember { mutableStateOf("") }
    var rootStatus by remember { mutableStateOf("尚未检查") }
    var outputStatus by remember { mutableStateOf("等待执行") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        asmSource = readAssetText(context, "main.S")
    }

    Scaffold(
        topBar = { HackerTopBar(title = "Root + ASM") }
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
                text = "使用 su 权限编译 ARM64 汇编并执行 ELF。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(onClick = {
                scope.launch {
                    rootStatus = checkRoot()
                }
            }) {
                Text(text = "检查 Root")
            }
            Text(
                text = "Root 状态: $rootStatus",
                color = MaterialTheme.colorScheme.onBackground
            )
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
                value = asmSource,
                onValueChange = { asmSource = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                label = { Text("ARM64 汇编源码") },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                singleLine = false
            )
            Button(onClick = {
                scope.launch {
                    outputStatus = compileAndRun(clangPath, asmSource)
                }
            }) {
                Text(text = "编译并执行")
            }
            Text(
                text = outputStatus,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.Monospace
            )
            Button(onClick = { navController.popBackStack() }) {
                Text(text = "返回")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HackerTopBar(title: String) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
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

private suspend fun compileAndRun(clangPath: String, asmSource: String): String =
    withContext(Dispatchers.IO) {
        AdosLogger.d("Starting compileAndRun")
        val workDir = File("/data/local/tmp/APDNOS")
        try {
            if (!workDir.exists()) {
                workDir.mkdirs()
            }
            val sourceFile = File(workDir, "main.S")
            val outputFile = File(workDir, "main.out")
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
