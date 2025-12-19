package com.limo1800driver.app.ui.screens.registration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * iOS-parity vehicle details flow used from Account Settings.
 *
 * Mirrors `VehicleDetailsCoordinatorView.swift`:
 * Form -> Amenities -> Image Upload, with back navigation between steps,
 * and a single "close" back to Account Settings when finished.
 */
@Composable
fun VehicleDetailsCoordinatorFromAccountSettings(
    onClose: () -> Unit,
    onProfileUpdated: (() -> Unit)? = null
) {
    var step by remember { mutableStateOf(VehicleDetailsCoordinatorStep.Form) }

    when (step) {
        VehicleDetailsCoordinatorStep.Form -> {
            VehicleDetailsScreen(
                onNext = { step = VehicleDetailsCoordinatorStep.Amenities },
                onBack = { onClose() }
            )
        }

        VehicleDetailsCoordinatorStep.Amenities -> {
            VehicleAmenitiesScreen(
                onNext = { step = VehicleDetailsCoordinatorStep.ImageUpload },
                onBack = { step = VehicleDetailsCoordinatorStep.Form }
            )
        }

        VehicleDetailsCoordinatorStep.ImageUpload -> {
            VehicleDetailsImageUploadScreen(
                onNext = { _ ->
                    // Profile/vehicle details have been updated, refresh the cache
                    onProfileUpdated?.invoke()
                    onClose()
                },
                onBack = { step = VehicleDetailsCoordinatorStep.Amenities }
            )
        }
    }
}

private enum class VehicleDetailsCoordinatorStep {
    Form,
    Amenities,
    ImageUpload
}


