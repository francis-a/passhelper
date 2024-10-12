package io.eyecu.passhelper.models

import java.time.LocalDate

data class CreatePassportForm(
    var firstName: String? = null,
    var lastName: String? = null,
    var dob: LocalDate? = null,
    var number: String? = null,
    var issuingCountry: String? = null,
    var issuedDate: LocalDate? = null,
    var expiresDate: LocalDate? = null,
)