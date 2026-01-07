package com.limo1800driver.app

import com.limo1800driver.app.ui.screens.registration.DriverLicenseFormScreen
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.limo1800driver.app.ui.screens.registration.WelcomeBannerScreen
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.data.storage.TokenManager
import com.limo1800driver.app.ui.screens.registration.VehicleDetailsStepScreen
import com.limo1800driver.app.ui.viewmodel.SplashViewModel
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
import com.limo1800driver.app.ui.screens.map.BookingMapView
import com.limo1800driver.app.ui.screens.chat.ChatScreen
import com.limo1800driver.app.ui.screens.ride.RideInProgressScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.limo1800driver.app.ui.theme.LimoDriverAppTheme
import com.limo1800driver.app.data.notification.DriverBookingReminderData
import com.limo1800driver.app.ui.screens.PhoneEntryScreen
import kotlinx.coroutines.launch
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

        // Process notification intent if app was launched from notification
        processNotificationIntent(intent)

        // Handle foreground reminder dialog trigger
        if (intent.action == "SHOW_REMINDER_DIALOG") {
            handleForegroundReminderIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Process notification intent if app was already running
        processNotificationIntent(intent)

        // Handle foreground reminder dialog trigger
        if (intent.action == "SHOW_REMINDER_DIALOG") {
            handleForegroundReminderIntent(intent)
        }
    }

    /**
     * Process notification intent and extract booking data for navigation
     */
    private fun processNotificationIntent(intent: Intent?): NotificationNavigationData? {
        intent ?: return null

        Timber.d("🔍 Processing notification intent: ${intent.action} - extras: ${intent.extras}")

        // Extract notification data from intent extras
        val extras = intent.extras ?: return null

        // Check if this is a notification tap
        val isFromNotification = extras.containsKey("booking_id") ||
                               extras.containsKey("bookingId") ||
                               extras.containsKey("event") ||
                               extras.containsKey("title") ||
                               extras.containsKey("message")

        if (!isFromNotification) {
            Timber.d("📱 Not a notification intent")
            return null
        }

        Timber.i("╔════════════════════════════════════════╗")
        Timber.i("║  NOTIFICATION INTENT RECEIVED         ║")
        Timber.i("╚════════════════════════════════════════╝")

        // Extract booking data from notification payload
        val bookingData = extractBookingDataFromIntent(intent)

        if (bookingData != null) {
            Timber.i("📋 Extracted booking data: ${bookingData.bookingId}")

            // Create navigation data
            val navigationData = NotificationNavigationData(
                bookingId = bookingData.bookingId,
                event = extras.getString("event"),
                isReminder = bookingData.requiresDriverEnRouteAction(),
                bookingData = bookingData
            )

            Timber.i("🎯 Navigation data: bookingId=${navigationData.bookingId}, isReminder=${navigationData.isReminder}")

            // Store for navigation (will be processed by DriverAppNavigation)
            currentNotificationData = navigationData

            return navigationData
        } else {
            Timber.w("⚠️ Could not extract booking data from notification intent")
            return null
        }
    }

    /**
     * Extract booking data from notification intent
     */
    private fun extractBookingDataFromIntent(intent: Intent): DriverBookingReminderData? {
        val extras = intent.extras ?: return null

        Timber.d("🔧 Extracting booking data from intent extras")

        // Convert Bundle to Map for DriverBookingReminderData.fromFcmDataMap
        val dataMap = mutableMapOf<String, String>()
        for (key in extras.keySet()) {
            val value = extras.get(key)
            if (value != null) {
                dataMap[key] = value.toString()
            }
        }

        // Try to extract booking data using the same logic as FCM service
        return DriverBookingReminderData.fromFcmDataMap(dataMap)
    }

    /**
     * Handle foreground reminder dialog trigger from FCM service
     */
    private fun handleForegroundReminderIntent(intent: Intent) {
        Timber.i("🚨 Handling foreground reminder intent")

        val bookingData = extractBookingDataFromIntent(intent)
        if (bookingData != null && bookingData.requiresDriverEnRouteAction()) {
            // Store in a way that DashboardScreen can pick it up immediately
            // We'll use the same mechanism as notification navigation but trigger it immediately
            val navigationData = NotificationNavigationData(
                bookingId = bookingData.bookingId,
                event = "booking_reminder",
                isReminder = true,
                bookingData = bookingData
            )

            // Since we're already in MainActivity, we need to trigger the reminder through navigation
            // This will be handled by the DriverAppNavigation composable
            currentNotificationData = navigationData

            Timber.i("✅ Prepared foreground reminder data for booking ${bookingData.bookingId}")
        } else {
            Timber.w("⚠️ Could not extract valid reminder data from foreground intent")
        }
    }

    /**
     * Data class to hold notification navigation information
     */
    data class NotificationNavigationData(
        val bookingId: Int,
        val event: String? = null,
        val isReminder: Boolean = false,
        val bookingData: DriverBookingReminderData? = null
    )

    companion object {
        // Store current notification data for navigation
        var currentNotificationData: NotificationNavigationData? = null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DriverAppNavigation(
    tokenManager: TokenManager,
    notificationData: MainActivity.NotificationNavigationData? = null
) {
    // Check for notification data from various sources
    val effectiveNotificationData = notificationData ?: MainActivity.currentNotificationData
    val navController = rememberNavController()
    val registrationNavigationState = remember { RegistrationNavigationState() }

    // Sync registration state with API before determining initial destination
    // This ensures we have the latest profile completion status from the server
    val splashViewModel: com.limo1800driver.app.ui.viewmodel.SplashViewModel = hiltViewModel()
    var hasSynced by remember { mutableStateOf(false) }
    
    // Determine initial destination based on authentication state (using local state initially)
    // Will be updated after API sync completes
    val initialDestination = remember {
        val isAuthenticated = tokenManager.isAuthenticated()
        val hasSeenOnboarding = tokenManager.hasSeenOnboarding()

        when {
            isAuthenticated -> {
                val isProfileCompleted = tokenManager.isProfileCompleted()
                if (isProfileCompleted) {
                    // Check if welcome screen has been seen
                    val hasSeenWelcome = tokenManager.hasSeenWelcomeScreen()
                    if (!hasSeenWelcome) {
                        NavRoutes.WelcomeBanner
                    } else {
                        NavRoutes.Dashboard
                    }
                } else {
                    val registrationState = tokenManager.getDriverRegistrationState()
                    val nextStep = registrationState?.nextStep ?: tokenManager.getNextStep()
                    when {
                        nextStep == null || nextStep == "dashboard" -> {
                            // Check if welcome screen has been seen
                            val hasSeenWelcome = tokenManager.hasSeenWelcomeScreen()
                            if (!hasSeenWelcome) {
                                NavRoutes.WelcomeBanner
                            } else {
                                NavRoutes.Dashboard
                            }
                        }
                        else -> registrationNavigationState.getRouteForStep(nextStep)
                    }
                }
            }
            hasSeenOnboarding -> NavRoutes.PhoneEntry
            else -> NavRoutes.Onboarding
        }
    }
    
    // Sync with API if authenticated (only once on app start)
    // After sync completes, navigate to correct destination if it differs from initial
    LaunchedEffect(Unit) {
        if (tokenManager.isAuthenticated() && !hasSynced) {
            splashViewModel.syncRegistrationState { nextStep ->
                hasSynced = true
                // Profile completion status is updated in TokenManager by syncRegistrationState
                val isProfileCompleted = tokenManager.isProfileCompleted()
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                
                // Determine correct destination after sync
                val correctDestination = if (isProfileCompleted) {
                    // Check if welcome screen has been seen
                    val hasSeenWelcome = tokenManager.hasSeenWelcomeScreen()
                    if (!hasSeenWelcome) {
                        NavRoutes.WelcomeBanner
                    } else {
                        NavRoutes.Dashboard
                    }
                } else {
                    val finalNextStep = nextStep ?: tokenManager.getDriverRegistrationState()?.nextStep ?: tokenManager.getNextStep()
                    when {
                        finalNextStep == null || finalNextStep == "dashboard" -> {
                            // Check if welcome screen has been seen
                            val hasSeenWelcome = tokenManager.hasSeenWelcomeScreen()
                            if (!hasSeenWelcome) {
                                NavRoutes.WelcomeBanner
                            } else {
                                NavRoutes.Dashboard
                            }
                        }
                        else -> registrationNavigationState.getRouteForStep(finalNextStep)
                    }
                }
                
                // Navigate to correct destination if it differs from current
                if (currentRoute != correctDestination) {
                    navController.navigate(correctDestination) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        } else {
            hasSynced = true
        }
    }

    // Handle notification navigation after app is ready
    LaunchedEffect(effectiveNotificationData, hasSynced) {
        if (effectiveNotificationData != null && hasSynced && tokenManager.isAuthenticated() && tokenManager.isProfileCompleted()) {
            Timber.i("🔔 Processing notification navigation: ${effectiveNotificationData.bookingId}, isReminder=${effectiveNotificationData.isReminder}")

            if (effectiveNotificationData.isReminder) {
                // For booking reminders, navigate to dashboard and show reminder dialog
                Timber.i("📱 Booking reminder notification - navigating to dashboard")
                navController.navigate(NavRoutes.Dashboard) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }

                // Trigger reminder dialog after a short delay to ensure dashboard is loaded
                kotlinx.coroutines.delay(1000)
                runCatching {
                    navController.getBackStackEntry(NavRoutes.Dashboard)
                        .savedStateHandle["showNotificationReminder"] = effectiveNotificationData.bookingData
                }.onFailure { error ->
                    Timber.e("Failed to trigger reminder dialog: $error")
                }
            } else {
                // For booking notifications, navigate to booking preview
                Timber.i("📱 Booking notification - navigating to booking preview: ${effectiveNotificationData.bookingId}")
                navController.navigate("${NavRoutes.BookingPreview}/${effectiveNotificationData.bookingId}") {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }

            // Clear the notification data after processing
            MainActivity.currentNotificationData = null
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
            // Check if welcome screen has been seen
            val hasSeenWelcome = tokenManager.hasSeenWelcomeScreen()
            val targetRoute = if (!hasSeenWelcome) {
                NavRoutes.WelcomeBanner
            } else {
                NavRoutes.Dashboard
            }
            navController.navigate(targetRoute) {
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
            isComplete -> {
                // Check if welcome screen has been seen when registration completes
                val hasSeenWelcome = tokenManager.hasSeenWelcomeScreen()
                if (!hasSeenWelcome) {
                    NavRoutes.WelcomeBanner
                } else {
                    NavRoutes.Dashboard
                }
            }
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
        
        // Check profile completion status and redirect if on registration screens
        // This ensures users with completed profiles are redirected even if they land on registration screens
        // (e.g., from Firebase notifications, deep links, or app restart)
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        
        // Check on every route change - always check fresh value from TokenManager
        LaunchedEffect(currentRoute) {
            // Check profile completion status fresh (not cached)
            val isProfileCompleted = tokenManager.isProfileCompleted()
            
            if (isProfileCompleted && currentRoute != null && currentRoute != NavRoutes.Dashboard && currentRoute != NavRoutes.WelcomeBanner) {
                val registrationRoutes = setOf(
                    NavRoutes.PrivacyTerms,
                    NavRoutes.DrivingLicense,
                    NavRoutes.BankDetails,
                    NavRoutes.ProfilePicture,
                    NavRoutes.CompanyInfo,
                    NavRoutes.CompanyDocuments,
                    NavRoutes.BasicInfo,
                    NavRoutes.VehicleSelection,
                    NavRoutes.VehicleDetailsStep,
                    NavRoutes.UserProfileDetails
                )
                if (currentRoute in registrationRoutes) {
                    // Check if welcome screen has been seen
                    val hasSeenWelcome = tokenManager.hasSeenWelcomeScreen()
                    val targetRoute = if (!hasSeenWelcome) {
                        NavRoutes.WelcomeBanner
                    } else {
                        NavRoutes.Dashboard
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

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
                // Check if profile is completed before showing the screen
                // If completed, redirect to WelcomeBanner or Dashboard (handled in PrivacyTermsScreen LaunchedEffect)
                if (tokenManager.isProfileCompleted()) {
                    LaunchedEffect(Unit) {
                        val hasSeenWelcome = tokenManager.hasSeenWelcomeScreen()
                        val targetRoute = if (!hasSeenWelcome) {
                            NavRoutes.WelcomeBanner
                        } else {
                            NavRoutes.Dashboard
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                } else {
                PrivacyTermsScreen(
                        onNext = { nextStep -> 
                            if (nextStep == "dashboard") {
                                // Check if welcome screen has been seen
                                val hasSeenWelcome = tokenManager.hasSeenWelcomeScreen()
                                val targetRoute = if (!hasSeenWelcome) {
                                    NavRoutes.WelcomeBanner
                                } else {
                                    NavRoutes.Dashboard
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navigateToRegistrationStep(nextStep, false, forceDirect = false)
                            }
                        },
                    onBack = {
                            // If profile is completed, go to WelcomeBanner or Dashboard instead
                            if (tokenManager.isProfileCompleted()) {
                                val hasSeenWelcome = tokenManager.hasSeenWelcomeScreen()
                                val targetRoute = if (!hasSeenWelcome) {
                                    NavRoutes.WelcomeBanner
                                } else {
                                    NavRoutes.Dashboard
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(NavRoutes.CompanyDocuments) {
                                launchSingleTop = true
                                popUpTo(NavRoutes.PrivacyTerms) { inclusive = true }
                                    }
                            }
                        }
                    },
                    onNavigateToWebView = { url, title ->
                        navController.navigate("${NavRoutes.WebView}?url=${url}&title=${title}")
                    }
                )
                }
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
                val accountSettingsViewModel: com.limo1800driver.app.ui.viewmodel.AccountSettingsViewModel = hiltViewModel()
                val scope = rememberCoroutineScope()
                ProfilePictureScreen(
                    onNext = { _ -> },
                    onBack = { navController.popBackStack() },
                    isEditMode = true,
                    onUpdateComplete = {
                        scope.launch {
                            // Wait for both refreshes to complete before navigating
                            // This ensures the AccountSettingsScreen shows updated data immediately
                            kotlinx.coroutines.coroutineScope {
                                launch { 
                        profileViewModel.refreshDriverProfile()
                                    Timber.d("ProfileVM: Refresh completed")
                                }
                                launch { 
                                    accountSettingsViewModel.refreshProfileData()
                                    Timber.d("AccountSettingsVM: Refresh completed")
                                }
                            }
                            // Small delay to ensure state updates are propagated
                            kotlinx.coroutines.delay(50)
                        navController.popBackStack()
                        }
                    }
                )
            }

            composable(NavRoutes.CompanyInfoFromAccountSettings) {
                val profileViewModel: com.limo1800driver.app.ui.viewmodel.DriverProfileViewModel = hiltViewModel()
                val accountSettingsViewModel: com.limo1800driver.app.ui.viewmodel.AccountSettingsViewModel = hiltViewModel()
                val scope = rememberCoroutineScope()
                CompanyInfoScreen(
                    onNext = { _ -> },
                    onBack = { navController.popBackStack() },
                    isEditMode = true,
                    onUpdateComplete = {
                        scope.launch {
                            // Wait for both refreshes to complete before navigating
                            kotlinx.coroutines.coroutineScope {
                                launch { profileViewModel.refreshDriverProfile() }
                                launch { accountSettingsViewModel.refreshProfileData() }
                            }
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(NavRoutes.CompanyDocumentsFromAccountSettings) {
                val profileViewModel: com.limo1800driver.app.ui.viewmodel.DriverProfileViewModel = hiltViewModel()
                val accountSettingsViewModel: com.limo1800driver.app.ui.viewmodel.AccountSettingsViewModel = hiltViewModel()
                val scope = rememberCoroutineScope()
                CompanyDocumentsScreen(
                    onNext = { _ -> navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    isEditMode = true,
                    onUpdateComplete = {
                        scope.launch {
                            // Wait for both refreshes to complete before navigating
                            kotlinx.coroutines.coroutineScope {
                                launch { profileViewModel.refreshDriverProfile() }
                                launch { accountSettingsViewModel.refreshProfileData() }
                            }
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(NavRoutes.BasicInfoFromAccountSettings) {
                val profileViewModel: com.limo1800driver.app.ui.viewmodel.DriverProfileViewModel = hiltViewModel()
                val accountSettingsViewModel: com.limo1800driver.app.ui.viewmodel.AccountSettingsViewModel = hiltViewModel()
                val scope = rememberCoroutineScope()
                BasicInfoScreen(
                    onNext = { _ -> navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    isEditMode = true,
                    onUpdateComplete = {
                        scope.launch {
                            // Wait for both refreshes to complete before navigating
                            // This ensures the AccountSettingsScreen shows updated data immediately
                            kotlinx.coroutines.coroutineScope {
                                launch { 
                                    profileViewModel.refreshDriverProfile()
                                    Timber.d("ProfileVM: Refresh completed")
                                }
                                launch { 
                                    accountSettingsViewModel.refreshProfileData()
                                    Timber.d("AccountSettingsVM: Refresh completed")
                                }
                            }
                            // Small delay to ensure state updates are propagated
                            kotlinx.coroutines.delay(50)
                            navController.popBackStack()
                        }
                    }
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
                val accountSettingsViewModel: com.limo1800driver.app.ui.viewmodel.AccountSettingsViewModel = hiltViewModel()
                val scope = rememberCoroutineScope()
                VehicleDetailsCoordinatorFromAccountSettings(
                    onClose = { navController.popBackStack() },
                    onProfileUpdated = {
                        scope.launch {
                            // Wait for both refreshes to complete
                            kotlinx.coroutines.coroutineScope {
                                launch { profileViewModel.refreshDriverProfile() }
                                launch { accountSettingsViewModel.refreshProfileData() }
                            }
                        }
                    }
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

            composable(NavRoutes.WelcomeBanner) {
                WelcomeBannerScreen(
                    onContinue = {
                        tokenManager.saveWelcomeScreenSeen(true)
                        navController.navigate(NavRoutes.Dashboard) {
                            popUpTo(NavRoutes.WelcomeBanner) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(NavRoutes.Dashboard) {
                val openDrawer by it.savedStateHandle
                    .getStateFlow("openDrawer", false)
                    .collectAsStateWithLifecycle()

                // Watch for navigation triggers from email verification dialog
                val navigateToBasicInfo by it.savedStateHandle
                    .getStateFlow("navigateToBasicInfo", false)
                    .collectAsStateWithLifecycle()
                val navigateToCompanyInfo by it.savedStateHandle
                    .getStateFlow("navigateToCompanyInfo", false)
                    .collectAsStateWithLifecycle()

                LaunchedEffect(navigateToBasicInfo) {
                    if (navigateToBasicInfo) {
                        it.savedStateHandle["navigateToBasicInfo"] = false
                        navController.navigate(NavRoutes.BasicInfoFromAccountSettings) {
                            launchSingleTop = true
                        }
                    }
                }

                LaunchedEffect(navigateToCompanyInfo) {
                    if (navigateToCompanyInfo) {
                        it.savedStateHandle["navigateToCompanyInfo"] = false
                        navController.navigate(NavRoutes.CompanyInfoFromAccountSettings) {
                            launchSingleTop = true
                        }
                    }
                }

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
                        navController.navigate("${NavRoutes.BookingMap}/$bookingId")
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

            composable("${NavRoutes.BookingMap}/{bookingId}") { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toIntOrNull() ?: 0
                BookingMapView(
                    bookingId = bookingId,
                    onBack = { navController.popBackStack() }
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
    } // End of Box
}