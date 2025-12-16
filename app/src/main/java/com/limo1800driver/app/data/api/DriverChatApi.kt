package com.limo1800driver.app.data.api

import com.limo1800driver.app.data.socket.DriverChatHistoryResponse
import com.limo1800driver.app.data.socket.DriverChatSendRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Chat REST API (mirrors iOS `ChatService.fetchChatHistory` / `sendMessageViaAPI`).
 *
 * Base URL: https://limortservice.infodevbox.com
 * Auth: x-secret header (same secret as socket connect params).
 */
interface DriverChatApi {
    companion object {
        const val CHAT_BASE_URL = "https://limortservice.infodevbox.com/"
        const val CHAT_SECRET = "limoapi_notifications_secret_2024_xyz789"
    }

    @GET("api/messages/{bookingId}")
    suspend fun getChatHistory(
        @Path("bookingId") bookingId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Header("x-secret") secret: String = CHAT_SECRET
    ): DriverChatHistoryResponse

    @POST("api/messages/send")
    suspend fun sendMessage(
        @Body request: DriverChatSendRequest,
        @Header("x-secret") secret: String = CHAT_SECRET
    ): Any
}


