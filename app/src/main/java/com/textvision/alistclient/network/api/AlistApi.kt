package com.textvision.alistclient.network.api

import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.dto.AlistFsList
import com.textvision.alistclient.network.dto.AlistLoginData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.CopyMovePathRequest
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.FsListRequest
import com.textvision.alistclient.network.dto.FsSearchRequest
import com.textvision.alistclient.network.dto.LoginRequest
import com.textvision.alistclient.network.dto.MkdirRequest
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.RemoveRequest
import com.textvision.alistclient.network.dto.RenameRequest
import com.textvision.alistclient.network.dto.RoleList
import com.textvision.alistclient.network.dto.SessionInfo
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.network.dto.SettingSaveRequest
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.StoragePatch
import com.textvision.alistclient.network.dto.TaskInfo
import com.textvision.alistclient.network.dto.UserList
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

    @GET
    suspend fun listStorage(@Url url: String, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 0): AlistResponse<StorageList>

    @GET
    suspend fun listUsers(@Url url: String, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 0): AlistResponse<UserList>

    @GET
    suspend fun listRoles(@Url url: String, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 0): AlistResponse<RoleList>

    @GET
    suspend fun listSessions(@Url url: String): AlistResponse<List<SessionInfo>>

    @GET
    suspend fun taskUndone(@Url url: String): AlistResponse<List<TaskInfo>>

    @GET
    suspend fun getPublicSettings(@Url url: String, @Header(SkipAuthRetry.HEADER) skipAuthRetry: String = SkipAuthRetry.HEADER): AlistResponse<PublicSettings>

    @POST
    suspend fun updateStorage(@Url url: String, @Body body: StoragePatch): AlistResponse<Unit>

    @POST
    suspend fun enableStorage(@Url url: String, @Query("id") id: Long): AlistResponse<Unit>

    @POST
    suspend fun disableStorage(@Url url: String, @Query("id") id: Long): AlistResponse<Unit>

    @GET
    suspend fun listDrivers(@Url url: String, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 0): AlistResponse<Map<String, DriverInfo>>

    @GET
    suspend fun listSettings(@Url url: String, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 0): AlistResponse<List<SettingItem>>

    @POST
    suspend fun saveSettings(@Url url: String, @Body body: SettingSaveRequest): AlistResponse<Unit>
}
