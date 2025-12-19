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
                val response = dashboardApi.getDashboardStats()
                Result.success(response)
            } catch (e: Exception) {
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
                val response = dashboardApi.getDriverBookings(page, perPage, startDate, endDate, search, status)
                Result.success(response)
            } catch (e: Exception) {
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
                val response = dashboardApi.getDriverAllActivity(page, perPage, startDate, endDate)
                Result.success(response)
            } catch (e: Exception) {
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
                val response = dashboardApi.getDriverEarningsSummary(startDate, endDate)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Driver Updates ====================
    
    suspend fun getDriverUpdates(): Result<BaseResponse<DriverUpdatesData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = dashboardApi.getDriverUpdates()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    // ==================== Driver Profile ====================
    
    suspend fun getDriverProfile(): Result<BaseResponse<DriverProfileData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = dashboardApi.getDriverProfile()
                Result.success(response)
            } catch (e: Exception) {
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
                val response = dashboardApi.getDriverWallet(page, perPage)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    suspend fun getDriverWalletDetails(): Result<BaseResponse<DriverWalletDetailsData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = dashboardApi.getDriverWalletDetails()
                Result.success(response)
            } catch (e: Exception) {
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
    
    suspend fun getReservationRates(bookingId: Int): Result<BaseResponse<AdminReservationRatesData>> {
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
        request: FinalizeRatesRequest
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
    
    suspend fun getBookingPreview(bookingId: Int): Result<BaseResponse<AdminBookingPreviewData>> {
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

    // ==================== Booking Status (Accept/Reject) ====================

    suspend fun acceptBooking(
        bookingId: Int
    ): Result<BaseResponse<BookingStatusData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Accepting booking: $bookingId")
                val response = dashboardApi.acceptBooking(bookingId)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to accept booking")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }

    suspend fun rejectBooking(
        bookingId: Int
    ): Result<BaseResponse<BookingStatusData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Rejecting booking: $bookingId")
                val response = dashboardApi.rejectBooking(bookingId)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to reject booking")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }

    suspend fun getCreditCardDetails(bookingId: Int): Result<BaseResponse<com.google.gson.JsonElement>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching credit card details for booking: $bookingId")
                val response = dashboardApi.getCreditCardDetails(bookingId)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch credit card details")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }

    suspend fun processCreditCardPayment(
        request: CreditCardPaymentRequest
    ): Result<BaseResponse<PaymentProcessingData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Processing credit card payment")
                val response = dashboardApi.processCreditCardPayment(request)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to process credit card payment")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }

    // ==================== Mobile Data (Airports/Airlines) ====================

    suspend fun getMobileDataAirlines(): Result<BaseResponse<MobileDataAirlinesData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching mobile data: airlines")
                val response = dashboardApi.getMobileDataAirlines(true)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch mobile airlines")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }

    suspend fun getMobileDataAirports(): Result<BaseResponse<MobileDataAirportsData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching mobile data: airports")
                val response = dashboardApi.getMobileDataAirports(true)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch mobile airports")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
}

