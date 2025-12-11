package com.limo1800driver.app.data.model.dashboard

import com.google.gson.annotations.SerializedName

// ==================== Dashboard Stats ====================

data class DriverDashboardStatsData(
    @SerializedName("today")
    val today: DashboardStatsPeriod?,
    
    @SerializedName("weekly")
    val weekly: DashboardStatsPeriod?,
    
    @SerializedName("monthly")
    val monthly: DashboardStatsPeriod?,
    
    @SerializedName("total_rides")
    val totalRides: Int?,
    
    @SerializedName("currency_symbol")
    val currencySymbol: String?
)

data class DashboardStatsPeriod(
    @SerializedName("earnings")
    val earnings: Double?,
    
    @SerializedName("rides")
    val rides: Int?,
    
    @SerializedName("online_time")
    val onlineTime: String?
)

// ==================== Driver Bookings ====================

data class DriverBookingsData(
    @SerializedName("bookings")
    val bookings: List<DriverBooking>,
    
    @SerializedName("pagination")
    val pagination: BookingPagination?
)

data class DriverBooking(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("individual_account_id")
    val individualAccountId: Int,
    
    @SerializedName("travel_client_id")
    val travelClientId: Int?,
    
    @SerializedName("pickup_date")
    val pickupDate: String,
    
    @SerializedName("pickup_time")
    val pickupTime: String,
    
    @SerializedName("dropoff_longitude")
    val dropoffLongitude: String?,
    
    @SerializedName("dropoff_latitude")
    val dropoffLatitude: String?,
    
    @SerializedName("pickup_longitude")
    val pickupLongitude: String?,
    
    @SerializedName("pickup_latitude")
    val pickupLatitude: String?,
    
    @SerializedName("changed_fields")
    val changedFields: String?,
    
    @SerializedName("service_type")
    val serviceType: String?,
    
    @SerializedName("transfer_type")
    val transferType: String,
    
    @SerializedName("payment_status")
    val paymentStatus: String,
    
    @SerializedName("created_by_role")
    val createdByRole: String,
    
    @SerializedName("payment_method")
    val paymentMethod: String?,
    
    @SerializedName("booking_status")
    val bookingStatus: String,
    
    @SerializedName("affiliate_id")
    val affiliateId: Int,
    
    @SerializedName("passenger_name")
    val passengerName: String?,
    
    @SerializedName("passenger_email")
    val passengerEmail: String?,
    
    @SerializedName("passenger_cell_isd")
    val passengerCellIsd: String?,
    
    @SerializedName("passenger_cell_country")
    val passengerCellCountry: String?,
    
    @SerializedName("passenger_cell")
    val passengerCell: String?,
    
    @SerializedName("passenger_image")
    val passengerImage: String?,
    
    @SerializedName("pickup_address")
    val pickupAddress: String,
    
    @SerializedName("account_type")
    val accountType: String?,
    
    @SerializedName("dropoff_address")
    val dropoffAddress: String,
    
    @SerializedName("affiliate_charged_amount")
    val affiliateChargedAmount: String?,
    
    @SerializedName("vehicle_cat_name")
    val vehicleCatName: String?,
    
    @SerializedName("affiliate_type")
    val affiliateType: String?,
    
    @SerializedName("company_name")
    val companyName: String?,
    
    @SerializedName("affiliate_dispatch_isd")
    val affiliateDispatchIsd: String,
    
    @SerializedName("affiliate_dispatch_number")
    val affiliateDispatchNumber: String,
    
    @SerializedName("dispatchEmail")
    val dispatchEmail: String,
    
    @SerializedName("gig_cell_isd")
    val gigCellIsd: String,
    
    @SerializedName("gig_cell_mobile")
    val gigCellMobile: String,
    
    @SerializedName("gig_email")
    val gigEmail: String,
    
    @SerializedName("reservation_type")
    val reservationType: String?,
    
    @SerializedName("charge_object_id")
    val chargeObjectId: String?,
    
    @SerializedName("grand_total")
    val grandTotal: Double?,
    
    @SerializedName("currency")
    val currency: String?,
    
    @SerializedName("farmout_affiliate")
    val farmoutAffiliate: Int?,
    
    @SerializedName("currency_symbol")
    val currencySymbol: String,
    
    @SerializedName("driver_rating")
    val driverRating: Double?,
    
    @SerializedName("passenger_rating")
    val passengerRating: Double?,
    
    @SerializedName("show_changed_fields")
    val showChangedFields: Boolean?
)

data class BookingPagination(
    @SerializedName("current_page")
    val currentPage: Int,
    
    @SerializedName("per_page")
    val perPage: Int,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("last_page")
    val lastPage: Int,
    
    @SerializedName("from")
    val from: Int?,
    
    @SerializedName("to")
    val to: Int?
)

// ==================== Driver All Activity ====================

data class DriverAllActivityData(
    @SerializedName("bookings")
    val bookings: List<DriverBooking>,
    
    @SerializedName("pagination")
    val pagination: BookingPagination?
)

// ==================== Driver Earnings Summary ====================

data class DriverEarningsSummaryData(
    @SerializedName("total_earnings")
    val totalEarnings: Double?,
    
    @SerializedName("total_rides")
    val totalRides: Int?,
    
    @SerializedName("currency_symbol")
    val currencySymbol: String?,
    
    @SerializedName("period")
    val period: EarningsPeriod?
)

data class EarningsPeriod(
    @SerializedName("start_date")
    val startDate: String?,
    
    @SerializedName("end_date")
    val endDate: String?
)

// ==================== Driver Updates ====================

data class DriverUpdatesData(
    @SerializedName("updates")
    val updates: List<DriverUpdate>?
)

data class DriverUpdate(
    @SerializedName("id")
    val id: Int?,
    
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("booking_id")
    val bookingId: Int?,
    
    @SerializedName("created_at")
    val createdAt: String?
)

// ==================== Driver Profile ====================

data class DriverProfileData(
    @SerializedName("driver_first_name")
    val driverFirstName: String?,
    
    @SerializedName("driver_last_name")
    val driverLastName: String?,
    
    @SerializedName("driver_email")
    val driverEmail: String?,
    
    @SerializedName("driver_phone")
    val driverPhone: String?,
    
    @SerializedName("driver_image")
    val driverImage: String?,
    
    @SerializedName("affiliate_type")
    val affiliateType: String?,
    
    @SerializedName("company_name")
    val companyName: String?
)

// ==================== Driver Wallet ====================

data class DriverWalletData(
    @SerializedName("balance")
    val balance: Double?,
    
    @SerializedName("currency_symbol")
    val currencySymbol: String?,
    
    @SerializedName("transactions")
    val transactions: List<WalletTransaction>?,
    
    @SerializedName("pagination")
    val pagination: BookingPagination?
)

data class WalletTransaction(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("amount")
    val amount: Double?,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("created_at")
    val createdAt: String?,
    
    @SerializedName("booking_id")
    val bookingId: Int?
)

// ==================== Driver Wallet Details ====================

data class DriverWalletDetailsData(
    @SerializedName("total_earnings")
    val totalEarnings: Double?,
    
    @SerializedName("total_withdrawals")
    val totalWithdrawals: Double?,
    
    @SerializedName("current_balance")
    val currentBalance: Double?,
    
    @SerializedName("currency_symbol")
    val currencySymbol: String?
)

// ==================== Booking Audit Records ====================

data class BookingAuditRecordsData(
    @SerializedName("records")
    val records: List<AuditRecord>?,
    
    @SerializedName("pagination")
    val pagination: BookingPagination?
)

data class AuditRecord(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("field_name")
    val fieldName: String?,
    
    @SerializedName("old_value")
    val oldValue: String?,
    
    @SerializedName("new_value")
    val newValue: String?,
    
    @SerializedName("changed_by")
    val changedBy: String?,
    
    @SerializedName("created_at")
    val createdAt: String?
)

// ==================== Reservation ====================

data class ReservationData(
    @SerializedName("reservation")
    val reservation: Reservation?
)

data class Reservation(
    @SerializedName("id")
    val id: Int?,
    
    @SerializedName("booking_id")
    val bookingId: Int?,
    
    @SerializedName("vehicle_id")
    val vehicleId: Int?,
    
    @SerializedName("rates")
    val rates: ReservationRates?
)

// ==================== Reservation Rates ====================

data class ReservationRatesData(
    @SerializedName("rates")
    val rates: ReservationRates?
)

data class ReservationRates(
    @SerializedName("base_rate")
    val baseRate: Double?,
    
    @SerializedName("per_mile_rate")
    val perMileRate: Double?,
    
    @SerializedName("per_hour_rate")
    val perHourRate: Double?,
    
    @SerializedName("currency")
    val currency: String?
)

// ==================== Booking Rates Vehicle ====================

data class BookingRatesVehicleRequest(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("vehicle_id")
    val vehicleId: Int
)

data class BookingRatesVehicleData(
    @SerializedName("rates")
    val rates: BookingRates?
)

data class BookingRates(
    @SerializedName("base_rate")
    val baseRate: Double?,
    
    @SerializedName("per_mile_rate")
    val perMileRate: Double?,
    
    @SerializedName("per_hour_rate")
    val perHourRate: Double?,
    
    @SerializedName("total")
    val total: Double?,
    
    @SerializedName("currency")
    val currency: String?
)

// ==================== Finalize Rate Edit ====================

data class FinalizeRateEditRequest(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("rates")
    val rates: Map<String, Any>
)

data class FinalizeRateEditData(
    @SerializedName("success")
    val success: Boolean?,
    
    @SerializedName("message")
    val message: String?
)

// ==================== Payment Processing ====================

data class PaymentProcessingRequest(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("payment_method")
    val paymentMethod: String
)

data class PaymentProcessingData(
    @SerializedName("success")
    val success: Boolean?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("transaction_id")
    val transactionId: String?
)

// ==================== Booking Preview ====================

data class BookingPreviewData(
    @SerializedName("booking")
    val booking: BookingPreview?
)

data class BookingPreview(
    @SerializedName("booking_id")
    val bookingId: Int?,
    
    @SerializedName("pickup_address")
    val pickupAddress: String?,
    
    @SerializedName("dropoff_address")
    val dropoffAddress: String?,
    
    @SerializedName("pickup_date")
    val pickupDate: String?,
    
    @SerializedName("pickup_time")
    val pickupTime: String?,
    
    @SerializedName("passenger_name")
    val passengerName: String?,
    
    @SerializedName("grand_total")
    val grandTotal: Double?,
    
    @SerializedName("currency_symbol")
    val currencySymbol: String?
)

// ==================== Edit Reservation ====================

data class EditReservationRequest(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("pickup_address")
    val pickupAddress: String?,
    
    @SerializedName("dropoff_address")
    val dropoffAddress: String?,
    
    @SerializedName("pickup_date")
    val pickupDate: String?,
    
    @SerializedName("pickup_time")
    val pickupTime: String?,
    
    @SerializedName("vehicle_id")
    val vehicleId: Int?,
    
    @SerializedName("rates")
    val rates: Map<String, Any>?
)

data class EditReservationData(
    @SerializedName("success")
    val success: Boolean?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("booking")
    val booking: DriverBooking?
)

