package io.eyecu.passhelper.models

import java.time.LocalDate

data class EditPassportForm(
    var firstName: String,
    var lastName: String,
    var number: String,
    var issuedDate: LocalDate,
    var expiresDate: LocalDate,
)