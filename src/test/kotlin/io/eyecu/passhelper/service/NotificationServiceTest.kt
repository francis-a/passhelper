import io.eyecu.passhelper.models.ExpiringPassportEmailView
import io.eyecu.passhelper.models.UserView
import io.eyecu.passhelper.models.countryCodeToCountryName
import io.eyecu.passhelper.repository.PartitionKey
import io.eyecu.passhelper.repository.PassportRepository
import io.eyecu.passhelper.repository.PassportRepository.Passport
import io.eyecu.passhelper.repository.SortKey
import io.eyecu.passhelper.service.EmailService
import io.eyecu.passhelper.service.NotificationService
import io.eyecu.passhelper.service.UserPoolService
import io.eyecu.passhelper.util.millisecondsToLocalDate
import io.eyecu.passhelper.util.toTimestamp
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class NotificationServiceTest {

    private val emailService = mock<EmailService>()
    private val userPoolService = mock<UserPoolService>()
    private val passportRepository = mock<PassportRepository>()

    private val domain = "example.com"
    private val notificationService = NotificationService(
        emailService,
        userPoolService,
        passportRepository,
        domain,
        "no-reply"
    )

    @Test
    fun `should send notification emails for expiring passport`() {
        val partitionKey = PartitionKey("passport123")
        val sortKey = SortKey("range1")
        val passport = createPassport()
        val emails = createEmails("test1@example.com", "test2@example.com")

        whenever(passportRepository.find(partitionKey, sortKey)).thenReturn(passport)
        whenever(userPoolService.listAllUsersWithEmailEnabled()).thenReturn(emails)

        notificationService.send(partitionKey, sortKey)

        verify(emailService).sendEmail(
            from = "no-reply@example.com",
            to = "test1@example.com",
            template = "emails/reminder",
            source = "Passport Renewal Reminder",
            subject = "It's time to renew your passport ${passport.firstName}!",
            content = mapOf(
                "expiringPassport" to ExpiringPassportEmailView(
                    fullName = "${passport.firstName} ${passport.lastName}",
                    countryName = passport.countryCode.countryCodeToCountryName(),
                    issuedDate = millisecondsToLocalDate(passport.issued),
                    expiresDate = millisecondsToLocalDate(passport.expires),
                    url = "https://$domain"
                )
            )
        )

        verify(emailService).sendEmail(
            from = "no-reply@example.com",
            to = "test2@example.com",
            template = "emails/reminder",
            source = "Passport Renewal Reminder",
            subject = "It's time to renew your passport ${passport.firstName}!",
            content = mapOf(
                "expiringPassport" to ExpiringPassportEmailView(
                    fullName = "${passport.firstName} ${passport.lastName}",
                    countryName = passport.countryCode.countryCodeToCountryName(),
                    issuedDate = millisecondsToLocalDate(passport.issued),
                    expiresDate = millisecondsToLocalDate(passport.expires),
                    url = "https://$domain"
                )
            )
        )
    }

    @Test
    fun `should not send emails if no passport is found`() {
        val partitionKey = PartitionKey("passport123")
        val sortKey = SortKey("range1")

        whenever(passportRepository.find(partitionKey, sortKey)).thenReturn(null)

        notificationService.send(partitionKey, sortKey)

        verify(emailService, never()).sendEmail(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `should not send emails if no email addresses are found`() {
        val partitionKey = PartitionKey("passport123")
        val sortKey = SortKey("range1")
        val passport = createPassport()

        whenever(passportRepository.find(partitionKey, sortKey)).thenReturn(passport)
        whenever(userPoolService.listAllUsersWithEmailEnabled()).thenReturn(emptyList())

        notificationService.send(partitionKey, sortKey)

        verify(emailService, never()).sendEmail(any(), any(), any(), any(), any(), any())
    }

    private fun createEmails(vararg addresses: String) = addresses.map {
        UserView(
            username = it,
            emailAddress = it,
            emailEnabled = true,
            owner = false,
            loginEnabled = false
        )
    }

    private fun createPassport(
        id: String = "passport123",
        firstName: String = "John",
        lastName: String = "Doe",
        dob: LocalDate = LocalDate.of(1990, 1, 1),
        number: String = "123456",
        countryCode: String = "USA",
        issued: LocalDate = LocalDate.of(2020, 1, 1),
        expires: LocalDate = LocalDate.of(2030, 1, 1)
    ) = Passport(
        id = id,
        firstName = firstName,
        lastName = lastName,
        dob = dob.toTimestamp(),
        number = number,
        countryCode = countryCode,
        issued = issued.toTimestamp(),
        expires = expires.toTimestamp()
    )
}
