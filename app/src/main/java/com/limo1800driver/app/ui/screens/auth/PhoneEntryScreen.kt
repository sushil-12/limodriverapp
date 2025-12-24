package com.limo1800driver.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.Countries
import com.limo1800driver.app.data.model.Country
import com.limo1800driver.app.domain.validation.CountryCode
import com.limo1800driver.app.ui.components.AlertType
import com.limo1800driver.app.ui.components.CommonErrorAlertDialog
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.components.ErrorAlertDialog
import com.limo1800driver.app.ui.state.PhoneEntryUiEvent
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.theme.LimoRed
import com.limo1800driver.app.ui.viewmodel.PhoneEntryViewModel
import kotlinx.coroutines.launch

// Defining local colors to match the specific UI grey in the screenshot
// You should ideally move this to your AppColors
private val InputGrayBackground = Color(0xFFF3F4F6)
private val BorderGray = Color(0xFFE5E7EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneEntryScreen(
    onNext: (String, String) -> Unit, // tempUserId, phoneNumber
    onBack: (() -> Unit)? = null,
    viewModel: PhoneEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedCountry by remember {
        mutableStateOf(
            Countries.getCountryFromCode(
                uiState.selectedCountryCode.shortCode.uppercase()
            )
        )
    }

    var showCountryPicker by remember { mutableStateOf(false) }
    val phone = remember { mutableStateOf(uiState.phoneNumber) }

    // Error dialog state
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogTitle by remember { mutableStateOf("") }
    var errorDialogMessage by remember { mutableStateOf("") }

    // Navigate when OTP is sent successfully
    LaunchedEffect(uiState.success) {
        if (uiState.success && uiState.tempUserId.isNotEmpty()) {
            onNext(uiState.tempUserId, uiState.phoneNumberWithCountryCode)
        }
    }

    // Show error dialog when there's an API error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            errorDialogTitle = "Error"
            errorDialogMessage = error
            showErrorDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp) // Adjusted padding to match screenshot
    ) {
        Spacer(Modifier.height(60.dp)) // Top spacing

        // --- Header Section ---
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
            // 1. Country Picker (Gray Box)
            CountryPickerButton(
                country = selectedCountry,
                onClick = { showCountryPicker = true }
            )

            Spacer(Modifier.width(12.dp))

            // 2. Phone Input (Outlined Box)
            OutlinedTextField(
                value = phone.value,
                onValueChange = { input ->
                    val cleaned = input.filter { it.isDigit() }.take(selectedCountry.phoneLength)
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
                    letterSpacing = 1.5.sp, // 2.sp is clean; try 3 or 4 for a more 'airy' look
                    color = AppColors.LimoBlack,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp), // Fixed height to match picker
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

        // --- Error/Success Messages ---
        uiState.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = LimoRed,
                style = MaterialTheme.typography.bodySmall
            )
        }

        uiState.message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = LimoRed, // Using LimoRed for all messages as requested
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))

        // --- Action Button (Continue) ---
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
            shape = RoundedCornerShape(8.dp), // Standard UI button radius
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (uiState.isLoading == true) {
                ShimmerCircle(
                    size = 20.dp,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Continue",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Legal Footer ---
        val annotatedString = buildAnnotatedString {
            append("By tapping Continue, you are indicating that you accept our ")

            pushStringAnnotation(tag = "TERMS", annotation = "terms")
            withStyle(style = SpanStyle(color = AppColors.LimoOrange)) {
                append("Terms of Service")
            }
            pop()

            append(" and ")

            pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
            withStyle(style = SpanStyle(color = AppColors.LimoOrange)) {
                append("Privacy Policy")
            }
            pop()

            append(". An SMS may be sent. Message & data rates may apply.")
        }

        ClickableText(
            text = annotatedString,
            style = TextStyle(
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                    .firstOrNull()?.let {
                        // Handle Terms Click
                    }
                annotatedString.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                    .firstOrNull()?.let {
                        // Handle Privacy Click
                    }
            }
        )
    }

    // --- Country Picker Sheet Logic (Unchanged but ensuring scope is correct) ---
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
                viewModel.onEvent(PhoneEntryUiEvent.CountryCodeChanged(countryCode))
                showCountryPicker = false
            }
        )
    }

    // Error Alert Dialog
    CommonErrorAlertDialog(
        isVisible = showErrorDialog,
        onDismiss = {
            showErrorDialog = false
            // Clear the error from viewModel when dialog is dismissed
            // Note: This is handled by the LaunchedEffect clearing the error
        },
        type = AlertType.ERROR,
        title = errorDialogTitle,
        message = errorDialogMessage
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
                style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
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