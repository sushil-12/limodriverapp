package com.limo1800driver.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.R
import com.limo1800driver.app.ui.theme.LimoBlack
import com.limo1800driver.app.ui.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
    onFinished: (String?) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    var scale by remember { mutableStateOf(0.3f) }
    var alpha by remember { mutableStateOf(0f) }
    var hasSynced by remember { mutableStateOf(false) }
    
    // Sync registration state if needed
    LaunchedEffect(Unit) {
        viewModel.syncRegistrationState { nextStep ->
            hasSynced = true
        }
    }
    
    LaunchedEffect(hasSynced) {
        if (hasSynced) {
            // Start with fade in and zoom in simultaneously
            alpha = 1f
            scale = 1.0f
            kotlinx.coroutines.delay(1200)
            
            // Hold for a moment
            kotlinx.coroutines.delay(500)
            
            // Fade out
            alpha = 0f
            kotlinx.coroutines.delay(300)
            
            // Get the synced next step
            val state = viewModel.uiState.value
            onFinished(null) // Pass null, MainActivity will determine route from stored state
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LimoBlack),
        contentAlignment = Alignment.Center
    ) {
        Image(
            // Use rasterized launcher foreground to avoid adaptive icon crash
            painter = painterResource(id = R.drawable.splashlogo),
            contentDescription = "1-800-LIMO.COM Logo",
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .fillMaxWidth(0.6f) // increases size relative to screen width
                .aspectRatio(1f)    // or set actual ratio of your logo, for example 1f means square
        )
    }
}

