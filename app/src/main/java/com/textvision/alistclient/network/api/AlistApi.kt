package com.textvision.alistclient.network.api

import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.dto.AlistLoginData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.LoginRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface AlistApi {
    @POST
    suspend fun login(@Url url: String, @Header(SkipAuthRetry.HEADER) skipAuthRetry: String, @Body request: LoginRequest): AlistResponse<AlistLoginData>
}
