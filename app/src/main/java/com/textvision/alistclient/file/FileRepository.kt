package com.textvision.alistclient.file

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.FsListRequest
import com.textvision.alistclient.network.dto.FsSearchRequest
import com.textvision.alistclient.network.dto.MkdirRequest
import com.textvision.alistclient.network.dto.RemoveRequest
import com.textvision.alistclient.network.dto.RenameRequest
import com.textvision.alistclient.network.dto.toFileItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val api: AlistApi,
) {
    private val baseUrl: String = "" // Phase 1 dynamic base URL replacement can update this provider later.

    suspend fun list(path: String): ApiResult<List<FileItem>> = runAlist {
        val response = api.list(FsListRequest(path = path))
        if (response.code == 200) response.data?.content.orEmpty()
            .map { it.toFileItem(path, baseUrl) }
            .sortedWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.name.lowercase() })
            .let { ApiResult.Success(it) }
        else ApiResult.Failure(response.code, response.message)
    }

    suspend fun search(path: String, keyword: String): ApiResult<List<FileItem>> = runAlist {
        val response = api.search(FsSearchRequest(path = path, keywords = keyword))
        if (response.code == 200) ApiResult.Success(response.data?.content.orEmpty().map { it.toFileItem(path, baseUrl) })
        else ApiResult.Failure(response.code, response.message)
    }

    suspend fun mkdir(path: String): ApiResult<Unit> = runUnit { api.mkdir(MkdirRequest(path)) }
    suspend fun rename(path: String, name: String): ApiResult<Unit> = runUnit { api.rename(RenameRequest(path, name)) }

    suspend fun delete(paths: List<String>): ApiResult<Unit> {
        if (paths.isEmpty()) return ApiResult.Success(Unit)
        val dir = paths.first().substringBeforeLast('/', missingDelimiterValue = "/").ifBlank { "/" }
        val names = paths.map { it.substringAfterLast('/') }
        return runUnit { api.remove(RemoveRequest(dir, names)) }
    }

    private suspend fun runUnit(block: suspend () -> com.textvision.alistclient.network.dto.AlistResponse<Unit>): ApiResult<Unit> = runAlist {
        val response = block()
        if (response.code == 200) ApiResult.Success(Unit) else ApiResult.Failure(response.code, response.message)
    }

    private suspend fun <T> runAlist(block: suspend () -> ApiResult<T>): ApiResult<T> = try { block() } catch (t: Throwable) { ApiResult.NetworkError(t) }
}