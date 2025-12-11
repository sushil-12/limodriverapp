package com.limo1800driver.app.domain.validation

/**
 * Country code enumeration with phone number specifications
 */
enum class CountryCode(
    val code: String,
    val shortCode: String,
    val displayName: String,
    val phoneLength: Int
) {
    US("+1", "us", "United States", 10),
    UK("+44", "uk", "United Kingdom", 10),
    CA("+1", "ca", "Canada", 10),
    AU("+61", "au", "Australia", 9),
    DE("+49", "de", "Germany", 11),
    FR("+33", "fr", "France", 10),
    IN("+91", "in", "India", 10)
}

