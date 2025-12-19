package com.limo1800driver.app.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

import androidx.compose.runtime.Composable

/**
 * A modifier that makes a composable clickable without any visual ripple effect.
 * This disables the default Material Design ripple/hover effects.
 */
@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    onClick = onClick
)
