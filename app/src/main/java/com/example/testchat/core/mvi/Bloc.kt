package com.example.testchat.core.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface Bloc<I : Any, S : Any> {
    val state: StateFlow<S>
    fun dispatch(intent: I)
    fun clear()
}

abstract class MviBloc<I : Any, S : Any>(
    initialState: S,
    protected val scope: CoroutineScope
) : Bloc<I, S> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<S> = _state.asStateFlow()

    override fun dispatch(intent: I) {
        scope.launch { handleIntent(intent) }
    }

    protected fun setState(reducer: (S) -> S) {
        _state.value = reducer(_state.value)
    }

    protected suspend fun emitState(newState: S) {
        _state.emit(newState)
    }

    protected abstract suspend fun handleIntent(intent: I)

    override fun clear() {
        scope.cancel()
    }
}
