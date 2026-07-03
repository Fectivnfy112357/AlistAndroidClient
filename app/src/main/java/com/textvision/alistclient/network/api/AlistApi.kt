package com.textvision.alistclient.network.api

import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.dto.AdminInfo
import com.textvision.alistclient.network.dto.AlistFsList
import com.textvision.alistclient.network.dto.AlistLoginData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.CopyMovePathRequest
import com.textvision.alistclient.network.dto.FsListRequest
import com.textvision.alistclient.network.dto.FsSearchRequest
import com.textvision.alistclient.network.dto.LoginRequest
import com.textvision.alistclient.network.dto.MkdirRequest
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.RemoveRequest
import com.textvision.alistclient.network.dto.RenameRequest
import com.textvision.alistclient.network.dto.StorageList
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface AlistApi {
    @POST
    suspend fun login(@Url url: String, @Header(SkipAuthRetry.HEADER) skipAuthRetry: String, @Body request: LoginRequest): AlistResponse<AlistLoginData>

    @POST
    suspend fun list(@Url url: String, @Body request: FsListRequest): AlistResponse<AlistFsList>

    @POST
    suspend fun search(@Url url: String, @Body request: FsSearchRequest): AlistResponse<AlistFsList>

    @POST
    suspend fun mkdir(@Url url: String, @Body request: MkdirRequest): AlistResponse<Unit>

    @POST
    suspend fun rename(@Url url: String, @Body request: RenameRequest): AlistResponse<Unit>

    @POST
    suspend fun remove(@Url url: String, @Body request: RemoveRequest): AlistResponse<Unit>

    @POST
    suspend fun copy(@Url url: String, @Body request: CopyMovePathRequest): AlistResponse<Unit>

    @POST
    suspend fun move(@Url url: String, @Body request: CopyMovePathRequest): AlistResponse<Unit>

    @POST
    suspend fun adminInfo(@Url url: String, @Header(SkipAuthRetry.HEADER) skipAuthRetry: String, @Body request: com.textvision.alistclient.network.dto.AdminInfoRequest = com.textvision.alistclient.network.dto.AdminInfoRequest()): AlistResponse<AdminInfo>

    @GET
    suspend fun listStorage(@Url url: String, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 0): AlistResponse<StorageList>
    @GET
    suspend fun getPublicSettings(@Url url: String, @Header(SkipAuthRetry.HEADER) skipAuthRetry: String = SkipAuthRetry.HEADER): AlistResponse<PublicSettings>
}
