import io.eyecu.passhelper.repository.DynamoTablesTest
import io.eyecu.passhelper.repository.PassportNotification
import io.eyecu.passhelper.repository.PassportNotificationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema
import java.time.LocalDate
import java.util.Base64

class PassportNotificationRepositoryTest : DynamoTablesTest(
    schema = BeanTableSchema.create(PassportNotification::class.java),
    tableName = "passport_notification_test"
) {

    private lateinit var repository: PassportNotificationRepository

    override fun createRepository(client: DynamoDbEnhancedClient) {
        repository = PassportNotificationRepository("passport_notification_test", client)
    }


    @Test
    fun `should add and retrieve passport notification`() {
        val passportId = createPassportId("passport123", "range1")
        val notificationDate = LocalDate.of(2024, 9, 10)

        repository.put(passportId, notificationDate)
        val result = repository.get(passportId)

        assertNotNull(result)
        assertEquals(notificationDate, result)
    }

    @Test
    fun `should find all passport notifications`() {
        val passportId1 = createPassportId("passport123", "range1")
        val passportId2 = createPassportId("passport456", "range2")
        val notificationDate1 = LocalDate.of(2024, 9, 10)
        val notificationDate2 = LocalDate.of(2024, 10, 15)

        repository.put(passportId1, notificationDate1)
        repository.put(passportId2, notificationDate2)

        val result = repository.findAll()

        assertTrue(result.containsKey(passportId1))
        assertTrue(result.containsKey(passportId2))
        assertEquals(notificationDate1, result[passportId1])
        assertEquals(notificationDate2, result[passportId2])
    }

    @Test
    fun `should delete passport notification`() {
        val passportId = createPassportId("passport123", "range1")
        val notificationDate = LocalDate.of(2024, 9, 10)

        repository.put(passportId, notificationDate)

        val deletedNotification = repository.delete(passportId)
        val result = repository.get(passportId)

        assertNotNull(deletedNotification)
        assertEquals("passport123", deletedNotification.name)
        assertNull(result)
    }

    @Test
    fun `should find all matching passport notifications`() {
        val passportId = createPassportId("passport123", "range1")
        val notificationDate = LocalDate.of(2024, 9, 10)

        repository.put(passportId, notificationDate)

        val result = repository.findAllMatching(passportId)

        assertTrue(result.containsKey(passportId))
        assertEquals(notificationDate, result[passportId])
    }

    private fun createPassportId(hash: String, range: String): String {
        return Base64.getUrlEncoder().encodeToString("$hash#$range".toByteArray())
    }

}
