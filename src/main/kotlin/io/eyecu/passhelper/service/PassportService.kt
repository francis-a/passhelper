package io.eyecu.passhelper.service

import io.eyecu.passhelper.models.CreatePassportForm
import io.eyecu.passhelper.models.EditPassportForm
import io.eyecu.passhelper.models.ExpiringWithin
import io.eyecu.passhelper.models.PassportView
import io.eyecu.passhelper.models.countries
import io.eyecu.passhelper.models.countryCodeToCountryName
import io.eyecu.passhelper.models.countryToCountryCode
import io.eyecu.passhelper.repository.PartitionKey
import io.eyecu.passhelper.repository.PassportNotificationRepository
import io.eyecu.passhelper.repository.PassportRepository
import io.eyecu.passhelper.repository.PassportRepository.CreatePassportRequest
import io.eyecu.passhelper.repository.PassportRepository.EditPassportRequest
import io.eyecu.passhelper.repository.PassportRepository.Passport
import io.eyecu.passhelper.repository.SortKey
import io.eyecu.passhelper.util.millisecondsToLocalDate
import io.eyecu.passhelper.util.toTimestamp
import io.eyecu.passhelper.web.DisplayException
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit

const val WARN_BEFORE_EXPIRATION_MONTHS = 6L

class PassportService(
    private val passportRepository: PassportRepository,
    private val passportNotificationRepository: PassportNotificationRepository
) {

    fun save(createPassportForm: CreatePassportForm): String {
        val createRequest = createPassportForm.toCreateRequest()
        val passportId = passportRepository.save(createRequest)
        savePassportNotification(passportId, createRequest.expires)
        return passportId
    }

    fun update(id: String, editPassportForm: EditPassportForm): String {
        val editRequest = editPassportForm.toEditRequest()
        val updatedPassportId = passportRepository.update(id, editRequest)
        savePassportNotification(updatedPassportId, editRequest.expires)
        return updatedPassportId
    }

    fun get(id: String): PassportView {
        val passport = passportRepository.get(id)
        val notificationDate = passportNotificationRepository.get(passport.id)
        return passport.toView(notificationDate)
    }

    fun find(partitionKey: PartitionKey, sortKey: SortKey): PassportView? =
        passportRepository.find(partitionKey, sortKey)?.toView(LocalDate.now())

    fun delete(id: String) {
        passportRepository.delete(id)
        passportNotificationRepository.delete(id)
    }

    fun findAll(): List<PassportView> = with(passportRepository.findAll()) {
        mergeWithNotificationDates(passportNotificationRepository.findAll())
    }

    private fun Iterable<Passport>.mergeWithNotificationDates(notificationDates: Map<String, LocalDate>) =
        map {
            val notificationDate = notificationDates[it.id]
            it.toView(notificationDate)
        }

    private fun savePassportNotification(passportId: String, expiresTimestamp: Long) {
        val notificationTime = notificationDate(expiresTimestamp)
        if (notificationTime.isBefore(LocalDate.now(UTC))) {
            passportNotificationRepository.delete(passportId)
        } else {
            passportNotificationRepository.put(
                passportId = passportId,
                notificationTime = notificationTime
            )
        }
    }

    private fun notificationDate(expiresTimestamp: Long): LocalDate =
        millisecondsToLocalDate(expiresTimestamp)
            .minusMonths(WARN_BEFORE_EXPIRATION_MONTHS)

    private fun EditPassportForm.toEditRequest() = EditPassportRequest(
        firstName = firstName.validateAndGet(),
        lastName = lastName.validateAndGet(),
        number = number.validateAndGet(),
        issued = issuedDate.validateAndGet().toTimestamp(),
        expires = expiresDate.validateAndGet().toTimestamp()
    )

    private fun CreatePassportForm.toCreateRequest() = CreatePassportRequest(
        firstName = firstName.validateAndGet(),
        lastName = lastName.validateAndGet(),
        dob = dob.validateAndGet().toTimestamp(),
        number = number.validateAndGet(),
        countryCode = issuingCountry.validateAndGet().countryToCountryCode(),
        issued = issuedDate.validateAndGet().toTimestamp(),
        expires = expiresDate.validateAndGet().toTimestamp()
    )

    private fun Passport.toView(
        notificationDate: LocalDate?,
        requestTime: LocalDate = LocalDate.now()
    ) = PassportView(
        id = id,
        firstName = firstName,
        lastName = lastName,
        dob = millisecondsToLocalDate(dob),
        number = number,
        countryCode = countryCode.validCountryCodeOrDefault(),
        countryName = countryCode.countryCodeToCountryName(),
        issuedDate = millisecondsToLocalDate(issued),
        expiresDate = millisecondsToLocalDate(expires),
        expiringWithin = millisecondsToLocalDate(expires).calculateExpiringWithin(requestTime),
        notificationDate = notificationDate,
    )

    private fun String.validCountryCodeOrDefault() = countries.firstOrNull {
        it.code.equals(this, ignoreCase = true)
    }?.code ?: "xx"

    private fun LocalDate.calculateExpiringWithin(requestTime: LocalDate): ExpiringWithin {
        val months = ChronoUnit.MONTHS.between(requestTime, this)
        return when {
            months <= 6L -> ExpiringWithin.HALF_YEAR
            months <= 12L -> ExpiringWithin.YEAR
            else -> ExpiringWithin.OVER_YEAR
        }
    }

    private fun <T> T?.validateAndGet(): T {
        if (this == null) {
            throw DisplayException("Invalid passport request")
        }
        return this
    }
}