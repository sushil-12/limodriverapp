package com.limo1800driver.app.data.api

import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.dashboard.*
import com.google.gson.JsonElement
import retrofit2.http.*

/**
 * API interface for Driver Dashboard endpoints
 */
interface DriverDashboardApi {
    
    // Dashboard Stats
    @GET("api/mobile/v1/driver/dashboard")
    suspend fun getDashboardStats(): BaseResponse<DriverDashboardStatsData>
    
    // Driver Bookings
    @GET("api/mobile/v1/driver/bookings")
    suspend fun getDriverBookings(
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("from") startDate: String? = null,
        @Query("to") endDate: String? = null,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): BaseResponse<DriverBookingsData>

    // Scheduled Pickups
    @GET("api/mobile/v1/driver/bookings/scheduled-pickups")
    suspend fun getScheduledPickups(
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("from") startDate: String? = null,
        @Query("to") endDate: String? = null,
        @Query("search") search: String? = null
    ): BaseResponse<DriverBookingsData>
    
    // Driver All Activity
    @GET("api/mobile/v1/driver/bookings/all-activity")
    suspend fun getDriverAllActivity(
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("from") startDate: String? = null,
        @Query("to") endDate: String? = null,
        @Query("search") search: String? = null
    ): BaseResponse<DriverAllActivityData>
    
    // Driver Earnings Summary
    @GET("api/mobile/v1/driver/bookings/earnings/summary")
    suspend fun getDriverEarningsSummary(
        @Query("from") startDate: String? = null,
        @Query("to") endDate: String? = null
    ): BaseResponse<DriverEarningsSummaryData>
    
    // Driver Updates (Alerts/Warnings)
    @GET("api/mobile/v1/driver/bookings/error-warning-updates")
    suspend fun getDriverUpdates(): BaseResponse<DriverUpdatesData>
    
    // Driver Profile
    @GET("api/mobile/v1/driver/profile")
    suspend fun getDriverProfile(): BaseResponse<DriverProfileData>
    
    // Driver Wallet
    @GET("api/mobile/v1/driver/wallet")
    suspend fun getDriverWallet(
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): BaseResponse<DriverWalletData>
    
    // Driver Wallet Details
    @GET("api/mobile/v1/driver/wallet/details")
    suspend fun getDriverWalletDetails(): BaseResponse<DriverWalletDetailsData>
    
    // Booking Audit Records
    @GET("api/mobile/v1/driver/bookings/audit-records")
    suspend fun getBookingAuditRecords(
        @Query("booking_id") bookingId: Int,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): BaseResponse<BookingAuditRecordsData>
    
    // Get Reservation
    @GET("api/affiliate/get-reservation/{bookingId}")
    suspend fun getReservation(
        @Path("bookingId") bookingId: Int
    ): BaseResponse<ReservationData>
    
    // Get Reservation Rates
    @GET("api/admin/reservation-rates/{bookingId}")
    suspend fun getReservationRates(
        @Path("bookingId") bookingId: Int
    ): BaseResponse<AdminReservationRatesData>
    
    // Booking Rates Vehicle
    @POST("api/admin/booking-rates-vehicle")
    suspend fun getBookingRatesVehicle(
        @Body request: BookingRatesVehicleRequest
    ): BaseResponse<BookingRatesVehicleData>
    
    // Finalize Rate Edit
    @POST("api/affiliate/finalize-rate-edit")
    suspend fun finalizeRateEdit(
        @Body request: FinalizeRatesRequest
    ): BaseResponse<FinalizeRateEditData>
    
    // Affiliate Payment Processing
    @POST("api/affiliate/affiliate-payment-processing")
    suspend fun processPayment(
        @Body request: PaymentProcessingRequest
    ): BaseResponse<PaymentProcessingData>

    // Get Credit Card Details
    @GET("api/affiliate/get-credit-card-detail/{bookingId}")
    suspend fun getCreditCardDetails(
        @Path("bookingId") bookingId: Int
    ): BaseResponse<JsonElement>

    // Credit card payment (same endpoint, different payload)
    @POST("api/affiliate/affiliate-payment-processing")
    suspend fun processCreditCardPayment(
        @Body request: CreditCardPaymentRequest
    ): BaseResponse<PaymentProcessingData>
    
    // Booking Preview
    @GET("api/admin/get-booking-preview/{bookingId}")
    suspend fun getBookingPreview(
        @Path("bookingId") bookingId: Int
    ): BaseResponse<AdminBookingPreviewData>

    @GET("api/affiliate/get-booking-preview/{bookingId}")
    suspend fun getAffiliateBookingPreview(
        @Path("bookingId") bookingId: Int
    ): BaseResponse<AffiliateBookingPreviewData>
    
    // Edit Reservation
    @PUT("api/affiliate/edit-reservation")
    suspend fun editReservation(
        @Body request: EditReservationRequest
    ): BaseResponse<EditReservationData>

    // Mobile Data (Airlines / Airports) - used by iOS edit-booking flow
    @GET("api/mobile-data")
    suspend fun getMobileDataAirlines(
        @Query("only_airlines") onlyAirlines: Boolean = true
    ): BaseResponse<MobileDataAirlinesData>

    @GET("api/mobile-data")
    suspend fun getMobileDataAirports(
        @Query("only_airports") onlyAirports: Boolean = true
    ): BaseResponse<MobileDataAirportsData>

    // Accept Booking (iOS: /api/affiliate/change-booking-status/accepted/{bookingId})
    @GET("api/affiliate/change-booking-status/accepted/{bookingId}")
    suspend fun acceptBooking(
        @Path("bookingId") bookingId: Int
    ): BaseResponse<BookingStatusData>

    // Reject/Cancel Booking (iOS: /api/affiliate/cancel-booking/{bookingId})
    @GET("api/affiliate/cancel-booking/{bookingId}")
    suspend fun rejectBooking(
        @Path("bookingId") bookingId: Int
    ): BaseResponse<BookingStatusData>

    // Get User Notifications (iOS parity)
    @GET("api/notifications/user/{userId}")
    suspend fun getUserNotifications(
        @Path("userId") userId: String,
        @Header("X-Secret") secret: String = "limoapi_notifications_secret_2024_xyz789"
    ): BaseResponse<NotificationResponseData>
}

