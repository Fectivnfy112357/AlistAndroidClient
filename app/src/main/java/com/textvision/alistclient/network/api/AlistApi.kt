package com.textvision.alistclient.network.api

import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.dto.AlistFsList
import com.textvision.alistclient.network.dto.AlistLoginData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.FsListRequest
import com.textvision.alistclient.network.dto.FsSearchRequest
import com.textvision.alistclient.network.dto.LoginRequest
import com.textvision.alistclient.network.dto.MkdirRequest
import com.textvision.alistclient.network.dto.RemoveRequest
import com.textvision.alistclient.network.dto.RenameRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface AlistApi {
    @POST
    suspend fun login(@Url url: String, @Header(SkipAuthRetry.HEADER) skipAuthRetry: String, @Body request: LoginRequest): AlistResponse<AlistLoginData>

    @POST("api/fs/list")
    suspend fun list(@Body request: FsListRequest): AlistResponse<AlistFsList>

    @POST("api/fs/search")
    suspend fun search(@Body request: FsSearchRequest): AlistResponse<AlistFsList>

    @POST("api/fs/mkdir")
    suspend fun mkdir(@Body request: MkdirRequest): AlistResponse<Unit>

    @POST("api/fs/rename")
    suspend fun rename(@Body request: RenameRequest): AlistResponse<Unit>

    @POST("api/fs/remove")
    suspend fun remove(@Body request: RemoveRequest): AlistResponse<Unit>
}
