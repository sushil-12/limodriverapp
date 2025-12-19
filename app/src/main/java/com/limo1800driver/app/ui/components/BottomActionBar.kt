package com.limo1800driver.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.limo1800driver.app.ui.components.ShimmerCircle

/**
 * A reusable bottom action bar with Back and Next buttons.
 *
 * @param isLoading Whether to show a loading indicator on the Next button.
 * @param onBack The callback to be invoked when the Back button is clicked. Can be null to hide the button.
 * @param onNext The callback to be invoked when the Next button is clicked.
 * @param nextButtonText The text to display on the Next button.
 * @param backButtonText The text to display on the Back button.
 */
@Composable
fun BottomActionBar(
    isLoading: Boolean,
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    nextButtonText: String = "Submit",
    backButtonText: String = "Back"
) {
    val brandOrange = Color(0xFFF28B2F)
    Surface(
        // Keeps the action bar (and its primary CTA) above the on-screen keyboard and system bars.
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.ime)
            ),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onNext,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandOrange)
            ) {
                if (isLoading) {
                    ShimmerCircle(size = 20.dp)
                } else {
                    Text(text = nextButtonText, color = Color.White)
                }
            }
        }
    }
}
