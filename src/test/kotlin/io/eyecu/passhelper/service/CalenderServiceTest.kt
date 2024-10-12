import biweekly.Biweekly
import io.eyecu.passhelper.models.ExpiringWithin
import io.eyecu.passhelper.models.PassportView
import io.eyecu.passhelper.service.CalenderService
import io.eyecu.passhelper.service.PassportService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import java.net.URI
import java.time.LocalDate

class CalenderServiceTest {

    private val icsBucket = "test-bucket"
    private val passportService = mock<PassportService>()
    private val awsS3 = mock<S3Client>()
    private val s3Presigner = mock<S3Presigner>()

    private val calenderService = CalenderService(
        icsBucket = icsBucket,
        passportService = passportService,
        awsS3 = awsS3,
        s3Presigner = s3Presigner
    )

    private fun createPassportView(
        id: String = "1",
        firstName: String = "John",
        lastName: String = "Doe",
        number: String = "123456789",
        countryCode: String = "US",
        countryName: String = "USA",
        issuedDate: LocalDate = LocalDate.of(2015, 1, 1),
        expiresDate: LocalDate = LocalDate.now().plusMonths(6)
    ) = PassportView(
        id = id,
        firstName = firstName,
        lastName = lastName,
        dob = LocalDate.of(1990, 1, 1),
        number = number,
        countryCode = countryCode,
        countryName = countryName,
        issuedDate = issuedDate,
        expiresDate = expiresDate,
        notificationDate = null,
        expiringWithin = ExpiringWithin.HALF_YEAR
    )

    @Test
    fun `should create passport expiration calendar and save to S3`() {
        val passport = createPassportView()
        whenever(passportService.findAll()).thenReturn(listOf(passport))

        val presignedUrl = "https://test-bucket.s3.amazonaws.com/test.ics"
        val presignedRequest = mock<PresignedGetObjectRequest> {
            on { url() } doReturn URI.create(presignedUrl).toURL()
        }
        whenever(s3Presigner.presignGetObject(any<GetObjectPresignRequest>())).thenReturn(presignedRequest)

        val result = calenderService.createPassportExpirationCalender()

        verify(awsS3).putObject(any<PutObjectRequest>(), any<RequestBody>())
        assertEquals(presignedUrl, result)
    }

    @Test
    fun `should generate valid S3 key for saving calendar`() {
        val passport = createPassportView()
        whenever(passportService.findAll()).thenReturn(listOf(passport))

        val presignedUrl = "https://test-bucket.s3.amazonaws.com/test.ics"
        val presignedRequest = mock<PresignedGetObjectRequest> {
            on { url() } doReturn URI.create(presignedUrl).toURL()
        }
        whenever(s3Presigner.presignGetObject(any<GetObjectPresignRequest>())).thenReturn(presignedRequest)
        calenderService.createPassportExpirationCalender()

        argumentCaptor<PutObjectRequest>().apply {
            verify(awsS3).putObject(capture(), any<RequestBody>())
            val s3Key = firstValue.key()
            assertTrue(s3Key.startsWith("ics/"))
            assertTrue(s3Key.endsWith("/reminders.ics"))
        }
    }

    @Test
    fun `should create events for all passports returned by passport service`() {
        val passports = listOf(
            createPassportView(id = "1", firstName = "John", lastName = "Doe"),
            createPassportView(id = "2", firstName = "Jane", lastName = "Foe")
        )
        whenever(passportService.findAll()).thenReturn(passports)

        val presignedUrl = "https://test-bucket.s3.amazonaws.com/test.ics"
        val presignedRequest = mock<PresignedGetObjectRequest> {
            on { url() } doReturn URI.create(presignedUrl).toURL()
        }
        whenever(s3Presigner.presignGetObject(any<GetObjectPresignRequest>())).thenReturn(presignedRequest)

        calenderService.createPassportExpirationCalender()

        argumentCaptor<RequestBody>().apply {
            verify(awsS3).putObject(any<PutObjectRequest>(), capture())
            val calendarContent = firstValue.contentStreamProvider().newStream().reader().use { it.readText() }

            val ical = Biweekly.parse(calendarContent).first()
            val events = ical.events

            assertEquals(4, events.size, "There should be 4 events (2 expiration, 2 will-expire-soon)")
            assertTrue(events.any { it.summary.value == "John Doe's passport issued by USA expires today" })
            assertTrue(events.any { it.summary.value == "Jane Foe's passport issued by USA expires today" })
            assertTrue(events.any { it.summary.value == "John Doe's passport issued by USA is expiring soon" })
            assertTrue(events.any { it.summary.value == "Jane Foe's passport issued by USA is expiring soon" })
        }
    }

    @Test
    fun `should set unique event id`() {
        val passports = listOf(
            createPassportView(id = "1", firstName = "John", lastName = "Doe")
        )
        whenever(passportService.findAll()).thenReturn(passports)

        val presignedUrl = "https://test-bucket.s3.amazonaws.com/test.ics"
        val presignedRequest = mock<PresignedGetObjectRequest> {
            on { url() } doReturn URI.create(presignedUrl).toURL()
        }
        whenever(s3Presigner.presignGetObject(any<GetObjectPresignRequest>())).thenReturn(presignedRequest)

        calenderService.createPassportExpirationCalender()

        argumentCaptor<RequestBody>().apply {
            verify(awsS3).putObject(any<PutObjectRequest>(), capture())
            val calendarContent = firstValue.contentStreamProvider().newStream().reader().use { it.readText() }

            val ical = Biweekly.parse(calendarContent).first()
            val events = ical.events

            assertTrue(events.any { it.uid.value == "Sm9obiBEb2UtMjAyNS0wNC0xMi0yMDI1LTA0LTEyLVVT" })
            assertTrue(events.any { it.uid.value == "Sm9obiBEb2UtMjAyNC0xMC0xMi0yMDI1LTA0LTEyLVVT" })

        }
    }

    @Test
    fun `should format name correctly in event summary`() {
        val passports = listOf(
            createPassportView(firstName = "James", lastName = "Jones"),
            createPassportView(firstName = "John", lastName = "Doe")
        )
        whenever(passportService.findAll()).thenReturn(passports)

        val presignedUrl = "https://test-bucket.s3.amazonaws.com/test.ics"
        val presignedRequest = mock<PresignedGetObjectRequest> {
            on { url() } doReturn URI.create(presignedUrl).toURL()
        }
        whenever(s3Presigner.presignGetObject(any<GetObjectPresignRequest>())).thenReturn(presignedRequest)

        calenderService.createPassportExpirationCalender()

        argumentCaptor<RequestBody>().apply {
            verify(awsS3).putObject(any<PutObjectRequest>(), capture())
            val calendarContent = firstValue.contentStreamProvider().newStream().reader().use { it.readText() }

            val ical = Biweekly.parse(calendarContent).first()
            val events = ical.events

            assertTrue(
                events.any { it.summary.value == "James Jones' passport issued by USA expires today" },
                "Name ending in 's' should be formatted as Jones'"
            )
            assertTrue(
                events.any { it.summary.value == "John Doe's passport issued by USA expires today" },
                "Name not ending in 's' should be formatted as Doe's"
            )
        }
    }
}
