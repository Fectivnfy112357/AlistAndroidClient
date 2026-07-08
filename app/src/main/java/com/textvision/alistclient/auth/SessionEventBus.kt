package com.textvision.alistclient.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-level events surfaced from low-level layers (e.g. the OkHttp [AuthInterceptor])
 * up to the UI. Kept dependency-free so it can be injected into the interceptor without
 * forming the cycle AuthInterceptor -> OkHttp -> Retrofit -> AlistApi -> AuthRepository.
 */
sealed interface SessionEvent {
    data object Unauthorized : SessionEvent
    data object NetworkDown : SessionEvent
}

@Singleton
class SessionEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    /** Non-suspending, safe to call from the OkHttp interceptor thread. */
    fun emit(event: SessionEvent) {
        _events.tryEmit(event)
    }
}
