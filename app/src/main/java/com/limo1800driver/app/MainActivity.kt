package com.limo1800driver.app

import com.limo1800driver.app.ui.screens.registration.DriverLicenseFormScreen
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.limo1800driver.app.ui.navigation.NavRoutes
import com.limo1800driver.app.ui.screens.auth.OtpScreen
import com.limo1800driver.app.ui.screens.auth.PhoneEntryScreen
import com.limo1800driver.app.ui.screens.onboarding.OnboardingScreen
import com.limo1800driver.app.ui.screens.registration.BasicInfoScreen
import com.limo1800driver.app.ui.screens.registration.CompanyInfoScreen
import com.limo1800driver.app.ui.screens.registration.CompanyDocumentsScreen
import com.limo1800driver.app.ui.screens.registration.PrivacyTermsScreen
import com.limo1800driver.app.ui.screens.registration.BankDetailsScreen
import com.limo1800driver.app.ui.screens.registration.ProfilePictureScreen
import com.limo1800driver.app.ui.screens.registration.VehicleSelectionScreen
import com.limo1800driver.app.ui.components.WebViewScreen
import com.limo1800driver.app.ui.screens.registration.VehicleDetailsScreen
import com.limo1800driver.app.ui.screens.registration.VehicleAmenitiesScreen
import com.limo1800driver.app.ui.screens.registration.VehicleDetailsImageUploadScreen
import com.limo1800driver.app.ui.screens.registration.VehicleInsuranceScreen
import com.limo1800driver.app.ui.screens.registration.VehicleRatesScreen
import com.limo1800driver.app.ui.screens.registration.UserProfileDetailsScreen
import com.limo1800driver.app.ui.screens.registration.VehicleDetailsCoordinatorFromAccountSettings
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.data.storage.TokenManager
import com.limo1800driver.app.ui.components.EmailVerificationAlert
import com.limo1800driver.app.ui.screens.registration.VehicleDetailsStepScreen
import com.limo1800driver.app.ui.screens.dashboard.MyActivityScreen
import com.limo1800driver.app.ui.screens.dashboard.WalletScreen
import com.limo1800driver.app.ui.screens.dashboard.PreArrangedRidesScreen
import com.limo1800driver.app.ui.screens.dashboard.NotificationsScreen
import com.limo1800driver.app.ui.screens.dashboard.AccountSettingsScreen
import com.limo1800driver.app.ui.screens.dashboard.ProfileViewScreen
import com.limo1800driver.app.ui.screens.booking.BookingPreviewScreen
import com.limo1800driver.app.ui.screens.booking.EditBookingDetailsAndRatesScreen
import com.limo1800driver.app.ui.screens.booking.FinalizeBookingScreen
import com.limo1800driver.app.ui.screens.booking.FinalizeRatesScreen
import com.limo1800driver.app.ui.screens.chat.ChatScreen
import com.limo1800driver.app.ui.screens.ride.RideInProgressScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.limo1800driver.app.ui.theme.LimoDriverAppTheme
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hide action bar completely
        actionBar?.hide()

        // Set status bar and navigation bar colors to match app background
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Additional window flags to ensure no title bar
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                0, // Light status bar content
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }

        // Android 13+ notification runtime permission
        val requestNotificationsPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* no-op */ }

        setContent {
            LimoDriverAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestNotificationsPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    DriverAppNavigation(tokenManager)
                }
            }
        }
    }
}

@Composable
fun DriverAppNavigation(
    tokenManager: TokenManager
) {
    val navController = rememberNavController()
    val registrationNavigationState = remember { RegistrationNavigationState() }

    // Determine initial destination based on authentication state
    val initialDestination = remember {
        val isAuthenticated = tokenManager.isAuthenticated()
        val hasSeenOnboarding = tokenManager.hasSeenOnboarding()

        when {
            isAuthenticated -> {
                val isProfileCompleted = tokenManager.isProfileCompleted()
                if (isProfileCompleted) {
                    NavRoutes.Dashboard
                } else {
                    val registrationState = tokenManager.getDriverRegistrationState()
                    val nextStep = registrationState?.nextStep ?: tokenManager.getNextStep()
                    when {
                        nextStep == null || nextStep == "dashboard" -> NavRoutes.Dashboard
                        else -> registrationNavigationState.getRouteForStep(nextStep)
                    }
                }
            }
            hasSeenOnboarding -> NavRoutes.PhoneEntry
            else -> NavRoutes.Onboarding
        }
    }

    fun navigateToDashboardHome(openDrawer: Boolean = false) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute == NavRoutes.Dashboard) {
            if (openDrawer) {
                runCatching {
                    navController.currentBackStackEntry?.savedStateHandle?.set("openDrawer", true)
                }
            }
            return
        }

        if (openDrawer) {
            runCatching {
                navController.getBackStackEntry(NavRoutes.Dashboard)
                    .savedStateHandle["openDrawer"] = true
            }
        }

        val popped = navController.popBackStack(NavRoutes.Dashboard, inclusive = false)
        if (!popped) {
            navController.navigate(NavRoutes.Dashboard) {
                launchSingleTop = true
            }
            if (openDrawer) {
                runCatching {
                    navController.currentBackStackEntry?.savedStateHandle?.set("openDrawer", true)
                }
            }
        }
    }

    fun navigateToRegistrationStep(
        nextStep: String?,
        clearBackStack: Boolean = false,
        forceDirect: Boolean = false
    ) {
        val isProfileCompleted = tokenManager.isProfileCompleted()
        if (isProfileCompleted) {
            navController.navigate(NavRoutes.Dashboard) {
                launchSingleTop = true
                if (clearBackStack) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            return
        }

        val isComplete = registrationNavigationState.isRegistrationComplete(nextStep)
        val profileSteps = setOf("driving_license", "bank_details", "profile_picture")
        val targetRoute = when {
            isComplete -> NavRoutes.Dashboard
            !forceDirect && nextStep in profileSteps -> NavRoutes.UserProfileDetails
            else -> registrationNavigationState.getRouteForStep(nextStep)
        }

        navController.navigate(targetRoute) {
            launchSingleTop = true
            if (clearBackStack) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    // --- LAYOUT FIX: WRAP EVERYTHING IN A BOX ---
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Navigation Host (Background)
        NavHost(
            navController = navController,
            startDestination = initialDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(NavRoutes.Onboarding) {
                OnboardingScreen(
                    onContinue = {
                        tokenManager.saveOnboardingSeen(true)
                        navController.navigate(NavRoutes.PhoneEntry) {
                            popUpTo(NavRoutes.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.PhoneEntry) {
                PhoneEntryScreen(
                    onNext = { tempUserId, phoneNumber ->
                        navController.navigate("${NavRoutes.Otp}/$tempUserId/$phoneNumber")
                    },
                    onBack = null,
                    onNavigateToWebView = { url, title ->
                        navController.navigate("${NavRoutes.WebView}?url=${Uri.encode(url)}&title=${Uri.encode(title)}")
                    }
                )
            }

            composable("${NavRoutes.Otp}/{tempUserId}/{phoneNumber}") { backStackEntry ->
                val tempUserId = backStackEntry.arguments?.getString("tempUserId") ?: ""
                val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""

                OtpScreen(
                    tempUserId = tempUserId,
                    phoneNumber = phoneNumber,
                    onNext = { nextAction ->
                        navigateToRegistrationStep(nextAction, true, forceDirect = false)
                    },
                    onBack = {
                        navController.navigate(NavRoutes.PhoneEntry) {
                            popUpTo(NavRoutes.PhoneEntry) { inclusive = false }
                        }
                    }
                )
            }

            composable(NavRoutes.BasicInfo) {
                BasicInfoScreen(
                    onNext = { nextStep -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                    onBack = null
                )
            }

            composable(NavRoutes.CompanyInfo) {
                BackHandler {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        navController.navigate(NavRoutes.BasicInfo) {
                            launchSingleTop = true
                        }
                    }
                }
                CompanyInfoScreen(
                    onNext = { nextStep -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(NavRoutes.BasicInfo) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(NavRoutes.CompanyDocuments) {
                CompanyDocumentsScreen(
                    onNext = { nextStep -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(NavRoutes.CompanyInfo) {
                                launchSingleTop = true
                                popUpTo(NavRoutes.CompanyDocuments) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(NavRoutes.PrivacyTerms) {
                PrivacyTermsScreen(
                    onNext = { nextStep -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(NavRoutes.CompanyDocuments) {
                                launchSingleTop = true
                                popUpTo(NavRoutes.PrivacyTerms) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToWebView = { url, title ->
                        navController.navigate("${NavRoutes.WebView}?url=${url}&title=${title}")
                    }
                )
            }

            composable(NavRoutes.DrivingLicense) {
                DriverLicenseFormScreen(
                    onNext = { nextStep: String? -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(NavRoutes.UserProfileDetails) { launchSingleTop = true }
                        }
                    }
                )
            }

            composable(NavRoutes.BankDetails) {
                BankDetailsScreen(
                    onNext = { nextStep -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(NavRoutes.UserProfileDetails) { launchSingleTop = true }
                        }
                    }
                )
            }

            composable(NavRoutes.ProfilePicture) {
                ProfilePictureScreen(
                    onNext = { nextStep -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(NavRoutes.UserProfileDetails) { launchSingleTop = true }
                        }
                    }
                )
            }

            composable(NavRoutes.ProfilePictureFromAccountSettings) {
                val profileViewModel: com.limo1800driver.app.ui.viewmodel.DriverProfileViewModel = hiltViewModel()
                ProfilePictureScreen(
                    onNext = { _ ->
                        profileViewModel.refreshDriverProfile()
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.CompanyInfoFromAccountSettings) {
                CompanyInfoScreen(
                    onNext = { _ -> navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    isEditMode = true,
                    onUpdateComplete = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.CompanyDocumentsFromAccountSettings) {
                CompanyDocumentsScreen(
                    onNext = { _ -> navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    isEditMode = true,
                    onUpdateComplete = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.BasicInfoFromAccountSettings) {
                BasicInfoScreen(
                    onNext = { _ -> navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    isEditMode = true
                )
            }

            composable(NavRoutes.VehicleSelection) {
                VehicleSelectionScreen(
                    onNext = { navController.navigate(NavRoutes.VehicleDetailsStep) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.VehicleDetailsStep) {
                VehicleDetailsStepScreen(
                    onNavigateToStep = { route -> navController.navigate(route) },
                    onContinue = { navigateToRegistrationStep(NavRoutes.Dashboard, clearBackStack = true) },
                    onBack = {
                        if (!navController.popBackStack(NavRoutes.VehicleSelection, inclusive = false)) {
                            navController.navigate(NavRoutes.VehicleSelection) {
                                launchSingleTop = true
                                popUpTo(NavRoutes.VehicleSelection) { inclusive = false }
                            }
                        }
                    }
                )
            }

            composable(NavRoutes.VehicleDetailsStepFromAccountSettings) {
                VehicleDetailsStepScreen(
                    onNavigateToStep = { route -> navController.navigate(route) },
                    onContinue = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.VehicleDetailsCoordinatorFromAccountSettings) {
                val profileViewModel: com.limo1800driver.app.ui.viewmodel.DriverProfileViewModel = hiltViewModel()
                VehicleDetailsCoordinatorFromAccountSettings(
                    onClose = { navController.popBackStack() },
                    onProfileUpdated = { profileViewModel.refreshDriverProfile() }
                )
            }

            composable(NavRoutes.VehicleDetails) {
                VehicleDetailsScreen(
                    onNext = { navController.navigate(NavRoutes.VehicleAmenities) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.VehicleAmenities) {
                VehicleAmenitiesScreen(
                    onNext = { navController.navigate(NavRoutes.VehicleDetailsImageUpload) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.VehicleDetailsImageUpload) {
                VehicleDetailsImageUploadScreen(
                    onNext = { navController.navigate(NavRoutes.VehicleDetailsStep) { launchSingleTop = true } },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.VehicleInsurance) {
                VehicleInsuranceScreen(
                    onNext = { nextStep -> navigateToRegistrationStep(nextStep) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.VehicleInsuranceFromAccountSettings) {
                VehicleInsuranceScreen(
                    onNext = { _ -> navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.VehicleRates) {
                VehicleRatesScreen(
                    onNext = { nextStep -> navigateToRegistrationStep(nextStep) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.VehicleRatesFromAccountSettings) {
                VehicleRatesScreen(
                    onNext = { _ -> navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.UserProfileDetails) {
                UserProfileDetailsScreen(
                    onNavigateToStep = { step -> navigateToRegistrationStep(step, false, forceDirect = true) },
                    onContinue = { navigateToRegistrationStep(NavRoutes.VehicleSelection, false, forceDirect = false) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.Dashboard) {
                val openDrawer by it.savedStateHandle
                    .getStateFlow("openDrawer", false)
                    .collectAsStateWithLifecycle()

                com.limo1800driver.app.ui.screens.dashboard.DashboardScreen(
                    navBackStackEntry = it,
                    openDrawerRequest = openDrawer,
                    onDrawerRequestConsumed = { it.savedStateHandle["openDrawer"] = false },
                    onNavigateToBooking = { bookingId ->
                        navController.navigate("${NavRoutes.BookingPreview}/$bookingId")
                    },
                    onNavigateToBookingPreview = { bookingId ->
                        navController.navigate("${NavRoutes.BookingPreview}/$bookingId")
                    },
                    onNavigateToFinalizeRates = { bookingId, mode, source ->
                        navController.navigate("${NavRoutes.FinalizeRates}/$bookingId/$mode/$source")
                    },
                    onNavigateToEditBooking = { bookingId, source ->
                        navController.navigate("${NavRoutes.EditBooking}/$bookingId/$source")
                    },
                    onNavigateToWallet = { navController.navigate(NavRoutes.Wallet) },
                    onNavigateToMyActivity = { navController.navigate(NavRoutes.MyActivity) },
                    onNavigateToPreArrangedRides = { navController.navigate(NavRoutes.PreArrangedRides) },
                    onNavigateToNotifications = { navController.navigate(NavRoutes.Notifications) },
                    onNavigateToAccountSettings = { navController.navigate(NavRoutes.AccountSettings) },
                    onNavigateToRideInProgress = { bookingId ->
                        navController.navigate("${NavRoutes.RideInProgress}/$bookingId")
                    },
                    onLogout = {
                        tokenManager.clearAll()
                        navController.navigate(NavRoutes.PhoneEntry) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.MyActivity) {
                MyActivityScreen(onBack = { navigateToDashboardHome(openDrawer = true) })
            }

            composable(NavRoutes.Wallet) {
                WalletScreen(onBack = { navigateToDashboardHome(openDrawer = true) })
            }

            composable(NavRoutes.PreArrangedRides) {
                val refreshBookings by it.savedStateHandle
                    .getStateFlow("refreshBookings", false)
                    .collectAsStateWithLifecycle()
                PreArrangedRidesScreen(
                    onBack = { navigateToDashboardHome(openDrawer = true) },
                    onNavigateToBookingPreview = { bookingId ->
                        navController.navigate("${NavRoutes.BookingPreview}/$bookingId")
                    },
                    onNavigateToFinalizeBooking = { bookingId, source ->
                        navController.navigate("${NavRoutes.FinalizeRates}/$bookingId/finalizeOnly/$source")
                    },
                    onNavigateToFinalizeRates = { bookingId, mode, source ->
                        navController.navigate("${NavRoutes.FinalizeRates}/$bookingId/$mode/$source")
                    },
                    onNavigateToEditBooking = { bookingId, source ->
                        navController.navigate("${NavRoutes.EditBooking}/$bookingId/$source")
                    },
                    onNavigateToMap = { bookingId ->
                        android.util.Log.d("BookingNav", "Map requested for bookingId=$bookingId")
                    },
                    refreshRequested = refreshBookings,
                    onRefreshConsumed = { it.savedStateHandle["refreshBookings"] = false }
                )
            }

            composable("${NavRoutes.BookingPreview}/{bookingId}") { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toIntOrNull() ?: 0
                BookingPreviewScreen(
                    bookingId = bookingId,
                    onBack = { navController.popBackStack() },
                    onCompleted = {
                        runCatching {
                            navController.getBackStackEntry(NavRoutes.PreArrangedRides)
                                .savedStateHandle["refreshBookings"] = true
                        }
                        navController.popBackStack()
                    },
                    onNavigateToFinalizeRates = { id, mode, source ->
                        navController.navigate("${NavRoutes.FinalizeRates}/$id/$mode/$source")
                    },
                    source = "prearranged"
                )
            }

            composable("${NavRoutes.FinalizeBooking}/{bookingId}/{source}") { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toIntOrNull() ?: 0
                val source = backStackEntry.arguments?.getString("source") ?: "prearranged"
                FinalizeBookingScreen(
                    bookingId = bookingId,
                    source = source,
                    onBack = { navController.popBackStack() },
                    onNext = { id, mode, src ->
                        navController.navigate("${NavRoutes.FinalizeRates}/$id/$mode/$src")
                    }
                )
            }

            composable("${NavRoutes.FinalizeRates}/{bookingId}/{mode}/{source}") { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toIntOrNull() ?: 0
                val mode = backStackEntry.arguments?.getString("mode") ?: "finalizeOnly"
                val source = backStackEntry.arguments?.getString("source") ?: "prearranged"
                FinalizeRatesScreen(
                    bookingId = bookingId,
                    mode = mode,
                    source = source,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        if (source == "prearranged") {
                            runCatching {
                                navController.getBackStackEntry(NavRoutes.PreArrangedRides)
                                    .savedStateHandle["refreshBookings"] = true
                            }
                            navController.popBackStack(NavRoutes.PreArrangedRides, inclusive = false)
                        } else {
                            navigateToDashboardHome()
                        }
                    }
                )
            }

            composable("${NavRoutes.EditBooking}/{bookingId}/{source}") { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toIntOrNull() ?: 0
                val source = backStackEntry.arguments?.getString("source") ?: "prearranged"
                EditBookingDetailsAndRatesScreen(
                    bookingId = bookingId,
                    source = source,
                    onBack = { navController.popBackStack() },
                    onCompleted = {
                        runCatching {
                            navController.getBackStackEntry(NavRoutes.PreArrangedRides)
                                .savedStateHandle["refreshBookings"] = true
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable("${NavRoutes.RideInProgress}/{bookingId}") { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toIntOrNull() ?: 0
                RideInProgressScreen(
                    bookingId = bookingId,
                    onBack = { navController.popBackStack() },
                    onNavigateToFinalizeRates = { id, mode, source ->
                        navController.navigate("${NavRoutes.FinalizeRates}/$id/$mode/$source")
                    },
                    onNavigateToChat = { id, customerId, customerName ->
                        navController.navigate(
                            "${NavRoutes.Chat}/$id/$customerId/${Uri.encode(customerName)}"
                        )
                    }
                )
            }

            composable("${NavRoutes.Chat}/{bookingId}/{customerId}/{customerName}") { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toIntOrNull() ?: 0
                val customerId = backStackEntry.arguments?.getString("customerId").orEmpty()
                val customerName = Uri.decode(backStackEntry.arguments?.getString("customerName").orEmpty())
                ChatScreen(
                    bookingId = bookingId,
                    customerId = customerId,
                    customerName = customerName.ifBlank { "Passenger" },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.Notifications) {
                NotificationsScreen(
                    onBack = { navigateToDashboardHome(openDrawer = true) }
                )
            }

            composable(NavRoutes.ProfileView) {
                ProfileViewScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToBasicInfo = {
                        navController.navigate(NavRoutes.BasicInfoFromAccountSettings) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(NavRoutes.AccountSettings) {
                AccountSettingsScreen(
                    onBack = { navigateToDashboardHome(openDrawer = true) },
                    onNavigateToProfile = {
                        navController.navigate(NavRoutes.ProfileView) { launchSingleTop = true }
                    },
                    onNavigateToProfilePicture = {
                        navController.navigate(NavRoutes.ProfilePictureFromAccountSettings) { launchSingleTop = true }
                    },
                    onNavigateToCompanyInfo = {
                        navController.navigate(NavRoutes.CompanyInfoFromAccountSettings) { launchSingleTop = true }
                    },
                    onNavigateToCompanyDocuments = {
                        navController.navigate(NavRoutes.CompanyDocumentsFromAccountSettings) { launchSingleTop = true }
                    },
                    onNavigateToDrivingLicense = {
                        navController.navigate(NavRoutes.DrivingLicense) { launchSingleTop = true }
                    },
                    onNavigateToBankDetails = {
                        navController.navigate(NavRoutes.BankDetails) { launchSingleTop = true }
                    },
                    onNavigateToVehicleInsurance = {
                        navController.navigate(NavRoutes.VehicleInsuranceFromAccountSettings) { launchSingleTop = true }
                    },
                    onNavigateToVehicleDetails = {
                        navController.navigate(NavRoutes.VehicleDetailsCoordinatorFromAccountSettings) { launchSingleTop = true }
                    },
                    onNavigateToVehicleRates = {
                        navController.navigate(NavRoutes.VehicleRatesFromAccountSettings) { launchSingleTop = true }
                    },
                    onLogout = {
                        tokenManager.clearAll()
                        navController.navigate(NavRoutes.PhoneEntry) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = "${NavRoutes.WebView}?url={url}&title={title}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: "Web View"

                WebViewScreen(
                    url = url,
                    title = title,
                    onBack = { navController.popBackStack() }
                )
            }
        } // End of NavHost

        // 2. Alert Overlay (Foreground)
        // This sits on top of the NavHost
        EmailVerificationAlert(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp),
            onVerifyEmailClick = {
                // Navigate to email verification screen (to be implemented)
            }
        )
    } // End of Box
}