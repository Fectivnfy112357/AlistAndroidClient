package com.textvision.alistclient.auth

import javax.inject.Inject

import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.common.result.ApiResult
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private class FakeAuthRepository : AuthRepositoryContract {
        var result: ApiResult<SavedSession> = ApiResult.Success(SavedSession("http://s/", "u", "p", "t"))
        override suspend fun login(serverUrl: String, username: String, password: String): ApiResult<SavedSession> = result
        override fun loadSavedSession(): SavedSession? = null
    }

    private class TrackingDispatcher(
        private val delegate: CoroutineDispatcher,
    ) : CoroutineDispatcher() {
        private val active = ThreadLocal.withInitial { false }

        val isRunning: Boolean
            get() = active.get() == true

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            delegate.dispatch(context) {
                active.set(true)
                try {
                    block.run()
                } finally {
                    active.set(false)
                }
            }
        }
    }

    @Test fun injectedConstructorAcceptsBackgroundDispatcher() {
        val injectedConstructor = LoginViewModel::class.java.constructors.single {
            it.isAnnotationPresent(Inject::class.java)
        }

        assertTrue(injectedConstructor.parameterTypes.contains(CoroutineDispatcher::class.java))
    }

    @Test fun httpWarningUpdatesFromServerUrl() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(), StandardTestDispatcher(testScheduler))
        vm.updateServerUrl("http://example.com")
        assertTrue(vm.uiState.value.showHttpWarning)
    }

    @Test fun debugBuildPrefillsDevelopmentLoginFields() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(), StandardTestDispatcher(testScheduler))

        assertEquals("http://textvision.top:5244/", vm.uiState.value.serverUrl)
        assertEquals("fectivnfy", vm.uiState.value.username)
        assertEquals("Yishengaini12345", vm.uiState.value.password)
        assertTrue(vm.uiState.value.showHttpWarning)
    }

    @Test fun schemelessServerUrlShowsHttpWarningBecauseItNormalizesToHttp() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(), StandardTestDispatcher(testScheduler))

        vm.updateServerUrl("example.com")

        assertTrue(vm.uiState.value.showHttpWarning)
    }

    @Test fun httpsServerUrlDoesNotShowHttpWarning() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(), StandardTestDispatcher(testScheduler))

        vm.updateServerUrl("https://example.com")

        assertFalse(vm.uiState.value.showHttpWarning)
    }

    @Test fun blankAndInvalidServerUrlsDoNotShowMisleadingHttpWarning() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(), StandardTestDispatcher(testScheduler))

        vm.updateServerUrl("   ")
        assertFalse(vm.uiState.value.showHttpWarning)

        vm.updateServerUrl("ftp://bad")
        assertFalse(vm.uiState.value.showHttpWarning)
    }

    @Test fun invalidUrlShowsError() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(), StandardTestDispatcher(testScheduler))
        vm.updateServerUrl("ftp://bad")
        vm.updateUsername("admin")
        vm.updatePassword("pw")
        vm.login()
        testScheduler.advanceUntilIdle()
        assertEquals("仅支持 HTTP 或 HTTPS", vm.uiState.value.errorMessage)
    }

    @Test fun serializationFailureShowsNotAlistServerMessage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeAuthRepository().apply {
                result = ApiResult.NetworkError(SerializationException("unexpected html"))
            }
            val vm = LoginViewModel(repository, StandardTestDispatcher(testScheduler))
            vm.updateServerUrl("https://example.com")
            vm.updateUsername("admin")
            vm.updatePassword("pw")

            vm.login()
            testScheduler.advanceUntilIdle()

            assertEquals("该地址不是 Alist 服务", vm.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun successfulLoginInvokesOnSuccessAfterReturningToMainContext() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val ioDispatcher = TrackingDispatcher(StandardTestDispatcher(testScheduler))
            val vm = LoginViewModel(FakeAuthRepository(), ioDispatcher)
            vm.updateServerUrl("https://example.com")
            vm.updateUsername("admin")
            vm.updatePassword("pw")
            var callbackRanOnIoDispatcher = true

            vm.login {
                callbackRanOnIoDispatcher = ioDispatcher.isRunning
            }
            testScheduler.advanceUntilIdle()

            assertFalse(callbackRanOnIoDispatcher)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
