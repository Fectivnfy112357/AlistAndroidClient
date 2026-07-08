package com.textvision.alistclient.auth

import com.textvision.alistclient.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Consumes [SessionEvent]s from [SessionEventBus] and turns them into app-level reactions:
 * on [SessionEvent.Unauthorized] it clears the session and signals a navigation back to Login;
 * on [SessionEvent.NetworkDown] it flips [isNetworkDown].
 */
@Singleton
class SessionGate @Inject constructor(
    private val sessionEventBus: SessionEventBus,
    private val authRepository: AuthRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    // signal-only: consumers always navigate to LoginDest, no routing payload needed.
    private val _navEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val navEvent: SharedFlow<Unit> = _navEvent.asSharedFlow()

    private val _isNetworkDown = MutableStateFlow(false)
    val isNetworkDown: StateFlow<Boolean> = _isNetworkDown.asStateFlow()

    init {
        scope.launch {
            sessionEventBus.events.collect { event ->
                when (event) {
                    SessionEvent.Unauthorized -> {
                        authRepository.logout()
                        _navEvent.emit(Unit)
                    }
                    SessionEvent.NetworkDown -> _isNetworkDown.value = true
                }
            }
        }
    }
}
