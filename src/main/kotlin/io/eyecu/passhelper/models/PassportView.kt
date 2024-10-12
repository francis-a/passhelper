package io.eyecu.passhelper.models

import java.time.LocalDate

data class PassportView(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dob: LocalDate,
    val number: String,
    val countryCode: String,
    val countryName: String,
    val issuedDate: LocalDate,
    val expiresDate: LocalDate,
    val notificationDate: LocalDate?,
    private val expiringWithin: ExpiringWithin
) {
    val fullName = "$firstName $lastName"
    val expiring = expiringWithin.name
}

enum class ExpiringWithin {
    HALF_YEAR,
    YEAR,
    OVER_YEAR
}