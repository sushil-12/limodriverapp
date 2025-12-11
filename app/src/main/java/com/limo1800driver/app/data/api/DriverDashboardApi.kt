package com.limo1800driver.app.data.api

import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.dashboard.*
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
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): BaseResponse<DriverBookingsData>
    
    // Driver All Activity
    @GET("api/mobile/v1/driver/bookings/all-activity")
    suspend fun getDriverAllActivity(
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): BaseResponse<DriverAllActivityData>
    
    // Driver Earnings Summary
    @GET("api/mobile/v1/driver/bookings/earnings/summary")
    suspend fun getDriverEarningsSummary(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
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
    @GET("api/affiliate/get-reservation")
    suspend fun getReservation(
        @Query("booking_id") bookingId: Int
    ): BaseResponse<ReservationData>
    
    // Get Reservation Rates
    @GET("api/admin/reservation-rates/{bookingId}")
    suspend fun getReservationRates(
        @Path("bookingId") bookingId: Int
    ): BaseResponse<ReservationRatesData>
    
    // Booking Rates Vehicle
    @POST("api/admin/booking-rates-vehicle")
    suspend fun getBookingRatesVehicle(
        @Body request: BookingRatesVehicleRequest
    ): BaseResponse<BookingRatesVehicleData>
    
    // Finalize Rate Edit
    @POST("api/affiliate/finalize-rate-edit")
    suspend fun finalizeRateEdit(
        @Body request: FinalizeRateEditRequest
    ): BaseResponse<FinalizeRateEditData>
    
    // Affiliate Payment Processing
    @POST("api/affiliate/affiliate-payment-processing")
    suspend fun processPayment(
        @Body request: PaymentProcessingRequest
    ): BaseResponse<PaymentProcessingData>
    
    // Booking Preview
    @GET("api/admin/get-booking-preview/{bookingId}")
    suspend fun getBookingPreview(
        @Path("bookingId") bookingId: Int
    ): BaseResponse<BookingPreviewData>
    
    // Edit Reservation
    @POST("api/affiliate/edit-reservation")
    suspend fun editReservation(
        @Body request: EditReservationRequest
    ): BaseResponse<EditReservationData>
}

