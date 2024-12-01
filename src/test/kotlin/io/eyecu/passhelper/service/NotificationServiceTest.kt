import io.eyecu.passhelper.models.UserView
import io.eyecu.passhelper.repository.PartitionKey
import io.eyecu.passhelper.repository.PassportRepository
import io.eyecu.passhelper.repository.PassportRepository.Passport
import io.eyecu.passhelper.repository.SortKey
import io.eyecu.passhelper.service.NotificationService
import io.eyecu.passhelper.service.UserPoolService
import io.eyecu.passhelper.util.toTimestamp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import java.time.LocalDate

class NotificationServiceTest {

    private val sesClient = mock<SesClient>()
    private val userPoolService = mock<UserPoolService>()
    private val passportRepository = mock<PassportRepository>()

    private val domain = "example.com"
    private val emailName = "noreply"
    private val notificationService = NotificationService(
        sesClient,
        userPoolService,
        passportRepository,
        domain,
        emailName
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

        verify(sesClient, times(2)).sendEmail(isA<SendEmailRequest>())
    }

    @Test
    fun `should not send emails if no passport is found`() {
        val partitionKey = PartitionKey("passport123")
        val sortKey = SortKey("range1")

        whenever(passportRepository.find(partitionKey, sortKey)).thenReturn(null)

        notificationService.send(partitionKey, sortKey)

        verify(sesClient, never()).sendEmail(isA<SendEmailRequest>())
    }

    @Test
    fun `should not send emails if no email addresses are found`() {
        val partitionKey = PartitionKey("passport123")
        val sortKey = SortKey("range1")
        val passport = createPassport()

        whenever(passportRepository.find(partitionKey, sortKey)).thenReturn(passport)
        whenever(userPoolService.listAllUsersWithEmailEnabled()).thenReturn(emptyList())

        notificationService.send(partitionKey, sortKey)

        verify(sesClient, never()).sendEmail(isA<SendEmailRequest>())
    }

    @Test
    fun `should send email with correct request`() {
        val partitionKey = PartitionKey("passport123")
        val sortKey = SortKey("range1")
        val passport = createPassport()
        val emails = createEmails("test@example.com")

        whenever(passportRepository.find(partitionKey, sortKey)).thenReturn(passport)
        whenever(userPoolService.listAllUsersWithEmailEnabled()).thenReturn(emails)

        notificationService.send(partitionKey, sortKey)

        argumentCaptor<SendEmailRequest>().apply {
            verify(sesClient).sendEmail(capture())
            assertEquals("test@example.com", firstValue.destination().toAddresses()[0])
            assertTrue(firstValue.message().subject().data().contains("It's time to renew your passport John!"))
        }
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
