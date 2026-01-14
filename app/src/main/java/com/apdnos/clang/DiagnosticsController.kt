package com.apdnos.clang

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DiagnosticsState(
    val diagnostics: List<Diagnostic> = emptyList(),
    val isRunning: Boolean = false
)

class DiagnosticsController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val textState = MutableStateFlow("")
    private val _state = MutableStateFlow(DiagnosticsState())

    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()

    fun onEditorChange(text: String) {
        textState.value = text
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun start(clangPathProvider: () -> File?) {
        scope.launch {
            textState
                .debounce(200)
                .distinctUntilChanged()
                .mapLatest { text ->
                    val clangPath = clangPathProvider()
                    if (clangPath == null || !clangPath.exists()) {
                        DiagnosticsState(emptyList(), isRunning = false)
                    } else {
                        _state.value = DiagnosticsState(_state.value.diagnostics, isRunning = true)
                        val result = withContext(Dispatchers.IO) {
                            ClangRunner.runSyntaxCheck(context, clangPath, text)
                        }
                        val diagnostics = DiagnosticParser.parse(result.stderr)
                        DiagnosticsState(diagnostics, isRunning = false)
                    }
                }
                .collect { newState ->
                    _state.value = newState
                }
        }
    }
}
