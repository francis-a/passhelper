package io.eyecu.passhelper.service

import io.eyecu.passhelper.models.ExpiringPassportEmailView
import io.eyecu.passhelper.models.countryCodeToCountryName
import io.eyecu.passhelper.repository.PartitionKey
import io.eyecu.passhelper.repository.PassportRepository
import io.eyecu.passhelper.repository.PassportRepository.Passport
import io.eyecu.passhelper.repository.SortKey
import io.eyecu.passhelper.util.millisecondsToLocalDate

class NotificationService(
    private val emailService: EmailService,
    private val userPoolService: UserPoolService,
    private val passportRepository: PassportRepository,
    private val domain: String
) {

    fun send(partitionKey: PartitionKey, sortKey: SortKey) {
        val passport = passportRepository.find(partitionKey, sortKey) ?: return
        val emails = userPoolService.listAllUsersWithEmailEnabled().map { it.emailAddress }

        emails.forEach { email ->
            emailService.sendEmail(
                to = email,
                template = "emails/reminder",
                source = "Passport Renewal Reminder",
                subject = passport.subject(),
                content = mapOf("expiringPassport" to passport.toView())
            )
        }
    }

    private fun Passport.toView() = ExpiringPassportEmailView(
        fullName = "$firstName $lastName",
        countryName = countryCode.countryCodeToCountryName(),
        issuedDate = millisecondsToLocalDate(issued),
        expiresDate = millisecondsToLocalDate(expires),
        url = "https://$domain"
    )

    private fun Passport.subject() = "It's time to renew your passport $firstName!"

}