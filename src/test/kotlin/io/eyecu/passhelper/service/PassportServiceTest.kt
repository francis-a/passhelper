import io.eyecu.passhelper.models.CreatePassportForm
import io.eyecu.passhelper.models.EditPassportForm
import io.eyecu.passhelper.repository.PassportNotificationRepository
import io.eyecu.passhelper.repository.PassportRepository
import io.eyecu.passhelper.repository.PassportRepository.Passport
import io.eyecu.passhelper.service.PassportService
import io.eyecu.passhelper.util.toTimestamp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class PassportServiceTest {

    // Create the mocks at the class level
    private val passportRepository = mock<PassportRepository>()
    private val passportNotificationRepository = mock<PassportNotificationRepository>()
    private val passportService = PassportService(passportRepository, passportNotificationRepository)

    @Test
    fun `should save passport and create notification`() {
        val form = CreatePassportForm(
            firstName = "John",
            lastName = "Doe",
            dob = LocalDate.of(1990, 1, 1),
            number = "123456",
            issuingCountry = "USA",
            issuedDate = LocalDate.of(2020, 1, 1),
            expiresDate = LocalDate.of(2030, 1, 1)
        )

        val passportId = "passport123"
        whenever(passportRepository.save(any())).thenReturn(passportId)

        passportService.save(form)

        verify(passportRepository).save(any())
        argumentCaptor<LocalDate>().apply {
            verify(passportNotificationRepository).put(eq(passportId), capture())
            assertEquals(LocalDate.of(2029, 7, 1), firstValue)
        }
    }

    @Test
    fun `should update passport and create notification`() {
        val form = EditPassportForm(
            firstName = "John",
            lastName = "Doe",
            number = "123456",
            issuedDate = LocalDate.of(2020, 1, 1),
            expiresDate = LocalDate.of(2030, 1, 1)
        )

        val passportId = "passport123"
        whenever(passportRepository.update(any(), any())).thenReturn(passportId)

        passportService.update(passportId, form)

        verify(passportRepository).update(eq(passportId), any())
        argumentCaptor<LocalDate>().apply {
            verify(passportNotificationRepository).put(eq(passportId), capture())
            assertEquals(LocalDate.of(2029, 7, 1), firstValue)
        }
    }

    @Test
    fun `should get passport view`() {
        val passportId = "passport123"
        val passport = Passport(
            id = passportId,
            firstName = "John",
            lastName = "Doe",
            dob = LocalDate.of(1990, 1, 1).toTimestamp(),
            number = "123456",
            countryCode = "USA",
            issued = LocalDate.of(2020, 1, 1).toTimestamp(),
            expires = LocalDate.of(2030, 1, 1).toTimestamp()
        )

        whenever(passportRepository.get(passportId)).thenReturn(passport)
        whenever(passportNotificationRepository.get(passportId)).thenReturn(LocalDate.of(2029, 7, 1))

        val result = passportService.get(passportId)

        assertEquals(passportId, result.id)
        assertEquals("John", result.firstName)
        assertEquals(LocalDate.of(2029, 7, 1), result.notificationDate)
        verify(passportRepository).get(passportId)
        verify(passportNotificationRepository).get(passportId)
    }

    @Test
    fun `should delete passport and notification`() {
        val passportId = "passport123"

        passportService.delete(passportId)

        verify(passportRepository).delete(passportId)
        verify(passportNotificationRepository).delete(passportId)
    }

    @Test
    fun `should find all passports and merge notification dates`() {
        val passport1 = Passport(
            id = "passport123",
            firstName = "John",
            lastName = "Doe",
            dob = LocalDate.of(1990, 1, 1).toTimestamp(),
            number = "123456",
            countryCode = "USA",
            issued = LocalDate.of(2020, 1, 1).toTimestamp(),
            expires = LocalDate.of(2030, 1, 1).toTimestamp()
        )

        val passport2 = Passport(
            id = "passport456",
            firstName = "Jane",
            lastName = "Doe",
            dob = LocalDate.of(1985, 2, 2).toTimestamp(),
            number = "654321",
            countryCode = "CA",
            issued = LocalDate.of(2015, 2, 2).toTimestamp(),
            expires = LocalDate.of(2025, 2, 2).toTimestamp()
        )

        whenever(passportRepository.findAll()).thenReturn(listOf(passport1, passport2))
        whenever(passportNotificationRepository.findAll()).thenReturn(
            mapOf(
                "passport123" to LocalDate.of(2029, 7, 1),
                "passport456" to LocalDate.of(2024, 8, 1)
            )
        )

        val result = passportService.findAll()

        assertEquals(2, result.size)
        assertEquals("passport123", result[0].id)
        assertEquals(LocalDate.of(2029, 7, 1), result[0].notificationDate)
        assertEquals("passport456", result[1].id)
        assertEquals(LocalDate.of(2024, 8, 1), result[1].notificationDate)

        verify(passportRepository).findAll()
        verify(passportNotificationRepository).findAll()
    }
}
