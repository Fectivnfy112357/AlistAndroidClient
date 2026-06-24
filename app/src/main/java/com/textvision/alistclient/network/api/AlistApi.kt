package com.textvision.alistclient.network.api

import com.textvision.alistclient.network.dto.AlistLoginData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.LoginRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AlistApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AlistResponse<AlistLoginData>
}
