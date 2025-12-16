package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.theme.LimoGrey

/**
 * Common header for all dashboard menu-item screens.
 *
 * - Handles status bar safe area
 * - Centers title
 * - Optional trailing icon (e.g. Search/Close)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonMenuHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: () -> Unit,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    contentColor: Color = Color.Black,
    subtitleColor: Color = LimoGrey
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Surface(color = backgroundColor, contentColor = contentColor) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button (fixed width to keep title centered)
                IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                // Center: Title
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 13.sp,
                                color = subtitleColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Right: Optional trailing action (fixed width to keep title centered)
                if (trailingIcon != null && onTrailingClick != null) {
                    IconButton(onClick = onTrailingClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = trailingContentDescription
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}


