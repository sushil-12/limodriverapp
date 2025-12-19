package com.limo1800driver.app

import com.limo1800driver.app.ui.screens.registration.DriverLicenseFormScreen
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.limo1800driver.app.ui.navigation.NavRoutes
import com.limo1800driver.app.ui.screens.SplashScreen
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
import com.limo1800driver.app.ui.screens.registration.VehicleDetailsScreen
import com.limo1800driver.app.ui.screens.registration.VehicleAmenitiesScreen
import com.limo1800driver.app.ui.screens.registration.VehicleDetailsImageUploadScreen
import com.limo1800driver.app.ui.screens.registration.VehicleInsuranceScreen
import com.limo1800driver.app.ui.screens.registration.VehicleRatesScreen
import com.limo1800driver.app.ui.screens.registration.UserProfileDetailsScreen
import com.limo1800driver.app.ui.screens.registration.VehicleDetailsCoordinatorFromAccountSettings
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.data.storage.TokenManager
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
        
        // Set status bar and navigation bar colors to match app background
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
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

    fun navigateToDashboardHome(openDrawer: Boolean = false) {
        // Check if we're already on the Dashboard route
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute == NavRoutes.Dashboard) {
            // Already on Dashboard, just open the drawer if requested
            if (openDrawer) {
                runCatching {
                    navController.currentBackStackEntry?.savedStateHandle?.set("openDrawer", true)
                }
            }
            return
        }

        if (openDrawer) {
            // Signal Dashboard to open the drawer when we navigate to it.
            runCatching {
                navController.getBackStackEntry(NavRoutes.Dashboard)
                    .savedStateHandle["openDrawer"] = true
            }
        }
        // Prefer returning to an existing Dashboard instance to preserve its state.
        // If Dashboard isn't on the stack (edge cases), navigate to it.
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
        // Priority check: If profile is completed, always go to dashboard
        val isProfileCompleted = tokenManager.isProfileCompleted()
        if (isProfileCompleted) {
            navController.navigate(NavRoutes.Dashboard) {
                launchSingleTop = true
                if (clearBackStack) {
                    popUpTo(NavRoutes.Splash) { inclusive = true }
                }
            }
            return
        }
        
        // Otherwise, follow the step flow
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
                popUpTo(NavRoutes.Splash) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Splash
    ) {
        composable(NavRoutes.Splash) {
            SplashScreen(onFinished = { syncedNextStep ->
                val isAuthenticated = tokenManager.isAuthenticated()
                val hasSeenOnboarding = tokenManager.hasSeenOnboarding()

                Timber.tag("MainActivity").d("Splash finished with syncedNextStep: $syncedNextStep")

                when {
                    isAuthenticated -> {
                        // Check profile completion status first (priority check)
                        val isProfileCompleted = tokenManager.isProfileCompleted()

                        if (isProfileCompleted) {
                            // Profile completed, go directly to dashboard
                            Timber.tag("MainActivity").d("Profile completed, navigating to dashboard")
                            navigateToRegistrationStep("dashboard", true)
                        } else {
                            // Use the synced next step from SplashScreen, fallback to stored state
                            val nextStep = syncedNextStep ?: run {
                                val registrationState = tokenManager.getDriverRegistrationState()
                                registrationState?.nextStep ?: tokenManager.getNextStep()
                            }

                            Timber.tag("MainActivity").d("Using nextStep: $nextStep (synced: ${syncedNextStep != null})")

                            if (syncedNextStep == null || nextStep == null || nextStep == "dashboard") {
                                // Registration complete, go to dashboard and mark profile as completed
                                Timber.tag("MainActivity").d("Registration completed, marking profile as completed and going to dashboard")
                                tokenManager.saveProfileCompleted(true)
                                navigateToRegistrationStep("dashboard", true)
                            } else {
                                // Navigate to next registration step
                                Timber.tag("MainActivity").d("Navigating to next registration step: $nextStep")
                                navigateToRegistrationStep(nextStep, true, forceDirect = false)
                            }
                        }
                    }
                    hasSeenOnboarding -> navController.navigate(NavRoutes.PhoneEntry) {
                        popUpTo(NavRoutes.Splash) { inclusive = true }
                    }
                    else -> navController.navigate(NavRoutes.Onboarding) {
                        popUpTo(NavRoutes.Splash) { inclusive = true }
                    }
                }
            })
        }

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
                onBack = null
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
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavRoutes.BasicInfo) {
            BasicInfoScreen(
                onNext = { nextStep -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                onBack = null // No back button on Basic Info per iOS flow
            )
        }

        composable(NavRoutes.CompanyInfo) {
            BackHandler {
                val popped = navController.popBackStack()
                if (!popped) {
                    android.util.Log.d("NavBack", "System back CompanyInfo: no stack, navigating to BasicInfo")
                    navController.navigate(NavRoutes.BasicInfo) {
                        launchSingleTop = true
                    }
                } else {
                    android.util.Log.d("NavBack", "System back CompanyInfo: popped stack")
                }
                if (navController.currentDestination?.route == NavRoutes.BasicInfo) {
                    android.util.Log.d("NavBack", "System back CompanyInfo: now at BasicInfo")
                }
            }
            CompanyInfoScreen(
                onNext = { nextStep -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                onBack = {
                    // If nothing to pop (e.g., resumed straight into this step), go back to Basic Info
                    val popped = navController.popBackStack()
                    if (!popped) {
                        android.util.Log.d("NavBack", "CompanyInfo back: no stack, navigating to BasicInfo")
                        navController.navigate(NavRoutes.BasicInfo) {
                            launchSingleTop = true
                        }
                    } else {
                        android.util.Log.d("NavBack", "CompanyInfo back: popped stack")
                    }
                    if (navController.currentDestination?.route == NavRoutes.BasicInfo) {
                        android.util.Log.d("NavBack", "CompanyInfo back: now at BasicInfo")
                    }
                }
            )
        }

        composable(NavRoutes.CompanyDocuments) {
            CompanyDocumentsScreen(
                onNext = { nextStep -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                onBack = {
                    // Prefer stack pop; if stack is empty (e.g., resumed mid-flow), navigate back explicitly
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
                    // Prefer stack pop; if stack is empty (e.g., resumed mid-flow), navigate back explicitly
                    val popped = navController.popBackStack()
                    if (!popped) {
                        navController.navigate(NavRoutes.CompanyDocuments) {
                            launchSingleTop = true
                            popUpTo(NavRoutes.PrivacyTerms) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(NavRoutes.DrivingLicense) {
            DriverLicenseFormScreen(
                onNext = { nextStep: String? -> navigateToRegistrationStep(nextStep, false, forceDirect = false) },
                onBack = {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        android.util.Log.d("NavBack", "DrivingLicense back: navigating to UserProfileDetails")
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
                        android.util.Log.d("NavBack", "BankDetails back: navigating to UserProfileDetails")
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
                        android.util.Log.d("NavBack", "ProfilePicture back: navigating to UserProfileDetails")
                        navController.navigate(NavRoutes.UserProfileDetails) { launchSingleTop = true }
                    }
                }
            )
        }
        
        // Profile picture edit when launched from Account Settings -> Profile
        composable(NavRoutes.ProfilePictureFromAccountSettings) {
            val profileViewModel: com.limo1800driver.app.ui.viewmodel.DriverProfileViewModel = hiltViewModel()
            ProfilePictureScreen(
                onNext = { _ ->
                    // Profile picture updated, refresh the cache
                    profileViewModel.refreshDriverProfile()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Company Info edit from Account Settings
        composable(NavRoutes.CompanyInfoFromAccountSettings) {
            CompanyInfoScreen(
                onNext = { _ ->
                    // Company info updated, just go back to Account Settings
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                isEditMode = true,
                onUpdateComplete = { navController.popBackStack() }
            )
        }

        // Company Documents edit from Account Settings
        composable(NavRoutes.CompanyDocumentsFromAccountSettings) {
            CompanyDocumentsScreen(
                onNext = { _ ->
                    // Company documents updated, just go back to Account Settings
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                isEditMode = true,
                onUpdateComplete = { navController.popBackStack() }
            )
        }

        // Basic Info edit from Account Settings/Profile View
        composable(NavRoutes.BasicInfoFromAccountSettings) {
            BasicInfoScreen(
                onNext = { _ -> navController.popBackStack() }, // Return to previous screen on save
                onBack = { navController.popBackStack() },      // Allow going back
                isEditMode = true                                // Enable edit mode
            )
        }

        composable(NavRoutes.VehicleSelection) {
            VehicleSelectionScreen(
                onNext = {
                    navController.navigate(NavRoutes.VehicleDetailsStep)
                },
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

        // Vehicle details flow when launched from Account Settings -> Vehicles
        composable(NavRoutes.VehicleDetailsStepFromAccountSettings) {
            VehicleDetailsStepScreen(
                onNavigateToStep = { route -> navController.navigate(route) },
                onContinue = { navController.popBackStack() }, // return to Account Settings
                onBack = { navController.popBackStack() }
            )
        }

        // iOS-style coordinator flow used from Account Settings
        composable(NavRoutes.VehicleDetailsCoordinatorFromAccountSettings) {
            val profileViewModel: com.limo1800driver.app.ui.viewmodel.DriverProfileViewModel = hiltViewModel()
            VehicleDetailsCoordinatorFromAccountSettings(
                onClose = { navController.popBackStack() },
                onProfileUpdated = {
                    // Refresh driver profile cache when vehicle details are updated
                    profileViewModel.refreshDriverProfile()
                }
            )
        }

        composable(NavRoutes.VehicleDetails) {
            VehicleDetailsScreen(
                onNext = {
                    navController.navigate(NavRoutes.VehicleAmenities)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.VehicleAmenities) {
            VehicleAmenitiesScreen(
                onNext = {
                    navController.navigate(NavRoutes.VehicleDetailsImageUpload)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.VehicleDetailsImageUpload) {
            VehicleDetailsImageUploadScreen(
                // On success, return to the step hub so user can continue as per flow
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

        // Vehicle insurance when launched from Account Settings -> Vehicles
        composable(NavRoutes.VehicleInsuranceFromAccountSettings) {
            VehicleInsuranceScreen(
                onNext = { _ -> navController.popBackStack() }, // return to Account Settings
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.VehicleRates) {
            VehicleRatesScreen(
                onNext = { nextStep -> navigateToRegistrationStep(nextStep) },
                onBack = { navController.popBackStack() }
            )
        }

        // Vehicle rates when launched from Account Settings -> Vehicles
        composable(NavRoutes.VehicleRatesFromAccountSettings) {
            VehicleRatesScreen(
                onNext = { _ -> navController.popBackStack() }, // return to Account Settings
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
                    // Booking tap from dashboard - route to BookingPreview for now
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
                onNavigateToWallet = {
                    navController.navigate(NavRoutes.Wallet)
                },
                onNavigateToMyActivity = {
                    navController.navigate(NavRoutes.MyActivity)
                },
                onNavigateToPreArrangedRides = {
                    navController.navigate(NavRoutes.PreArrangedRides)
                },
                onNavigateToNotifications = {
                    navController.navigate(NavRoutes.Notifications)
                },
                onNavigateToAccountSettings = {
                    navController.navigate(NavRoutes.AccountSettings)
                },
                onNavigateToRideInProgress = { bookingId ->
                    navController.navigate("${NavRoutes.RideInProgress}/$bookingId")
                },
                onLogout = {
                    // Clear all data and navigate to splash
                    tokenManager.clearAll()
                    navController.navigate(NavRoutes.Splash) {
                        popUpTo(NavRoutes.Splash) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavRoutes.MyActivity) {
            MyActivityScreen(
                onBack = { navigateToDashboardHome(openDrawer = true) }
            )
        }
        
        composable(NavRoutes.Wallet) {
            WalletScreen(
                onBack = { navigateToDashboardHome(openDrawer = true) }
            )
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
                    // Keep callback for backwards compatibility, but route directly to rates screen.
                    navController.navigate("${NavRoutes.FinalizeRates}/$bookingId/finalizeOnly/$source")
                },
                onNavigateToFinalizeRates = { bookingId, mode, source ->
                    navController.navigate("${NavRoutes.FinalizeRates}/$bookingId/$mode/$source")
                },
                onNavigateToEditBooking = { bookingId, source ->
                    navController.navigate("${NavRoutes.EditBooking}/$bookingId/$source")
                },
                onNavigateToMap = { bookingId ->
                    // TODO: Map screen not implemented yet
                    android.util.Log.d("BookingNav", "Map requested for bookingId=$bookingId")
                },
                refreshRequested = refreshBookings,
                onRefreshConsumed = { it.savedStateHandle["refreshBookings"] = false }
            )
        }

        // --- Booking flow screens (ported from iOS driver) ---
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
                        // RideInProgress / Dashboard flows should return home.
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

        // --- Live ride screen (active_ride -> RideInProgress) ---
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
                    navController.navigate(NavRoutes.ProfileView) {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfilePicture = {
                    navController.navigate(NavRoutes.ProfilePictureFromAccountSettings) {
                        launchSingleTop = true
                    }
                },
                onNavigateToCompanyInfo = {
                    navController.navigate(NavRoutes.CompanyInfoFromAccountSettings) {
                        launchSingleTop = true
                    }
                },
                onNavigateToCompanyDocuments = {
                    navController.navigate(NavRoutes.CompanyDocumentsFromAccountSettings) {
                        launchSingleTop = true
                    }
                },
                onNavigateToDrivingLicense = {
                    navController.navigate(NavRoutes.DrivingLicense) {
                        launchSingleTop = true
                    }
                },
                onNavigateToBankDetails = {
                    navController.navigate(NavRoutes.BankDetails) {
                        launchSingleTop = true
                    }
                },
                onNavigateToVehicleInsurance = {
                    navController.navigate(NavRoutes.VehicleInsuranceFromAccountSettings) {
                        launchSingleTop = true
                    }
                },
                onNavigateToVehicleDetails = {
                    navController.navigate(NavRoutes.VehicleDetailsCoordinatorFromAccountSettings) {
                        launchSingleTop = true
                    }
                },
                onNavigateToVehicleRates = {
                    navController.navigate(NavRoutes.VehicleRatesFromAccountSettings) {
                        launchSingleTop = true
                    }
                },
                onLogout = {
                    // Clear token and navigate to splash
                    tokenManager.clearAll()
                    navController.navigate(NavRoutes.Splash) {
                        popUpTo(NavRoutes.Splash) { inclusive = true }
                    }
                }
            )
        }
    }
}

