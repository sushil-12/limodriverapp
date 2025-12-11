package com.limo1800driver.app.data.repository

import com.limo1800driver.app.data.api.DriverDashboardApi
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.dashboard.*
import com.limo1800driver.app.data.network.error.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Driver Dashboard operations
 * Handles all dashboard-related API calls
 */
@Singleton
class DriverDashboardRepository @Inject constructor(
    private val dashboardApi: DriverDashboardApi,
    private val errorHandler: ErrorHandler
) {
    
    companion object {
        private const val TAG = "DriverDashboardRepository"
    }
    
    // ==================== Dashboard Stats ====================
    
    suspend fun getDashboardStats(): Result<BaseResponse<DriverDashboardStatsData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching dashboard stats")
                val response = dashboardApi.getDashboardStats()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch dashboard stats")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Driver Bookings ====================
    
    suspend fun getDriverBookings(
        page: Int? = null,
        perPage: Int? = null,
        startDate: String? = null,
        endDate: String? = null,
        search: String? = null,
        status: String? = null
    ): Result<BaseResponse<DriverBookingsData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching driver bookings - page: $page, startDate: $startDate, endDate: $endDate")
                val response = dashboardApi.getDriverBookings(page, perPage, startDate, endDate, search, status)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch driver bookings")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Driver All Activity ====================
    
    suspend fun getDriverAllActivity(
        page: Int? = null,
        perPage: Int? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<BaseResponse<DriverAllActivityData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching driver all activity")
                val response = dashboardApi.getDriverAllActivity(page, perPage, startDate, endDate)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch driver all activity")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Driver Earnings Summary ====================
    
    suspend fun getDriverEarningsSummary(
        startDate: String? = null,
        endDate: String? = null
    ): Result<BaseResponse<DriverEarningsSummaryData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching driver earnings summary")
                val response = dashboardApi.getDriverEarningsSummary(startDate, endDate)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch driver earnings summary")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Driver Updates ====================
    
    suspend fun getDriverUpdates(): Result<BaseResponse<DriverUpdatesData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching driver updates")
                val response = dashboardApi.getDriverUpdates()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch driver updates")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Driver Profile ====================
    
    suspend fun getDriverProfile(): Result<BaseResponse<DriverProfileData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching driver profile")
                val response = dashboardApi.getDriverProfile()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch driver profile")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Driver Wallet ====================
    
    suspend fun getDriverWallet(
        page: Int? = null,
        perPage: Int? = null
    ): Result<BaseResponse<DriverWalletData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching driver wallet")
                val response = dashboardApi.getDriverWallet(page, perPage)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch driver wallet")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    suspend fun getDriverWalletDetails(): Result<BaseResponse<DriverWalletDetailsData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching driver wallet details")
                val response = dashboardApi.getDriverWalletDetails()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch driver wallet details")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Booking Audit Records ====================
    
    suspend fun getBookingAuditRecords(
        bookingId: Int,
        page: Int? = null,
        perPage: Int? = null
    ): Result<BaseResponse<BookingAuditRecordsData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching booking audit records for booking: $bookingId")
                val response = dashboardApi.getBookingAuditRecords(bookingId, page, perPage)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch booking audit records")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Reservation ====================
    
    suspend fun getReservation(bookingId: Int): Result<BaseResponse<ReservationData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching reservation for booking: $bookingId")
                val response = dashboardApi.getReservation(bookingId)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch reservation")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    suspend fun getReservationRates(bookingId: Int): Result<BaseResponse<ReservationRatesData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching reservation rates for booking: $bookingId")
                val response = dashboardApi.getReservationRates(bookingId)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch reservation rates")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Booking Rates Vehicle ====================
    
    suspend fun getBookingRatesVehicle(
        request: BookingRatesVehicleRequest
    ): Result<BaseResponse<BookingRatesVehicleData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching booking rates for vehicle")
                val response = dashboardApi.getBookingRatesVehicle(request)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch booking rates vehicle")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Finalize Rate Edit ====================
    
    suspend fun finalizeRateEdit(
        request: FinalizeRateEditRequest
    ): Result<BaseResponse<FinalizeRateEditData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Finalizing rate edit")
                val response = dashboardApi.finalizeRateEdit(request)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to finalize rate edit")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Payment Processing ====================
    
    suspend fun processPayment(
        request: PaymentProcessingRequest
    ): Result<BaseResponse<PaymentProcessingData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Processing payment")
                val response = dashboardApi.processPayment(request)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to process payment")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Booking Preview ====================
    
    suspend fun getBookingPreview(bookingId: Int): Result<BaseResponse<BookingPreviewData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching booking preview for booking: $bookingId")
                val response = dashboardApi.getBookingPreview(bookingId)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch booking preview")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Edit Reservation ====================
    
    suspend fun editReservation(
        request: EditReservationRequest
    ): Result<BaseResponse<EditReservationData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Editing reservation")
                val response = dashboardApi.editReservation(request)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to edit reservation")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
}

