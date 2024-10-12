package io.eyecu.passhelper.models

import java.util.Locale

val countries = Locale.getISOCountries()
    .map {
        Country(
            name = Locale.of("", it).getDisplayCountry(Locale.US),
            code = it
        )
    }

fun String.countryToCountryCode() = countries.firstOrNull {
    it.name.equals(this, ignoreCase = true)
}?.code ?: this

fun String.countryCodeToCountryName() = countries.firstOrNull {
    it.code.equals(this, ignoreCase = true)
}?.name ?: this

data class Country(
    val name: String,
    val code: String
)