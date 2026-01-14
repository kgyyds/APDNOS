package com.apdnos.editor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CompletionState(
    val isVisible: Boolean = false,
    val items: List<CompletionItem> = emptyList(),
    val selectedIndex: Int = 0,
    val tokenRange: IntRange? = null
)

data class EditorSnapshot(
    val text: String,
    val selectionStart: Int
)

class EditorController(
    private val scope: CoroutineScope
) {
    private val editorState = MutableStateFlow(EditorSnapshot("", 0))
    private val _completionState = MutableStateFlow(CompletionState())
    private val _registerUsage = MutableStateFlow(RegisterScanner.emptyUsage())

    val completionState: StateFlow<CompletionState> = _completionState.asStateFlow()
    val registerUsage: StateFlow<RegisterUsage> = _registerUsage.asStateFlow()

    fun onEditorChange(text: String, selectionStart: Int) {
        editorState.value = EditorSnapshot(text, selectionStart)
    }

    fun hideCompletion() {
        _completionState.value = _completionState.value.copy(isVisible = false)
    }

    fun setSelectedIndex(index: Int) {
        val state = _completionState.value
        if (state.items.isEmpty()) return
        _completionState.value = state.copy(selectedIndex = index.coerceIn(0, state.items.lastIndex))
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun start() {
        scope.launch {
            editorState
                .debounce(120)
                .distinctUntilChanged()
                .mapLatest { snapshot ->
                    withContext(Dispatchers.Default) {
                        CompletionEngine.compute(snapshot.text, snapshot.selectionStart)
                    }
                }
                .collect { result ->
                    if (result == null || result.items.isEmpty()) {
                        _completionState.value = CompletionState()
                    } else {
                        _completionState.value = CompletionState(
                            isVisible = true,
                            items = result.items,
                            selectedIndex = 0,
                            tokenRange = result.tokenRange
                        )
                    }
                }
        }

        scope.launch {
            editorState
                .map { it.text }
                .debounce(200)
                .distinctUntilChanged()
                .mapLatest { text ->
                    withContext(Dispatchers.Default) {
                        RegisterScanner.scan(text)
                    }
                }
                .collect { usage ->
                    _registerUsage.value = usage
                }
        }
    }
}
