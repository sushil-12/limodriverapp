package com.limo1800driver.app

import DriverLicenseFormScreen
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.data.storage.TokenManager
import com.limo1800driver.app.ui.screens.registration.VehicleDetailsStepScreen
import com.limo1800driver.app.ui.screens.dashboard.MyActivityScreen
import com.limo1800driver.app.ui.screens.dashboard.WalletScreen
import com.limo1800driver.app.ui.screens.dashboard.PreArrangedRidesScreen
import com.limo1800driver.app.ui.screens.dashboard.NotificationsScreen
import com.limo1800driver.app.ui.screens.dashboard.AccountSettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.limo1800driver.app.ui.theme.LimoDriverAppTheme

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
        
        setContent {
            LimoDriverAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
            SplashScreen(onFinished = {
                val isAuthenticated = tokenManager.isAuthenticated()
                val hasSeenOnboarding = tokenManager.hasSeenOnboarding()
                
                when {
                    isAuthenticated -> {
                        // Check profile completion status first (priority check)
                        val isProfileCompleted = tokenManager.isProfileCompleted()
                        
                        if (isProfileCompleted) {
                            // Profile completed, go directly to dashboard
                            navigateToRegistrationStep("dashboard", true)
                        } else {
                            // Check stored registration state
                            val registrationState = tokenManager.getDriverRegistrationState()
                            val nextStep = registrationState?.nextStep ?: tokenManager.getNextStep()
                            
                            if (registrationState?.isCompleted == true || nextStep == null || nextStep == "dashboard") {
                                // Registration complete, go to dashboard
                                navigateToRegistrationStep("dashboard", true)
                            } else if (nextStep != null) {
                                // Navigate to next registration step
                                navigateToRegistrationStep(nextStep, true, forceDirect = false)
                            } else {
                                // Fallback to phone entry
                                navController.navigate(NavRoutes.PhoneEntry) {
                                    popUpTo(NavRoutes.Splash) { inclusive = true }
                                }
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

        composable(NavRoutes.VehicleRates) {
            VehicleRatesScreen(
                onNext = { nextStep -> navigateToRegistrationStep(nextStep) },
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
            com.limo1800driver.app.ui.screens.dashboard.DashboardScreen(
                onNavigateToBooking = { bookingId ->
                    // TODO: Navigate to booking details
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
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(NavRoutes.Wallet) {
            WalletScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(NavRoutes.PreArrangedRides) {
            PreArrangedRidesScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(NavRoutes.Notifications) {
            NotificationsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(NavRoutes.AccountSettings) {
            AccountSettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToProfile = {
                    // TODO: Navigate to Profile screen when implemented
                    // navController.navigate(NavRoutes.Profile)
                },
                onNavigateToCompanyInfo = {
                    navController.navigate(NavRoutes.CompanyInfo) {
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
                    navController.navigate(NavRoutes.VehicleInsurance) {
                        launchSingleTop = true
                    }
                },
                onNavigateToVehicleDetails = {
                    navController.navigate(NavRoutes.VehicleDetailsStep) {
                        launchSingleTop = true
                    }
                },
                onNavigateToVehicleRates = {
                    navController.navigate(NavRoutes.VehicleRates) {
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

