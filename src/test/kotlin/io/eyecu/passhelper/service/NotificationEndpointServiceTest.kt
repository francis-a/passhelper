import io.eyecu.passhelper.repository.NotificationEndpointRepository
import io.eyecu.passhelper.service.NotificationEndpointService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Base64

class NotificationEndpointServiceTest {

    private val notificationEndpointRepository = mock<NotificationEndpointRepository>()
    private val notificationEndpointService = NotificationEndpointService(notificationEndpointRepository)

    @Test
    fun `should find all emails and convert to NotificationEndpointView`() {
        val emails = listOf("test1@example.com", "test2@example.com")
        whenever(notificationEndpointRepository.findAllEmails()).thenReturn(emails)

        val result = notificationEndpointService.findAllEmails()

        assertEquals(2, result.size)
        assertEquals("test1@example.com", result[0].email)
        assertEquals(Base64.getUrlEncoder().encodeToString("test1@example.com".toByteArray()), result[0].id)
        verify(notificationEndpointRepository).findAllEmails()
    }

    @Test
    fun `should add email in lowercase`() {
        val email = "Test@Example.Com"

        notificationEndpointService.addEmail(email)

        verify(notificationEndpointRepository).addEmail("test@example.com")
    }

    @Test
    fun `should delete email by id`() {
        val email = "test@example.com"
        val encodedId = Base64.getUrlEncoder().encodeToString(email.toByteArray())

        notificationEndpointService.deleteEmail(encodedId)

        verify(notificationEndpointRepository).deleteEmail(email)
    }
}
