package com.textvision.alistclient.file

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.CopyMoveResult
import javax.inject.Inject

interface FileOperationRepositoryContract {
    suspend fun copy(srcPath: String, dstDir: String): ApiResult<Unit>
    suspend fun move(srcPath: String, dstDir: String): ApiResult<Unit>
}

class CopyMoveUseCase @Inject constructor(
    private val repository: FileOperationRepositoryContract,
) {
    suspend fun copyMultiple(srcPaths: List<String>, targetDir: String, onProgress: (Int, Int) -> Unit): CopyMoveResult =
        runSerial(srcPaths, targetDir, onProgress, repository::copy)

    suspend fun moveMultiple(srcPaths: List<String>, targetDir: String, onProgress: (Int, Int) -> Unit): CopyMoveResult =
        runSerial(srcPaths, targetDir, onProgress, repository::move)

    private suspend fun runSerial(
        srcPaths: List<String>,
        targetDir: String,
        onProgress: (Int, Int) -> Unit,
        operation: suspend (String, String) -> ApiResult<Unit>,
    ): CopyMoveResult {
        var success = 0
        for (src in srcPaths) {
            when (val result = operation(src, targetDir)) {
                is ApiResult.Success -> {
                    success++
                    onProgress(success, srcPaths.size)
                }
                is ApiResult.Failure -> return CopyMoveResult(srcPaths.size, success, srcPaths.size - success, src to result.message, stopped = true)
                is ApiResult.NetworkError -> return CopyMoveResult(srcPaths.size, success, srcPaths.size - success, src to (result.cause.message ?: "操作失败"), stopped = true)
            }
        }
        return CopyMoveResult(srcPaths.size, success, 0, stopped = false)
    }
}