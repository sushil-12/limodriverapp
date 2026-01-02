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
    @SerializedName("data")
    val data: List<ActivityDateGroup>?,
    
    @SerializedName("total")
    val total: Int?,
    
    @SerializedName("per_page")
    val perPage: Int?,
    
    @SerializedName("current_page")
    val currentPage: Int?,
    
    @SerializedName("last_page")
    val lastPage: Int?,
    
    @SerializedName("from")
    val from: Int?,
    
    @SerializedName("to")
    val to: Int?,
    
    @SerializedName("weekly_summary")
    val weeklySummary: WeeklySummary?,
    
    @SerializedName("total_earnings")
    val totalEarnings: Double?
)

data class ActivityDateGroup(
    @SerializedName("date")
    val date: String,
    
    @SerializedName("day")
    val day: String,
    
    @SerializedName("date_key")
    val dateKey: String,
    
    @SerializedName("rides")
    val rides: List<ActivityRide>?,
    
    @SerializedName("total_earnings")
    val totalEarnings: Double?,
    
    @SerializedName("total_rides")
    val totalRides: Int?
)

data class ActivityRide(
    @SerializedName("ride_id")
    val rideId: Int,
    
    @SerializedName("time")
    val time: String,
    
    @SerializedName("destination")
    val destination: String,
    
    @SerializedName("pickup")
    val pickup: String,
    
    @SerializedName("earnings")
    val earnings: Double,
    
    @SerializedName("currency")
    val currency: String,
    
    @SerializedName("status")
    val status: String
)

data class WeeklySummary(
    @SerializedName("earnings")
    val earnings: Double?,
    
    @SerializedName("rides")
    val rides: Int?,
    
    @SerializedName("online_time")
    val onlineTime: String?,
    
    @SerializedName("date_range")
    val dateRange: String?
)

// ==================== Driver Earnings Summary ====================

data class DriverEarningsSummaryData(
    @SerializedName("currency")
    val currency: String?,
    
    @SerializedName("stripe_data")
    val stripeData: StripeData?
)

data class StripeData(
    @SerializedName("total_volume")
    val totalVolume: String?,
    
    @SerializedName("lifetime_volume")
    val lifetimeVolume: String?,
    
    @SerializedName("available_balance")
    val availableBalance: String?,
    
    @SerializedName("pending_balance")
    val pendingBalance: Any?, // Can be Int or Double
    
    @SerializedName("paid_balance")
    val paidBalance: String?,
    
    @SerializedName("recent_transfers")
    val recentTransfers: List<RecentTransfer>?,
    
    @SerializedName("next_payout_date")
    val nextPayoutDate: String?,
    
    @SerializedName("account_status")
    val accountStatus: String?,
    
    @SerializedName("account_info")
    val accountInfo: AccountInfo?
)

data class RecentTransfer(
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("amount")
    val amount: String?,
    
    @SerializedName("currency")
    val currency: String?,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("transfer_group")
    val transferGroup: String?,
    
    @SerializedName("reservation_id")
    val reservationId: Int?,
    
    @SerializedName("created_date")
    val createdDate: String?,
    
    @SerializedName("source_transaction")
    val sourceTransaction: String?,
    
    @SerializedName("status")
    val status: String?,
    
    @SerializedName("payout_date")
    val payoutDate: String?,
    
    @SerializedName("payout_id")
    val payoutId: String?
)

data class AccountInfo(
    @SerializedName("account_id")
    val accountId: String?,
    
    @SerializedName("charges_enabled")
    val chargesEnabled: Boolean?,
    
    @SerializedName("payouts_enabled")
    val payoutsEnabled: Boolean?
)

// ==================== Driver Updates ====================

data class DriverUpdatesData(
    @SerializedName("updates")
    val updates: List<DriverUpdate>?,
    
    @SerializedName("info")
    val info: List<DriverUpdateItem>?,
    
    @SerializedName("warning")
    val warning: List<DriverUpdateItem>?,
    
    @SerializedName("error")
    val error: List<DriverUpdateItem>?
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

data class DriverUpdateItem(
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("action_required")
    val actionRequired: Boolean?,
    
    @SerializedName("category")
    val category: String?,
    
    @SerializedName("account_id")
    val accountId: String?,
    
    @SerializedName("policy_number")
    val policyNumber: String?,
    
    @SerializedName("expiry_date")
    val expiryDate: String?,
    
    @SerializedName("days_overdue")
    val daysOverdue: Int?
)

// ==================== Driver Profile ====================

data class DriverProfileData(
    @SerializedName("driver_id")
    val driverId: Int? = null,

    @SerializedName("user_id")
    val userId: Int? = null,

    @SerializedName("driver_first_name")
    val driverFirstName: String?,
    
    @SerializedName("driver_last_name")
    val driverLastName: String?,

    @SerializedName("driver_cell_isd", alternate = ["driverCellIsd"])
    val driverCellIsd: String? = null,

    @SerializedName("driver_cell_number", alternate = ["driverCellNumber", "driver_cell_mobile"])
    val driverCellNumber: String? = null,
    
    @SerializedName("driver_email")
    val driverEmail: String?,
    
    // Some older endpoints may still return `driver_phone`.
    @SerializedName("driver_phone", alternate = ["driverPhone"])
    val driverPhone: String? = null,
    
    @SerializedName("driver_image")
    val driverImage: String?,

    @SerializedName("account")
    val account: DriverAccountSummary? = null,

    @SerializedName("vehicle")
    val vehicle: DriverVehicleSummary? = null,
    
    @SerializedName("affiliate_type")
    val affiliateType: String?,

    @SerializedName("driver_rating")
    val driverRating: String?,
    
    @SerializedName("company_name")
    val companyName: String?
)

data class DriverAccountSummary(
    @SerializedName("account_id")
    val accountId: Int? = null,

    @SerializedName("account_first_name")
    val accountFirstName: String? = null,

    @SerializedName("account_last_name")
    val accountLastName: String? = null,

    @SerializedName("account_email")
    val accountEmail: String? = null
)

data class DriverVehicleSummary(
    @SerializedName("vehicle_id")
    val vehicleId: Int? = null,

    @SerializedName("vehicle_cat_name")
    val vehicleCatName: String? = null,

    @SerializedName("vehicle_make_name")
    val vehicleMakeName: String? = null,

    @SerializedName("vehicle_model_name")
    val vehicleModelName: String? = null,

    @SerializedName("vehicle_year_name")
    val vehicleYearName: String? = null,

    @SerializedName("vehicle_color")
    val vehicleColor: String? = null,

    @SerializedName("vehicle_color_name")
    val vehicleColorName: String? = null,

    @SerializedName("vehicle_image")
    val vehicleImage: String? = null
)

// ==================== Driver Wallet ====================

data class DriverWalletData(
    @SerializedName("balance")
    val balance: BalanceDetails?,

    @SerializedName("stripe_balance")
    val stripeBalance: StripeBalanceDetails?,

    @SerializedName("all_transfers")
    val allTransfers: AllTransfersData?
)

data class BalanceDetails(
    @SerializedName("current_balance")
    val currentBalance: String?,

    @SerializedName("currency")
    val currency: String?,

    @SerializedName("currency_symbol")
    val currencySymbol: String?,

    @SerializedName("payout_schedule")
    val payoutSchedule: String?,

    @SerializedName("payout_schedule_description")
    val payoutScheduleDescription: String?,

    @SerializedName("account_status")
    val accountStatus: String?,

    @SerializedName("account_status_description")
    val accountStatusDescription: String?,

    @SerializedName("total_paid_amount")
    val totalPaidAmount: String?,

    @SerializedName("total_paid_amount_formatted")
    val totalPaidAmountFormatted: String?
)

data class StripeBalanceDetails(
    @SerializedName("available_balance")
    val availableBalance: String?,

    @SerializedName("available_balance_description")
    val availableBalanceDescription: String?,

    @SerializedName("pending_balance")
    val pendingBalance: String?,

    @SerializedName("pending_balance_description")
    val pendingBalanceDescription: String?,

    @SerializedName("paid_balance")
    val paidBalance: String?,

    @SerializedName("paid_balance_description")
    val paidBalanceDescription: String?,

    @SerializedName("instant_available_balance")
    val instantAvailableBalance: String?,

    @SerializedName("instant_available_balance_description")
    val instantAvailableBalanceDescription: String?,

    @SerializedName("total_balance")
    val totalBalance: String?,

    @SerializedName("total_balance_description")
    val totalBalanceDescription: String?
)

data class AllTransfersData(
    @SerializedName("data")
    val data: List<TransferDetails>?
)

data class TransferDetails(
    @SerializedName("id")
    val id: String?,

    @SerializedName("amount")
    val amount: Double?,

    @SerializedName("currency")
    val currency: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("transfer_group")
    val transferGroup: String?,

    @SerializedName("reservation_id")
    val reservationId: Int?,

    @SerializedName("created_date")
    val createdDate: String?,

    @SerializedName("created_datetime")
    val createdDatetime: String?,

    @SerializedName("source_transaction")
    val sourceTransaction: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("payout_date")
    val payoutDate: String?,

    @SerializedName("payout_id")
    val payoutId: String?,

    @SerializedName("payout_method")
    val payoutMethod: String?,

    @SerializedName("payout_bank_account")
    val payoutBankAccount: String?,

    @SerializedName("application_fee")
    val applicationFee: Int?,

    @SerializedName("application_fee_percentage")
    val applicationFeePercentage: Double?,

    @SerializedName("destination_payment")
    val destinationPayment: String?,

    @SerializedName("livemode")
    val livemode: Boolean?,

    @SerializedName("metadata")
    val metadata: List<Any>?,

    @SerializedName("reversed")
    val reversed: Boolean?,

    @SerializedName("reversal")
    val reversal: Any?,

    @SerializedName("source_transaction_created")
    val sourceTransactionCreated: String?,

    @SerializedName("net_amount")
    val netAmount: Double?,

    @SerializedName("fee_percentage")
    val feePercentage: Int?,

    @SerializedName("days_since_created")
    val daysSinceCreated: Int?,

    @SerializedName("is_recent")
    val isRecent: Boolean?,

    @SerializedName("is_this_month")
    val isThisMonth: Boolean?,

    @SerializedName("is_this_year")
    val isThisYear: Boolean?
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

// ==================== Admin Reservation Rates (Finalize/Edit flows) ====================

/**
 * Matches iOS `ReservationRatesResponse.data` shape:
 * - sub_total / grand_total
 * - min_rate_involved
 * - rateArray (all_inclusive_rates, taxes, amenities, misc)
 */
data class AdminReservationRatesData(
    @SerializedName("sub_total")
    val subTotal: Double? = null,
    @SerializedName("grand_total")
    val grandTotal: Double? = null,
    @SerializedName("min_rate_involved")
    val minRateInvolved: Boolean? = null,
    @SerializedName("rateArray")
    val rateArray: AdminReservationRateArray
)

data class AdminReservationRateArray(
    @SerializedName("all_inclusive_rates")
    val allInclusiveRates: Map<String, AdminReservationRateItem> = emptyMap(),
    @SerializedName("all_inclusive_rates_order")
    val allInclusiveRatesOrder: List<String> = emptyList(),
    @SerializedName("taxes")
    val taxes: Map<String, AdminReservationRateItem> = emptyMap(),
    @SerializedName("taxes_order")
    val taxesOrder: List<String> = emptyList(),
    @SerializedName("amenities")
    val amenities: Map<String, AdminReservationRateItem> = emptyMap(),
    @SerializedName("amenities_order")
    val amenitiesOrder: List<String> = emptyList(),
    @SerializedName("misc")
    val misc: Map<String, AdminReservationRateItem> = emptyMap(),
    @SerializedName("misc_order")
    val miscOrder: List<String> = emptyList()
)

data class AdminReservationRateItem(
    @SerializedName("rate_label")
    val rateLabel: String,
    @SerializedName("baserate")
    val baserate: Double? = null,
    @SerializedName("multiple")
    val multiple: Double? = null,
    @SerializedName("percentage")
    val percentage: Double? = null,
    @SerializedName("amount")
    val amount: Double? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("flat_baserate")
    val flatBaserate: Double? = null
)

// ==================== Booking Rates Vehicle ====================

data class BookingRatesVehicleRequest(
    @SerializedName("vehicle_id")
    val vehicleId: Int,
    
    @SerializedName("return_vehicle_id")
    val returnVehicleId: String = "",
    
    @SerializedName("transfer_type")
    val transferType: String,
    
    @SerializedName("service_type")
    val serviceType: String,
    
    @SerializedName("numberOfVehicles")
    val numberOfVehicles: Int,
    
    @SerializedName("distance")
    val distance: Int,
    
    @SerializedName("return_distance")
    val returnDistance: Int = 0,
    
    @SerializedName("no_of_hours")
    val noOfHours: Int,
    
    @SerializedName("is_master_vehicle")
    val isMasterVehicle: Boolean = false,
    
    @SerializedName("extra_stops")
    val extraStops: List<BookingRatesExtraStopRequest> = emptyList(),
    
    @SerializedName("return_extra_stops")
    val returnExtraStops: List<BookingRatesExtraStopRequest> = emptyList(),
    
    @SerializedName("manual_change_aff_veh")
    val manualChangeAffVeh: Boolean = false,
    
    @SerializedName("pickup_time")
    val pickupTime: String,
    
    @SerializedName("return_pickup_time")
    val returnPickupTime: String = "12:00 pm",
    
    @SerializedName("affiliate_type")
    val affiliateType: String = "affiliate",
    
    @SerializedName("return_affiliate_type")
    val returnAffiliateType: String = "affiliate"
)

data class BookingRatesExtraStopRequest(
    @SerializedName("address")
    val address: String,
    
    @SerializedName("latitude")
    val latitude: String,
    
    @SerializedName("longitude")
    val longitude: String,
    
    @SerializedName("booking_instructions")
    val bookingInstructions: String = "",
    
    @SerializedName("rate")
    val rate: String
)

data class BookingRatesVehicleData(
    @SerializedName("sub_total")
    val subTotal: Double? = null,
    
    @SerializedName("grand_total")
    val grandTotal: Double? = null,
    
    @SerializedName("min_rate_involved")
    val minRateInvolved: Boolean? = null,
    
    @SerializedName("rateArray")
    val rateArray: BookingRatesRateArray? = null
)

data class BookingRatesRateArray(
    @SerializedName("all_inclusive_rates")
    val allInclusiveRates: Map<String, BookingRatesRateItem>? = null,
    
    @SerializedName("taxes")
    val taxes: Map<String, BookingRatesRateItem>? = null,
    
    @SerializedName("amenities")
    val amenities: Map<String, BookingRatesRateItem>? = null,
    
    @SerializedName("misc")
    val misc: Map<String, BookingRatesRateItem>? = null
)

data class BookingRatesRateItem(
    @SerializedName("rate_label")
    val rateLabel: String? = null,
    
    @SerializedName("baserate")
    val baserate: Double? = null,
    
    @SerializedName("flat_baserate")
    val flatBaserate: Double? = null,
    
    @SerializedName("multiple")
    val multiple: Double? = null,
    
    @SerializedName("percentage")
    val percentage: Double? = null,
    
    @SerializedName("amount")
    val amount: Double? = null,
    
    @SerializedName("type")
    val type: String? = null
)

// ==================== Finalize Rate Edit ====================

data class FinalizeRatesRequest(
    @SerializedName("reservation_id")
    val reservationId: String,
    @SerializedName("rateArray")
    val rateArray: FinalizeRateArray,
    @SerializedName("sub_total")
    val subTotal: Double,
    @SerializedName("grand_total")
    val grandTotal: Double,
    @SerializedName("number_of_hours")
    val numberOfHours: Int,
    @SerializedName("shareArray")
    val shareArray: FinalizeShareArray,
    @SerializedName("waiting_time_in_mins")
    val waitingTimeInMins: Int
)

data class FinalizeRateArray(
    @SerializedName("all_inclusive_rates")
    val allInclusiveRates: Map<String, FinalizeRateItem>,
    @SerializedName("taxes")
    val taxes: Map<String, FinalizeRateItem>,
    @SerializedName("amenities")
    val amenities: Map<String, FinalizeRateItem>,
    @SerializedName("misc")
    val misc: Map<String, FinalizeRateItem>
)

data class FinalizeRateItem(
    @SerializedName("rate_label")
    val rateLabel: String,
    @SerializedName("baserate")
    val baserate: Double,
    @SerializedName("multiple")
    val multiple: Double? = null,
    @SerializedName("percentage")
    val percentage: Double? = null,
    @SerializedName("amount")
    val amount: Double? = null,
    @SerializedName("type")
    val type: String? = null
)

data class FinalizeShareArray(
    @SerializedName("baseRate")
    val baseRate: Double,
    @SerializedName("grandTotal")
    val grandTotal: Double,
    @SerializedName("stripeFee")
    val stripeFee: Double,
    @SerializedName("adminShare")
    val adminShare: Double,
    @SerializedName("deducted_admin_share")
    val deductedAdminShare: Double,
    @SerializedName("affiliateShare")
    val affiliateShare: Double,
    @SerializedName("travelAgentShare")
    val travelAgentShare: Double? = null,
    @SerializedName("farmoutShare")
    val farmoutShare: Double? = null
)

data class FinalizeRateEditData(
    @SerializedName("success")
    val success: Boolean?,
    
    @SerializedName("message")
    val message: String?
)

// ==================== Payment Processing ====================

data class PaymentProcessingRequest(
    @SerializedName("reservation_id")
    val reservationId: String,
    @SerializedName("grand_total")
    val grandTotal: Double,
    @SerializedName("paymentMethod")
    val paymentMethod: String
)

data class PaymentProcessingData(
    @SerializedName("success")
    val success: Boolean?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("transaction_id")
    val transactionId: String?,

    // iOS uses order_id from API response; keep optional for navigation
    @SerializedName("order_id")
    val orderId: Int? = null
)

// Credit card payment
data class CreditCardPaymentRequest(
    @SerializedName("isExistingCard")
    val isExistingCard: Boolean,
    @SerializedName("paymentMethod")
    val paymentMethod: String,
    @SerializedName("CreditCardsDetail")
    val creditCardsDetail: CreditCardDetail,
    @SerializedName("reservation_id")
    val reservationId: String,
    @SerializedName("grand_total")
    val grandTotal: Double
)

data class CreditCardDetail(
    @SerializedName("cardID")
    val cardId: String
)

data class CreditCardData(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("ID")
    val ID: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("brand")
    val brand: String? = null,
    @SerializedName("exp_month")
    val expMonth: Int? = null,
    @SerializedName("exp_year")
    val expYear: Int? = null,
    @SerializedName("last4")
    val last4: String? = null,
    @SerializedName("card_type")
    val cardType: String? = null,
    @SerializedName("cc_prority")
    val ccPriority: String? = null
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

// ==================== Admin Booking Preview (Accept/Reject + Preview screen) ====================

data class AdminBookingPreviewExtraStop(
    @SerializedName("rate")
    val rate: String? = null,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("latitude")
    val latitude: String? = null,
    @SerializedName("longitude")
    val longitude: String? = null,
    @SerializedName("booking_instructions")
    val bookingInstructions: String? = null
)

data class AdminRatesPreview(
    @SerializedName("baseRate")
    val baseRate: String? = null,
    @SerializedName("stripeFee")
    val stripeFee: String? = null,
    @SerializedName("adminShare")
    val adminShare: String? = null,
    @SerializedName("grandTotal")
    val grandTotal: String? = null,
    @SerializedName("affiliateShare")
    val affiliateShare: String? = null,
    @SerializedName("travelAgentShare")
    val travelAgentShare: String? = null,
    @SerializedName("deducted_admin_share")
    val deductedAdminShare: String? = null
)

/**
 * Matches iOS `BookingPreviewData` for /api/admin/get-booking-preview/{bookingId}.
 * We intentionally include the subset we need for the UI; Gson will ignore extra fields.
 */
data class AdminBookingPreviewData(
    @SerializedName("reservation_id")
    val reservationId: Int? = null,
    @SerializedName("booking_id")
    val bookingId: Int? = null,
    @SerializedName("transfer_type")
    val transferType: String? = null,
    @SerializedName("service_type")
    val serviceType: String? = null,
    @SerializedName("account_type")
    val accountType: String? = null,
    @SerializedName("created_by")
    val createdBy: Int? = null,
    @SerializedName("reservation_type")
    val reservationType: String? = null,
    @SerializedName("pickup_address")
    val pickupAddress: String? = null,
    @SerializedName("pickup_airport")
    val pickupAirport: String? = null,
    @SerializedName("pickup_airport_name")
    val pickupAirportName: String? = null,
    @SerializedName("pickup_airline")
    val pickupAirline: String? = null,
    @SerializedName("pickup_airline_name")
    val pickupAirlineName: String? = null,
    @SerializedName("pickup_flight")
    val pickupFlight: String? = null,
    @SerializedName("origin_airport_city")
    val originAirportCity: String? = null,
    @SerializedName("departing_airport_city")
    val departingAirportCity: String? = null,
    @SerializedName("dropoff_address")
    val dropoffAddress: String? = null,
    @SerializedName("dropoff_airport")
    val dropoffAirport: String? = null,
    @SerializedName("dropoff_airport_name")
    val dropoffAirportName: String? = null,
    @SerializedName("dropoff_airline")
    val dropoffAirline: String? = null,
    @SerializedName("dropoff_airline_name")
    val dropoffAirlineName: String? = null,
    @SerializedName("dropoff_flight")
    val dropoffFlight: String? = null,
    @SerializedName("pickup_date")
    val pickupDate: String? = null,
    @SerializedName("pickup_time")
    val pickupTime: String? = null,
    @SerializedName("pickup_latitude")
    val pickupLatitude: String? = null,
    @SerializedName("pickup_longitude")
    val pickupLongitude: String? = null,
    @SerializedName("dropoff_latitude")
    val dropoffLatitude: String? = null,
    @SerializedName("dropoff_longitude")
    val dropoffLongitude: String? = null,
    @SerializedName("pickup_airport_latitude")
    val pickupAirportLatitude: Double? = null,
    @SerializedName("pickup_airport_longitude")
    val pickupAirportLongitude: Double? = null,
    @SerializedName("dropoff_airport_latitude")
    val dropoffAirportLatitude: Double? = null,
    @SerializedName("dropoff_airport_longitude")
    val dropoffAirportLongitude: Double? = null,
    @SerializedName("extra_stops")
    val extraStops: List<AdminBookingPreviewExtraStop>? = null,
    @SerializedName("cruise_port")
    val cruisePort: String? = null,
    @SerializedName("cruise_name")
    val cruiseName: String? = null,
    @SerializedName("cruise_time")
    val cruiseTime: String? = null,
    @SerializedName("booking_instructions")
    val bookingInstructions: String? = null,
    @SerializedName("meet_greet_choice_name")
    val meetGreetChoiceName: String? = null,
    @SerializedName("vehicle_type_name")
    val vehicleTypeName: String? = null,
    @SerializedName("vehicle_id")
    val vehicleId: Int? = null,
    @SerializedName("cancellation_hours")
    val cancellationHours: Int? = null,
    @SerializedName("total_passengers")
    val totalPassengers: Int? = null,
    @SerializedName("luggage_count")
    val luggageCount: Int? = null,
    @SerializedName("number_of_hours")
    val numberOfHours: Int? = null,
    @SerializedName("number_of_vehicles")
    val numberOfVehicles: Int? = null,
    @SerializedName("passenger_name")
    val passengerName: String? = null,
    @SerializedName("passenger_email")
    val passengerEmail: String? = null,
    @SerializedName("passenger_cell_isd")
    val passengerCellIsd: String? = null,
    @SerializedName("passenger_cell")
    val passengerCell: String? = null,
    @SerializedName("passenger_cell_country")
    val passengerCellCountry: String? = null,
    @SerializedName("acc_id")
    val accId: Int? = null,
    @SerializedName("affiliate_id")
    val affiliateId: Int? = null,
    @SerializedName("travel_client_id")
    val travelClientId: Int? = null,
    @SerializedName("driver_id")
    val driverId: Int? = null,
    @SerializedName("driver_first_name")
    val driverFirstName: String? = null,
    @SerializedName("driver_last_name")
    val driverLastName: String? = null,
    @SerializedName("driver_cell_number")
    val driverCellNumber: String? = null,
    @SerializedName("driver_cell_isd")
    val driverCellIsd: String? = null,
    @SerializedName("driver_cell_country")
    val driverCellCountry: String? = null,
    @SerializedName("driver_email")
    val driverEmail: String? = null,
    @SerializedName("driver_gender")
    val driverGender: String? = null,
    @SerializedName("distance")
    val distance: String? = null,
    @SerializedName("duration")
    val duration: String? = null,
    @SerializedName("payment_status")
    val paymentStatus: String? = null,
    @SerializedName("booking_status")
    val bookingStatus: String? = null,
    @SerializedName("grand_total")
    val grandTotal: Double? = null,
    @SerializedName("currency_symbol")
    val currencySymbol: String? = null,
    @SerializedName("rates_preview")
    val ratesPreview: AdminRatesPreview? = null
)

// ==================== Edit Reservation ====================

// Supporting models matching iOS structure
data class AirportOption(
    @SerializedName("id")
    val id: Int,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("lat")
    val lat: Double? = null,
    @SerializedName("long")
    val long: Double? = null,
    @SerializedName("formatted_name")
    val formattedName: String? = null
)

data class AirlineOption(
    @SerializedName("id")
    val id: Int,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("formatted_name")
    val formattedName: String? = null
)

data class RateItem(
    @SerializedName("rate_label")
    val rateLabel: String,
    @SerializedName("baserate")
    val baserate: Double,
    @SerializedName("multiple")
    val multiple: Double,
    @SerializedName("percentage")
    val percentage: Double,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("type")
    val type: String? = null
)

data class RateArray(
    @SerializedName("all_inclusive_rates")
    val allInclusiveRates: Map<String, RateItem> = emptyMap(),
    @SerializedName("direct_taxes")
    val directTaxes: Map<String, RateItem> = emptyMap(),
    @SerializedName("taxes")
    val taxes: Map<String, RateItem> = emptyMap(),
    @SerializedName("amenities")
    val amenities: Map<String, RateItem> = emptyMap(),
    @SerializedName("misc")
    val misc: Map<String, RateItem> = emptyMap()
)

data class SharesArray(
    @SerializedName("baseRate")
    val baseRate: Double,
    @SerializedName("grandTotal")
    val grandTotal: Double,
    @SerializedName("stripeFee")
    val stripeFee: Double,
    @SerializedName("adminShare")
    val adminShare: Double,
    @SerializedName("deducted_admin_share")
    val deductedAdminShare: Double,
    @SerializedName("affiliateShare")
    val affiliateShare: Double,
    @SerializedName("travelAgentShare")
    val travelAgentShare: Double? = null,
    @SerializedName("farmoutShare")
    val farmoutShare: Double? = null
)

// Complete EditReservationRequest matching iOS exactly
data class EditReservationRequest(
    // Basic info
    @SerializedName("service_type")
    val serviceType: String,
    @SerializedName("transfer_type")
    val transferType: String,
    @SerializedName("return_transfer_type")
    val returnTransferType: String,
    @SerializedName("number_of_hours")
    val numberOfHours: Int,
    @SerializedName("acc_id")
    val accId: Int,
    @SerializedName("account_type")
    val accountType: String,
    @SerializedName("travel_client_id")
    val travelClientId: Int,
    
    // Passenger info
    @SerializedName("passenger_name")
    val passengerName: String,
    @SerializedName("passenger_email")
    val passengerEmail: String,
    @SerializedName("passenger_cell")
    val passengerCell: String,
    @SerializedName("passenger_cell_isd")
    val passengerCellIsd: String,
    @SerializedName("passenger_cell_country")
    val passengerCellCountry: String,
    @SerializedName("total_passengers")
    val totalPassengers: Int,
    @SerializedName("luggage_count")
    val luggageCount: Int,
    
    // Booking instructions
    @SerializedName("booking_instructions")
    val bookingInstructions: String,
    @SerializedName("return_booking_instructions")
    val returnBookingInstructions: String,
    
    // Affiliate info
    @SerializedName("affiliate_type")
    val affiliateType: String,
    @SerializedName("affiliate_id")
    val affiliateId: Int,
    @SerializedName("return_affiliate_type")
    val returnAffiliateType: String,
    @SerializedName("return_affiliate_id")
    val returnAffiliateId: String,
    @SerializedName("loose_affiliate_id")
    val looseAffiliateId: String,
    @SerializedName("is_old_loose_affiliate")
    val isOldLooseAffiliate: Boolean,
    @SerializedName("return_loose_affiliate_id")
    val returnLooseAffiliateId: String,
    @SerializedName("return_is_old_loose_affiliate")
    val returnIsOldLooseAffiliate: Boolean,
    @SerializedName("cancellation_hours")
    val cancellationHours: String,
    
    // Driver info
    @SerializedName("driver_id")
    val driverId: Int,
    @SerializedName("driver_name")
    val driverName: String,
    @SerializedName("driver_gender")
    val driverGender: String,
    @SerializedName("driver_cell")
    val driverCell: String,
    @SerializedName("driver_cell_isd")
    val driverCellIsd: String,
    @SerializedName("driver_cell_country")
    val driverCellCountry: String,
    @SerializedName("driver_email")
    val driverEmail: String,
    
    // Return vehicle info
    @SerializedName("return_vehicle_type")
    val returnVehicleType: String,
    @SerializedName("return_vehicle_type_name")
    val returnVehicleTypeName: String,
    @SerializedName("return_vehicle_id")
    val returnVehicleId: String,
    @SerializedName("return_vehicle_make")
    val returnVehicleMake: String,
    @SerializedName("return_vehicle_make_name")
    val returnVehicleMakeName: String,
    @SerializedName("return_vehicle_model")
    val returnVehicleModel: String,
    @SerializedName("return_vehicle_model_name")
    val returnVehicleModelName: String,
    @SerializedName("return_vehicle_year")
    val returnVehicleYear: String,
    @SerializedName("return_vehicle_year_name")
    val returnVehicleYearName: String,
    @SerializedName("return_vehicle_color")
    val returnVehicleColor: String,
    @SerializedName("return_vehicle_color_name")
    val returnVehicleColorName: String,
    @SerializedName("return_vehicle_license_plate")
    val returnVehicleLicensePlate: String,
    @SerializedName("return_vehicle_seats")
    val returnVehicleSeats: String,
    
    // Return driver info
    @SerializedName("return_driver_id")
    val returnDriverId: String,
    @SerializedName("return_driver_name")
    val returnDriverName: String,
    @SerializedName("return_driver_gender")
    val returnDriverGender: String,
    @SerializedName("return_driver_cell")
    val returnDriverCell: String,
    @SerializedName("return_driver_cell_isd")
    val returnDriverCellIsd: String,
    @SerializedName("return_driver_cell_country")
    val returnDriverCellCountry: String,
    @SerializedName("return_driver_email")
    val returnDriverEmail: String,
    @SerializedName("driver_phone_type")
    val driverPhoneType: String,
    @SerializedName("return_driver_phone_type")
    val returnDriverPhoneType: String,
    @SerializedName("driver_image_id")
    val driverImageId: String,
    @SerializedName("vehicle_image_id")
    val vehicleImageId: String,
    
    // Meet & greet
    @SerializedName("meet_greet_choices")
    val meetGreetChoices: Int,
    @SerializedName("meet_greet_choices_name")
    val meetGreetChoicesName: String,
    @SerializedName("number_of_vehicles")
    val numberOfVehicles: Int,
    @SerializedName("pickup_date")
    val pickupDate: String,
    @SerializedName("pickup_time")
    val pickupTime: String,
    @SerializedName("extra_stops")
    val extraStops: List<EditReservationExtraStopRequest>,
    
    // Pickup location
    @SerializedName("pickup")
    val pickup: String,
    @SerializedName("pickup_latitude")
    val pickupLatitude: String,
    @SerializedName("pickup_longitude")
    val pickupLongitude: String,
    @SerializedName("pickup_airport_option")
    val pickupAirportOption: AirportOption? = null,
    @SerializedName("pickup_airport")
    val pickupAirport: Int? = null,
    @SerializedName("pickup_airport_name")
    val pickupAirportName: String? = null,
    @SerializedName("pickup_airport_latitude")
    val pickupAirportLatitude: String? = null,
    @SerializedName("pickup_airport_longitude")
    val pickupAirportLongitude: String? = null,
    @SerializedName("pickup_airline_option")
    val pickupAirlineOption: AirlineOption? = null,
    @SerializedName("pickup_airline")
    val pickupAirline: Int? = null,
    @SerializedName("pickup_airline_name")
    val pickupAirlineName: String? = null,
    @SerializedName("pickup_flight")
    val pickupFlight: String? = null,
    @SerializedName("origin_airport_city")
    val originAirportCity: String? = null,
    @SerializedName("departing_airport_city")
    val departingAirportCity: String? = null,
    
    // Cruise info
    @SerializedName("cruise_port")
    val cruisePort: String,
    @SerializedName("cruise_name")
    val cruiseName: String,
    @SerializedName("cruise_time")
    val cruiseTime: String,
    
    // Dropoff location
    @SerializedName("dropoff")
    val dropoff: String,
    @SerializedName("dropoff_latitude")
    val dropoffLatitude: String,
    @SerializedName("dropoff_longitude")
    val dropoffLongitude: String,
    @SerializedName("dropoff_airport_option")
    val dropoffAirportOption: AirportOption? = null,
    @SerializedName("dropoff_airport")
    val dropoffAirport: Int? = null,
    @SerializedName("dropoff_airport_name")
    val dropoffAirportName: String? = null,
    @SerializedName("dropoff_airport_latitude")
    val dropoffAirportLatitude: String? = null,
    @SerializedName("dropoff_airport_longitude")
    val dropoffAirportLongitude: String? = null,
    @SerializedName("dropoff_airline_option")
    val dropoffAirlineOption: AirlineOption? = null,
    @SerializedName("dropoff_airline")
    val dropoffAirline: Int? = null,
    @SerializedName("dropoff_airline_name")
    val dropoffAirlineName: String? = null,
    @SerializedName("dropoff_flight")
    val dropoffFlight: String? = null,
    
    // Return trip info
    @SerializedName("return_meet_greet_choices")
    val returnMeetGreetChoices: Int,
    @SerializedName("return_meet_greet_choices_name")
    val returnMeetGreetChoicesName: String,
    @SerializedName("return_pickup_date")
    val returnPickupDate: String,
    @SerializedName("return_pickup_time")
    val returnPickupTime: String,
    @SerializedName("return_extra_stops")
    val returnExtraStops: List<EditReservationExtraStopRequest>,
    @SerializedName("return_pickup")
    val returnPickup: String,
    @SerializedName("return_pickup_latitude")
    val returnPickupLatitude: String,
    @SerializedName("return_pickup_longitude")
    val returnPickupLongitude: String,
    @SerializedName("return_pickup_airport_option")
    val returnPickupAirportOption: AirportOption? = null,
    @SerializedName("return_pickup_airport")
    val returnPickupAirport: Int? = null,
    @SerializedName("return_pickup_airport_name")
    val returnPickupAirportName: String? = null,
    @SerializedName("return_pickup_airport_latitude")
    val returnPickupAirportLatitude: String? = null,
    @SerializedName("return_pickup_airport_longitude")
    val returnPickupAirportLongitude: String? = null,
    @SerializedName("return_pickup_airline_option")
    val returnPickupAirlineOption: AirlineOption? = null,
    @SerializedName("return_pickup_airline")
    val returnPickupAirline: Int? = null,
    @SerializedName("return_pickup_airline_name")
    val returnPickupAirlineName: String? = null,
    @SerializedName("return_pickup_flight")
    val returnPickupFlight: String,
    @SerializedName("return_cruise_port")
    val returnCruisePort: String,
    @SerializedName("return_cruise_name")
    val returnCruiseName: String,
    @SerializedName("return_cruise_time")
    val returnCruiseTime: String,
    @SerializedName("return_dropoff")
    val returnDropoff: String,
    @SerializedName("return_dropoff_latitude")
    val returnDropoffLatitude: String,
    @SerializedName("return_dropoff_longitude")
    val returnDropoffLongitude: String,
    @SerializedName("return_dropoff_airport_option")
    val returnDropoffAirportOption: AirportOption? = null,
    @SerializedName("return_dropoff_airport")
    val returnDropoffAirport: Int? = null,
    @SerializedName("return_dropoff_airport_name")
    val returnDropoffAirportName: String? = null,
    @SerializedName("return_dropoff_airport_latitude")
    val returnDropoffAirportLatitude: String? = null,
    @SerializedName("return_dropoff_airport_longitude")
    val returnDropoffAirportLongitude: String? = null,
    @SerializedName("return_dropoff_airline_option")
    val returnDropoffAirlineOption: AirlineOption? = null,
    @SerializedName("return_dropoff_airline")
    val returnDropoffAirline: Int? = null,
    @SerializedName("return_dropoff_airline_name")
    val returnDropoffAirlineName: String? = null,
    @SerializedName("return_dropoff_flight")
    val returnDropoffFlight: String? = null,
    
    // Journey info
    @SerializedName("driver_languages")
    val driverLanguages: List<Int>,
    @SerializedName("driver_dresses")
    val driverDresses: List<String>,
    @SerializedName("amenities")
    val amenities: List<String>,
    @SerializedName("chargedAmenities")
    val chargedAmenities: List<String>,
    @SerializedName("journeyDistance")
    val journeyDistance: Int,
    @SerializedName("journeyTime")
    val journeyTime: Int,
    @SerializedName("returnJourneyDistance")
    val returnJourneyDistance: String,
    @SerializedName("returnJourneyTime")
    val returnJourneyTime: String,
    
    // Reservation info
    @SerializedName("reservation_id")
    val reservationId: Int,
    @SerializedName("updateType")
    val updateType: String,
    @SerializedName("susbcriber_name")
    val susbcriberName: String,
    @SerializedName("return_susbcriber_name")
    val returnSusbcriberName: String,
    @SerializedName("booking_created_from")
    val bookingCreatedFrom: String,
    @SerializedName("proceed")
    val proceed: Boolean,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("rateArray")
    val rateArray: RateArray,
    @SerializedName("grand_total")
    val grandTotal: Double,
    @SerializedName("sub_total")
    val subTotal: Double,
    @SerializedName("min_rate_involved")
    val minRateInvolved: Boolean,
    @SerializedName("shares_array")
    val sharesArray: SharesArray,
    @SerializedName("change_individual_data")
    val changeIndividualData: Boolean,
    
    // Vehicle info
    @SerializedName("vehicle_color")
    val vehicleColor: String,
    @SerializedName("vehicle_color_name")
    val vehicleColorName: String,
    @SerializedName("vehicle_id")
    val vehicleId: String,
    @SerializedName("vehicle_license_plate")
    val vehicleLicensePlate: String,
    @SerializedName("vehicle_make")
    val vehicleMake: String,
    @SerializedName("vehicle_make_name")
    val vehicleMakeName: String,
    @SerializedName("vehicle_model")
    val vehicleModel: String,
    @SerializedName("vehicle_model_name")
    val vehicleModelName: String,
    @SerializedName("vehicle_seats")
    val vehicleSeats: String,
    @SerializedName("vehicle_type")
    val vehicleType: String,
    @SerializedName("vehicle_type_name")
    val vehicleTypeName: String,
    @SerializedName("vehicle_year")
    val vehicleYear: String,
    @SerializedName("vehicle_year_name")
    val vehicleYearName: String
)

data class EditReservationExtraStopRequest(
    @SerializedName("rate")
    val rate: String? = null,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("latitude")
    val latitude: String? = null,
    @SerializedName("longitude")
    val longitude: String? = null,
    @SerializedName("booking_instructions")
    val bookingInstructions: String? = null
)

data class EditReservationData(
    @SerializedName("success")
    val success: Boolean?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("booking")
    val booking: DriverBooking?
)

// ==================== Booking Status (Accept/Reject) ====================

/**
 * Minimal payload used by accept/reject endpoints.
 * iOS primarily uses the message + success; data is optional and may vary.
 */
data class BookingStatusData(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("booking_id")
    val bookingId: Int? = null
)

