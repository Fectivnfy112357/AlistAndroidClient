package com.textvision.alistclient.auth

import app.cash.turbine.test
import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.common.result.ApiResult
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginViewModelTest {
    private class FakeAuthRepository : AuthRepositoryContract {
        var result: ApiResult<SavedSession> = ApiResult.Success(SavedSession("http://s/", "u", "p", "t"))
        override suspend fun login(serverUrl: String, username: String, password: String): ApiResult<SavedSession> = result
    }

    @Test fun httpWarningUpdatesFromServerUrl() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(), StandardTestDispatcher(testScheduler))
        vm.updateServerUrl("http://example.com")
        assertTrue(vm.uiState.value.showHttpWarning)
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
}
