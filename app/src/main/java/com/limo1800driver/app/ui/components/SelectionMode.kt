package com.limo1800driver.app.ui.components

/**
 * Defines the behavior of the CommonDropdown.
 * - NORMAL: Standard list of text options.
 * - YEAR: Calendar-style grid for selecting a year.
 * - MONTH: Grid layout for selecting a month (requires year context).
 * - DAY: Calendar grid for selecting a specific day (requires month/year context).
 */
enum class SelectionMode {
    NORMAL,
    YEAR,
    MONTH,
    DAY
}