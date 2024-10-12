package io.eyecu.passhelper.models

import java.time.LocalDate

data class ExpiringPassportEmailView(
    val fullName: String,
    val countryName: String,
    val issuedDate: LocalDate,
    val expiresDate: LocalDate,
    val url: String
)