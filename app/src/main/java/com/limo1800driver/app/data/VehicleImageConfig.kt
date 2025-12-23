package com.limo1800driver.app.ui.screens.registration

import com.limo1800driver.app.R

// 1. Data model for a single image slot
data class VehicleImageSlot(
    val index: Int,
    val label: String,
    val description: String,
    val isRequired: Boolean,
    val overlayResId: Int? = null // Resource ID for the ghost image
)

object VehicleImageConfig {

    // 2. Define standard slots for "Quotebot" display quality
    fun getSlotsForVehicle(vehicleType: String): List<VehicleImageSlot> {
        val overlay = getOverlayForVehicleType(vehicleType)

        return listOf(
            VehicleImageSlot(
                index = 0,
                label = "Front Driver Corner",
                description = "Best angle for main profile",
                isRequired = true,
                overlayResId = overlay // Only Slot 1 gets the ghost image
            ),
            VehicleImageSlot(
                index = 1,
                label = "Side Profile",
                description = "Show full length",
                isRequired = false
            ),
            VehicleImageSlot(
                index = 2,
                label = "Rear View",
                description = "Back bumper and trunk",
                isRequired = false
            ),
            VehicleImageSlot(
                index = 3,
                label = "Interior Front",
                description = "Dashboard & Driver seat",
                isRequired = false
            ),
            VehicleImageSlot(
                index = 4,
                label = "Interior Rear",
                description = "Passenger seating area",
                isRequired = false
            ),
            VehicleImageSlot(
                index = 5,
                label = "Trunk / Luggage",
                description = "Cargo space open",
                isRequired = false
            )
        )
    }

    // 3. Map your 16 types to ~4-5 Overlay Resource IDs
    // You need to add these drawables to your res/drawable folder
    private fun getOverlayForVehicleType(type: String): Int {
        return when (type) {
            // Group 1: Sedans / Standard Cars
            "Mid-Size Sedan",
            "Full-Size Sedan",
            "Classic / Antique",
            "Taxi Minivan / Handicap", // Often car-shaped or small van
            "Water Taxi (Venice / Brasil)" -> R.drawable.overlay_sedan_outline

            // Group 2: SUVs & Stretch
            "Mid-Size SUV/Comfort/X",
            "Full-Size SUV",
            "Stretch" -> R.drawable.overlay_suv_outline

            // Group 3: Vans
            "Mini-Van / V Class",
            "Passanger Van",
            "Sprinter / Transit Van" -> R.drawable.overlay_van_outline

            // Group 4: Large Bus / Coach
            "Minibus",
            "Bus / Party",
            "Motor Coach",
            "Trolley",
            "School Bus" -> R.drawable.overlay_bus_outline

            else -> R.drawable.overlay_van_outline // Default fallback
        }
    }
}