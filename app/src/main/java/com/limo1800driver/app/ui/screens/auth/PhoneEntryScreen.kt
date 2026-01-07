package com.limo1800driver.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.Countries
import com.limo1800driver.app.data.model.Country
import com.limo1800driver.app.domain.validation.CountryCode
import com.limo1800driver.app.ui.components.AlertType
import com.limo1800driver.app.ui.components.CommonErrorAlertDialog
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.state.PhoneEntryUiEvent
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.PhoneEntryViewModel
import kotlinx.coroutines.launch

// Ideally move to AppColors
private val InputGrayBackground = Color(0xFFF3F4F6)
private val BorderGray = Color(0xFFE5E7EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneEntryScreen(
    onNext: (String, String) -> Unit,
    onBack: (() -> Unit)? = null,
    onNavigateToWebView: (String, String) -> Unit,
    viewModel: PhoneEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // UI State for inputs
    var selectedCountry by remember {
        mutableStateOf(Countries.getCountryFromCode(uiState.selectedCountryCode.shortCode.uppercase()))
    }
    var showCountryPicker by remember { mutableStateOf(false) }
    val phone = remember { mutableStateOf(uiState.phoneNumber) }

    // Alert State
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogTitle by remember { mutableStateOf("") }
    var errorDialogMessage by remember { mutableStateOf("") }

    // Initialize phoneLength on first load if it doesn't match selectedCountry
    LaunchedEffect(selectedCountry) {
        val countryCode = try {
            CountryCode.valueOf(selectedCountry.shortCode.uppercase())
        } catch (e: IllegalArgumentException) {
            CountryCode.US
        }
        // Sync phoneLength if it doesn't match
        if (uiState.phoneLength != selectedCountry.phoneLength) {
            viewModel.onEvent(PhoneEntryUiEvent.CountryCodeChanged(countryCode, selectedCountry.phoneLength))
        }
    }

    // Handlers
    LaunchedEffect(uiState.success) {
        if (uiState.success && uiState.tempUserId.isNotEmpty()) {
            onNext(uiState.tempUserId, uiState.phoneNumberWithCountryCode)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            errorDialogTitle = "Error"
            errorDialogMessage = error
            showErrorDialog = true
        }
    }

    // SCROLL STATE: Essential for keyboard handling
    val scrollState = rememberScrollState()

    // ROOT CONTAINER
    // We do NOT use windowInsetsPadding(safeDrawing) here.
    // Instead, we use a Column that fills the screen and handles padding specifically.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(100.dp))

        // --- Header Section (Now Stable) ---
        Text(
            text = "Welcome Driver!",
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.LimoBlack
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Whats your phone number?",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = AppColors.LimoBlack.copy(alpha = 0.7f)
            )
        )

        Spacer(Modifier.height(32.dp))

        // --- Input Section ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CountryPickerButton(
                country = selectedCountry,
                onClick = { showCountryPicker = true }
            )

            Spacer(Modifier.width(12.dp))

            OutlinedTextField(
                value = phone.value,
                onValueChange = { input ->
                    // Allow up to 15 digits (reasonable max for international phone numbers)
                    // Validation will enforce country-specific length
                    val cleaned = input.filter { it.isDigit() }.take(15)
                    phone.value = cleaned
                    viewModel.onEvent(PhoneEntryUiEvent.PhoneNumberChanged(cleaned))
                },
                prefix = {
                    Text(
                        text = selectedCountry.code + " ",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = AppColors.LimoBlack,
                            fontWeight = FontWeight.Normal
                        )
                    )
                },
                placeholder = {
                    Text(
                        text = "Enter Your phone number",
                        color = Color.Gray.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    letterSpacing = 1.2.sp,
                    color = AppColors.LimoBlack,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.LimoOrange,
                    unfocusedBorderColor = BorderGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = AppColors.LimoOrange
                )
            )
        }

        // --- Error Messages ---
        uiState.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = LimoRed, style = MaterialTheme.typography.bodySmall)
        }

        uiState.message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = LimoGreen, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        // --- Action Button ---
        Button(
            onClick = {
                if (uiState.isLoading != true) {
                    viewModel.onEvent(PhoneEntryUiEvent.SendVerificationCode)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.LimoOrange,
                disabledContainerColor = AppColors.LimoOrange.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (uiState.isLoading == true) {
                ShimmerCircle(size = 20.dp, strokeWidth = 2.dp)
            } else {
                Text(
                    text = "Continue",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Legal Footer ---
        LegalFooter(onNavigateToWebView)
        
        // Add extra space at bottom for comfortable scrolling and gesture navigation
        Spacer(Modifier.height(24.dp))
        
        // Add padding for Android gesture navigation to prevent button underlapping
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }

    // --- Bottom Sheet ---
    if (showCountryPicker) {
        CountryPickerBottomSheet(
            onDismiss = { showCountryPicker = false },
            onCountrySelected = { country ->
                selectedCountry = country
                val countryCode = try {
                    CountryCode.valueOf(country.shortCode.uppercase())
                } catch (e: IllegalArgumentException) {
                    CountryCode.US
                }
                // Pass the actual phoneLength from Country model, not CountryCode enum
                viewModel.onEvent(PhoneEntryUiEvent.CountryCodeChanged(countryCode, country.phoneLength))
                showCountryPicker = false
            }
        )
    }
}

@Composable
fun LegalFooter(onNavigateToWebView: (String, String) -> Unit) {
    val annotatedString = buildAnnotatedString {
        append("By tapping Continue, you are indicating that you accept our ")
        pushStringAnnotation(tag = "TERMS", annotation = "terms")
        withStyle(style = SpanStyle(color = AppColors.LimoOrange)) { append("Terms of Service") }
        pop()
        append(" and ")
        pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
        withStyle(style = SpanStyle(color = AppColors.LimoOrange)) { append("Privacy Policy") }
        pop()
        append(". An SMS may be sent. Message & data rates may apply.")
    }

    ClickableText(
        text = annotatedString,
        style = TextStyle(fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                .firstOrNull()?.let {
                    onNavigateToWebView("https://1800limo.com/client-terms-condition", "Terms of Service")
                }
            annotatedString.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                .firstOrNull()?.let {
                    onNavigateToWebView("https://1800limo.com/privacy-policy", "Privacy Policy")
                }
        }
    )
}

/**
 * Updated Country Picker Button to match the "Gray Box" styling
 */
@Composable
fun CountryPickerButton(
    country: Country,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(56.dp) // Match TextField height
            .background(
                color = InputGrayBackground, // Use the light gray
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = country.flag,
            style = TextStyle(fontSize = 24.sp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Select Country",
            tint = AppColors.LimoBlack,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Extracted Bottom Sheet for cleanliness
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerBottomSheet(
    onDismiss: () -> Unit,
    onCountrySelected: (Country) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        },
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = "Select Country",
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold, 
                    fontSize = 18.sp,
                    color = AppColors.LimoBlack // Explicitly set to black for dark mode compatibility
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
            )
            HorizontalDivider(color = BorderGray)
            LazyColumn {
                items(Countries.list) { country ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { sheetState.hide() }
                                    .invokeOnCompletion { onCountrySelected(country) }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = country.flag, fontSize = 24.sp)
                            Spacer(Modifier.width(16.dp))
                            Text(text = country.name, fontSize = 16.sp, color = AppColors.LimoBlack)
                        }
                        Text(text = country.code, fontSize = 16.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}