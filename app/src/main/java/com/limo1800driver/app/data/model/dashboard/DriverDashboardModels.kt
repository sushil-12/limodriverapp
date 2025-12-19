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

    // --- Optional: richer edit payload (matches iOS more closely; backend tolerates missing fields) ---
    @SerializedName("service_type")
    val serviceType: String? = null,

    @SerializedName("transfer_type")
    val transferType: String? = null,

    @SerializedName("number_of_hours")
    val numberOfHours: Int? = null,

    @SerializedName("number_of_vehicles")
    val numberOfVehicles: Int? = null,

    @SerializedName("meet_greet_choices_name")
    val meetGreetChoiceName: String? = null,

    @SerializedName("booking_instructions")
    val bookingInstructions: String? = null,

    // --- Passenger fields (iOS edit flow includes these) ---
    @SerializedName("passenger_name")
    val passengerName: String? = null,

    @SerializedName("passenger_email")
    val passengerEmail: String? = null,

    @SerializedName("passenger_cell_isd")
    val passengerCellIsd: String? = null,

    @SerializedName("passenger_cell")
    val passengerCell: String? = null,

    @SerializedName("pickup_airport_name")
    val pickupAirportName: String? = null,

    @SerializedName("pickup_airline_name")
    val pickupAirlineName: String? = null,

    @SerializedName("pickup_flight")
    val pickupFlight: String? = null,

    @SerializedName("origin_airport_city")
    val originAirportCity: String? = null,

    @SerializedName("cruise_port")
    val cruisePort: String? = null,

    @SerializedName("cruise_name")
    val cruiseName: String? = null,

    @SerializedName("cruise_time")
    val cruiseTime: String? = null,

    @SerializedName("dropoff_airport_name")
    val dropoffAirportName: String? = null,

    @SerializedName("dropoff_airline_name")
    val dropoffAirlineName: String? = null,

    @SerializedName("dropoff_flight")
    val dropoffFlight: String? = null,

    @SerializedName("departing_airport_city")
    val departingAirportCity: String? = null,

    @SerializedName("extra_stops")
    val extraStops: List<EditReservationExtraStopRequest>? = null,
    
    @SerializedName("rates")
    val rates: Map<String, Any>?
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

