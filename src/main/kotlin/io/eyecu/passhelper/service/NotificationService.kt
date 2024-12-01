package io.eyecu.passhelper.service

import io.eyecu.passhelper.models.ExpiringPassportEmailView
import io.eyecu.passhelper.models.countryCodeToCountryName
import io.eyecu.passhelper.repository.PartitionKey
import io.eyecu.passhelper.repository.PassportRepository
import io.eyecu.passhelper.repository.PassportRepository.Passport
import io.eyecu.passhelper.repository.SortKey
import io.eyecu.passhelper.util.millisecondsToLocalDate
import io.eyecu.passhelper.util.templateEngine
import org.thymeleaf.context.Context
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.SendEmailRequest

class NotificationService(
    private val sesClient: SesClient,
    private val userPoolService: UserPoolService,
    private val passportRepository: PassportRepository,
    private val domain: String,
    emailName: String
) {

    private val emailAddress = "$emailName@$domain"

    fun send(partitionKey: PartitionKey, sortKey: SortKey) {
        val passport = passportRepository.find(partitionKey, sortKey) ?: return
        val emails = userPoolService.listAllUsersWithEmailEnabled().map { it.emailAddress }
        emails.forEach {
            sesClient.sendEmail(passport.sendRequestForEmail(it))
        }
    }

    private fun Passport.sendRequestForEmail(email: String) = SendEmailRequest.builder()
        .source("\"Passport Renewal Reminder\" <$emailAddress>")
        .destination(
            Destination.builder()
                .toAddresses(email)
                .build()
        )
        .message(
            Message.builder()
                .subject { it.data(subject()).charset("UTF-8") }
                .body { body -> body.html { it.data(body()).charset("UTF-8") } }
                .build()

        ).build()

    private fun Passport.body(): String {
        val context = Context()
        context.setVariable("expiringPassport", toView())
        return templateEngine.process("emails/reminder", context)
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