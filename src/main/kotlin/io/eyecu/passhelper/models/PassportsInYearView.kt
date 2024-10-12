package io.eyecu.passhelper.models

data class PassportsInYearView(
    val expirationYear: Int,
    val passports: List<PassportView>
)