package com.textvision.alistclient.file

import com.textvision.alistclient.common.result.ApiResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyMoveUseCaseTest {
    private class FakeRepo : FileOperationRepositoryContract {
        val copied = mutableListOf<String>()
        var failOn: String? = null
        override suspend fun copy(srcPath: String, dstDir: String): ApiResult<Unit> {
            if (srcPath == failOn) return ApiResult.Failure(409, "文件已存在")
            copied += srcPath
            return ApiResult.Success(Unit)
        }
        override suspend fun move(srcPath: String, dstDir: String): ApiResult<Unit> = copy(srcPath, dstDir)
    }

    @Test fun stopsOnFirstCopyFailureAndKeepsSuccessCount() = runTest {
        val repo = FakeRepo().apply { failOn = "/c" }
        val result = CopyMoveUseCase(repo).copyMultiple(listOf("/a", "/b", "/c", "/d"), "/target") { _, _ -> }
        assertEquals(2, result.success)
        assertEquals(2, result.failed)
        assertTrue(result.stopped)
        assertEquals(listOf("/a", "/b"), repo.copied)
        assertEquals("/c", result.firstFailure?.first)
    }
}
