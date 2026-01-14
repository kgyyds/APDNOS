package com.apdnos

import android.os.Bundle
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apdnos.ui.AppTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.util.UUID
import com.apdnos.clang.ClangInstaller
import com.apdnos.clang.DiagnosticsController
import com.apdnos.clang.Diagnostic
import com.apdnos.clang.DiagnosticSeverity
import com.apdnos.editor.CompletionEngine
import com.apdnos.editor.CompletionItem
import com.apdnos.editor.CompletionPopup
import com.apdnos.editor.RegisterBar
import com.apdnos.editor.EditorController

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

@Composable
private fun RootLabScreen() {
    val context = LocalContext.current
    val defaultClang = remember(context) { File(context.filesDir, "clang/bin/clang").absolutePath }
    var clangPath by remember { mutableStateOf(defaultClang) }
    var rootStatus by remember { mutableStateOf("检测中...") }
    var outputStatus by remember { mutableStateOf("等待执行") }
    var showSettings by remember { mutableStateOf(false) }
    var isSidebarOpen by remember { mutableStateOf(true) }
    var isConsoleExpanded by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val asmFiles = remember { mutableStateListOf<String>() }
    var activeFileName by remember { mutableStateOf<String?>(null) }
    var asmSource by remember { mutableStateOf(TextFieldValue("")) }
    val asmDirectory = remember { File(context.filesDir, "asm") }
    val editorScrollState = rememberScrollState()
    val consoleScrollState = rememberScrollState()
    var editorSize by remember { mutableStateOf(IntSize.Zero) }
    var completionOffset by remember { mutableStateOf(IntOffset.Zero) }
    var completionMaxSize by remember { mutableStateOf(IntSize.Zero) }
    val editorController = remember { EditorController(scope) }
    val diagnosticsController = remember { DiagnosticsController(context, scope) }
    val diagnosticsState by diagnosticsController.state.collectAsState()
    val completionState by editorController.completionState.collectAsState()
    val registerUsage by editorController.registerUsage.collectAsState()
    val completionVisible = completionState.isVisible && completionState.items.isNotEmpty()
    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarOpen) 260.dp else 0.dp,
        label = "sidebarWidth"
    )
    val consoleHeight by animateDpAsState(
        targetValue = if (isConsoleExpanded) 220.dp else 56.dp,
        label = "consoleHeight"
    )
    val lineNumbers by remember(asmSource.text) {
        derivedStateOf {
            val lineCount = asmSource.text.lineSequence().count().coerceAtLeast(1)
            (1..lineCount).joinToString("\n") { it.toString() }
        }
    }
    val density = LocalDensity.current
    val fontSize = MaterialTheme.typography.bodySmall.fontSize
    val lineHeight = MaterialTheme.typography.bodySmall.lineHeight.takeIf { it != androidx.compose.ui.unit.TextUnit.Unspecified }
        ?: (fontSize * 1.4f)
    val charWidthPx = with(density) { fontSize.toPx() * 0.6f }
    val lineHeightPx = with(density) { lineHeight.toPx() }
    val lineNumberWidthPx = with(density) { 48.dp.toPx() }
    val lineNumberPaddingPx = with(density) { 8.dp.toPx() }
    val editorPaddingPx = with(density) { 8.dp.toPx() }
    val popupGapPx = with(density) { 8.dp.toPx() }

    LaunchedEffect(Unit) {
        editorController.start()
        diagnosticsController.start { File(clangPath).takeIf { it.exists() } }
        if (!asmDirectory.exists()) {
            asmDirectory.mkdirs()
        }
        val existingFiles = asmDirectory.listFiles { file ->
            file.isFile && file.extension.equals("S", ignoreCase = true)
        }.orEmpty().sortedBy { it.name }
        if (existingFiles.isEmpty()) {
            val seedContent = readAssetText(context, "main.S")
            val initialFile = File(asmDirectory, "main.S")
            initialFile.writeText(seedContent)
            asmFiles.add(initialFile.name)
            activeFileName = initialFile.name
            asmSource = TextFieldValue(seedContent, selection = TextRange(seedContent.length))
        } else {
            asmFiles.addAll(existingFiles.map { it.name })
            activeFileName = existingFiles.first().name
            val content = existingFiles.first().readText()
            asmSource = TextFieldValue(content, selection = TextRange(content.length))
        }
        clangPath = ClangInstaller.installIfNeeded(context).absolutePath
        editorController.onEditorChange(asmSource.text, asmSource.selection.start)
        diagnosticsController.onEditorChange(asmSource.text)
        rootStatus = checkRoot()
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            val cursor = asmSource.selection.start.coerceIn(0, asmSource.text.length)
            val textBeforeCursor = asmSource.text.take(cursor)
            val lineIndex = textBeforeCursor.count { it == '\n' }
            val columnIndex = cursor - (textBeforeCursor.lastIndexOf('\n') + 1).coerceAtLeast(0)
            Triple(lineIndex, columnIndex, editorScrollState.value)
        }
            .distinctUntilChanged()
            .collect { (lineIndex, columnIndex, scrollY) ->
                val caretX = editorPaddingPx + lineNumberWidthPx + lineNumberPaddingPx + (columnIndex * charWidthPx)
                val caretY = editorPaddingPx + (lineIndex * lineHeightPx) - scrollY
                val popupWidthPx = completionMaxSize.width.toFloat().coerceAtLeast(160f)
                val popupHeightPx = completionMaxSize.height.toFloat()
                val shouldShowAbove = caretY + lineHeightPx + popupGapPx + popupHeightPx > editorSize.height
                val rawPopupY = if (shouldShowAbove) {
                    caretY - popupHeightPx - popupGapPx
                } else {
                    caretY + lineHeightPx + popupGapPx
                }
                val popupX = caretX.coerceIn(0f, editorSize.width - popupWidthPx)
                val popupY = rawPopupY.coerceIn(0f, editorSize.height - popupHeightPx)
                completionOffset = IntOffset(popupX.toInt(), popupY.toInt())
            }
    }

    BackHandler(enabled = completionVisible) {
        editorController.hideCompletion()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HackerTopBar(
            title = activeFileName ?: "汇编实验室",
            onCompileClick = {
                val fileName = activeFileName
                if (fileName == null) {
                    outputStatus = "请先创建汇编文件"
                } else {
                    scope.launch {
                        outputStatus = compileAndRun(
                            context = context,
                            clangPath = clangPath,
                            asmSource = asmSource.text,
                            fileName = fileName
                        )
                        isConsoleExpanded = true
                    }
                }
            },
            onMenuClick = { isSidebarOpen = !isSidebarOpen },
            onSettingsClick = { showSettings = true }
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(visible = isSidebarOpen) {
                Column(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
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
                        IconButton(onClick = { isSidebarOpen = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "关闭侧栏")
                        }
                    }
                    Button(
                        onClick = {
                            val index = asmFiles.size + 1
                            val newFileName = "asm_$index.S"
                            val newFile = File(asmDirectory, newFileName)
                            newFile.writeText("")
                            asmFiles.add(newFileName)
                            activeFileName = newFileName
                            asmSource = TextFieldValue("")
                            editorController.onEditorChange("", 0)
                            diagnosticsController.onEditorChange("")
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
                        asmFiles.forEach { fileName ->
                            val isActive = fileName == activeFileName
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
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
                                        text = fileName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    TextButton(
                                        onClick = {
                                            activeFileName = fileName
                                            val content = File(asmDirectory, fileName).readText()
                                            asmSource = TextFieldValue(content, selection = TextRange(content.length))
                                            editorController.onEditorChange(content, content.length)
                                            diagnosticsController.onEditorChange(content)
                                        }
                                    ) {
                                        Text(text = if (isActive) "当前文件" else "切换到此文件")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                val lineNumberWidth = 48.dp
                val lineNumberPadding = 8.dp
                val editorPadding = 8.dp
                val maxPopupWidthPx = with(density) { 320.dp.toPx() }
                val maxPopupHeightPx = with(density) { 240.dp.toPx() }
                val rowHeightPx = with(density) { 40.dp.toPx() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(editorPadding)
                        .onSizeChanged { editorSize = it }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(editorScrollState)
                    ) {
                        Text(
                            text = lineNumbers,
                            modifier = Modifier
                                .width(lineNumberWidth)
                                .padding(end = lineNumberPadding),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.End
                        )
                        OutlinedTextField(
                            value = asmSource,
                            onValueChange = { updated ->
                                val previousText = asmSource.text
                                asmSource = updated
                                val fileName = activeFileName ?: return@OutlinedTextField
                                scope.launch(Dispatchers.IO) {
                                    File(asmDirectory, fileName).writeText(updated.text)
                                }
                                val insertedChar = CompletionEngine.detectInsertedChar(previousText, updated.text)
                                if (insertedChar != null && CompletionEngine.shouldHideOnChar(insertedChar)) {
                                    editorController.hideCompletion()
                                }
                                editorController.onEditorChange(updated.text, updated.selection.start)
                                diagnosticsController.onEditorChange(updated.text)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent { event ->
                                    val items = completionState.items
                                    if (!completionVisible || items.isEmpty()) {
                                        return@onPreviewKeyEvent false
                                    }
                                    if (event.nativeKeyEvent.action != AndroidKeyEvent.ACTION_DOWN) {
                                        return@onPreviewKeyEvent false
                                    }
                                    when (event.nativeKeyEvent.keyCode) {
                                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                            editorController.setSelectedIndex(
                                                (completionState.selectedIndex + 1) % items.size
                                            )
                                            true
                                        }
                                        AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                            editorController.setSelectedIndex(
                                                (completionState.selectedIndex - 1 + items.size) % items.size
                                            )
                                            true
                                        }
                                        AndroidKeyEvent.KEYCODE_ENTER,
                                        AndroidKeyEvent.KEYCODE_TAB -> {
                                            applyCompletion(
                                                asmSource,
                                                items[completionState.selectedIndex],
                                                completionState.tokenRange
                                            ) { newValue ->
                                                asmSource = newValue
                                                editorController.onEditorChange(newValue.text, newValue.selection.start)
                                            }
                                            editorController.hideCompletion()
                                            true
                                        }
                                        AndroidKeyEvent.KEYCODE_ESCAPE,
                                        AndroidKeyEvent.KEYCODE_BACK -> {
                                            editorController.hideCompletion()
                                            true
                                        }
                                        else -> false
                                    }
                                },
                            label = { Text("ARM64 汇编源码") },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            singleLine = false,
                            maxLines = Int.MAX_VALUE
                        )
                    }
                    if (completionVisible) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(completionOffset, completionMaxSize) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type != PointerEventType.Release) continue
                                            val tapOffset = event.changes.firstOrNull()?.position ?: continue
                                            val withinX = tapOffset.x >= completionOffset.x &&
                                                tapOffset.x <= (completionOffset.x + completionMaxSize.width)
                                            val withinY = tapOffset.y >= completionOffset.y &&
                                                tapOffset.y <= (completionOffset.y + completionMaxSize.height)
                                            if (!withinX || !withinY) {
                                                editorController.hideCompletion()
                                            }
                                        }
                                    }
                                }
                        )
                    }
                    if (completionVisible && completionState.tokenRange != null) {
                        val items = completionState.items
                        val popupWidthPx = minOf(editorSize.width * 0.7f, maxPopupWidthPx)
                        val estimatedPopupHeightPx =
                            minOf(maxPopupHeightPx, rowHeightPx * items.size.coerceAtMost(10))
                        val popupHeightDp = with(density) { estimatedPopupHeightPx.toDp() }
                        val popupWidthDp = with(density) { popupWidthPx.toDp() }
                        val maxSize = IntSize(popupWidthPx.toInt(), estimatedPopupHeightPx.toInt())
                        if (completionMaxSize != maxSize) {
                            completionMaxSize = maxSize
                        }
                        CompletionPopup(
                            items = items,
                            selectedIndex = completionState.selectedIndex,
                            maxWidth = popupWidthDp,
                            maxHeight = popupHeightDp,
                            offset = completionOffset,
                            onSelect = { item ->
                                applyCompletion(asmSource, item, completionState.tokenRange) { newValue ->
                                    asmSource = newValue
                                    editorController.onEditorChange(newValue.text, newValue.selection.start)
                                }
                                editorController.hideCompletion()
                            }
                        )
                    }
                }
                DiagnosticsPanel(
                    diagnostics = diagnosticsState.diagnostics,
                    isRunning = diagnosticsState.isRunning,
                    onSelect = { diagnostic ->
                        val offset = findOffsetForLine(asmSource.text, diagnostic.line, diagnostic.column)
                        asmSource = asmSource.copy(selection = TextRange(offset))
                        editorController.onEditorChange(asmSource.text, offset)
                    }
                )
            }
        }
        RegisterBar(
            usage = registerUsage
        )
        ConsoleSheet(
            outputStatus = outputStatus,
            isExpanded = isConsoleExpanded,
            onToggle = { isConsoleExpanded = !isConsoleExpanded },
            scrollState = consoleScrollState,
            height = consoleHeight
        )
    }

    if (showSettings) {
        SettingsDialog(
            clangPath = clangPath,
            onClangPathChange = { clangPath = it },
            rootStatus = rootStatus,
            onRecheckRoot = { scope.launch { rootStatus = checkRoot() } },
            onDismiss = { showSettings = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HackerTopBar(
    title: String,
    onCompileClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit
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
            IconButton(onClick = onSettingsClick) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "设置")
            }
        }
    )
}

@Composable
private fun SettingsDialog(
    clangPath: String,
    onClangPathChange: (String) -> Unit,
    rootStatus: String,
    onRecheckRoot: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "完成")
            }
        },
        title = {
            Text(text = "设置")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Root 状态: $rootStatus")
                    TextButton(onClick = onRecheckRoot) {
                        Text(text = "重新检查")
                    }
                }
                OutlinedTextField(
                    value = clangPath,
                    onValueChange = onClangPathChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("clang 路径") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        }
    )
}

@Composable
private fun ConsoleSheet(
    outputStatus: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    height: androidx.compose.ui.unit.Dp
) {
    LaunchedEffect(outputStatus) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "输出日志",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            IconButton(onClick = onToggle) {
                val icon = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp
                val desc = if (isExpanded) "收起控制台" else "展开控制台"
                Icon(imageVector = icon, contentDescription = desc)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = outputStatus,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsPanel(
    diagnostics: List<Diagnostic>,
    isRunning: Boolean,
    onSelect: (Diagnostic) -> Unit
) {
    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "诊断",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            if (isRunning) {
                Text(
                    text = "分析中…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (diagnostics.isEmpty()) {
            Text(
                text = "无诊断信息",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(diagnostics.take(6)) { diag ->
                    val color = when (diag.severity) {
                        DiagnosticSeverity.ERROR -> MaterialTheme.colorScheme.error
                        DiagnosticSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
                        DiagnosticSeverity.NOTE -> MaterialTheme.colorScheme.primary
                    }
                    Text(
                        text = "${diag.line}:${diag.column} ${diag.message}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(diag) },
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun findOffsetForLine(text: String, line: Int, column: Int): Int {
    if (line <= 0) return 0
    var currentLine = 1
    var index = 0
    while (index < text.length && currentLine < line) {
        if (text[index] == '\n') {
            currentLine += 1
        }
        index += 1
    }
    val targetIndex = (index + column - 1).coerceIn(0, text.length)
    return targetIndex
}

private fun applyCompletion(
    current: TextFieldValue,
    item: CompletionItem,
    tokenRange: IntRange?,
    onUpdate: (TextFieldValue) -> Unit
) {
    if (tokenRange == null) return
    val start = tokenRange.first.coerceAtLeast(0)
    val end = (tokenRange.last + 1).coerceAtMost(current.text.length)
    val insertion = buildString {
        append(item.insertText)
        if (item.trailingSpace) {
            append(' ')
        }
    }
    val newText = current.text.replaceRange(start, end, insertion)
    val cursor = (start + insertion.length).coerceIn(0, newText.length)
    onUpdate(
        current.copy(
            text = newText,
            selection = TextRange(cursor)
        )
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

private suspend fun compileAndRun(
    context: android.content.Context,
    clangPath: String,
    asmSource: String,
    fileName: String
): String =
    withContext(Dispatchers.IO) {
        AdosLogger.d("Starting compileAndRun")
        val privateDir = File(context.filesDir, "asm")
        val workDirPath = "/data/local/tmp/APDNOS"
        val workDir = File(workDirPath)
        try {
            if (!privateDir.exists()) {
                privateDir.mkdirs()
            }
            val ensureWorkDir = runCommand(listOf("su", "-c", "mkdir -p $workDirPath"))
            if (ensureWorkDir.exitCode != 0) {
                return@withContext if (isRootDenied(ensureWorkDir)) {
                    "需要 root 权限"
                } else {
                    formatResult("创建工作目录失败", ensureWorkDir)
                }
            }
            val safeBaseName = sanitizeFileBase(fileName)
            val sourceFile = File(privateDir, "$safeBaseName.S")
            val outputFile = File(workDir, "$safeBaseName.out")
            sourceFile.writeText(asmSource)

            val escapedClang = shellEscape(clangPath)
            val escapedSource = shellEscape(sourceFile.absolutePath)
            val escapedOutput = shellEscape(outputFile.absolutePath)
            val compileCommand = buildString {
                append(escapedClang)
                append(" -fPIE -pie -nostdlib -Wl,-e,_start ")
                append("-Wl,--dynamic-linker=/system/bin/linker64 ")
                append(escapedSource)
                append(" -o ")
                append(escapedOutput)
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

            runCommand(listOf("su", "-c", "chmod 755 $escapedOutput"))
            val execResult = runCommand(listOf("su", "-c", escapedOutput))
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

private fun shellEscape(value: String): String {
    return "'" + value.replace("'", "'\\''") + "'"
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
