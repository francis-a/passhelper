import io.eyecu.passhelper.repository.DynamoTablesTest
import io.eyecu.passhelper.repository.NotificationEndpoint
import io.eyecu.passhelper.repository.NotificationEndpointRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

class NotificationEndpointRepositoryTest : DynamoTablesTest(
    schema = TableSchema.fromBean(NotificationEndpoint::class.java),
    tableName = "notification_endpoint_test"
) {

    private lateinit var repository: NotificationEndpointRepository

    override fun createRepository(client: DynamoDbEnhancedClient) {
        repository = NotificationEndpointRepository(tableName, client)
    }

    @Test
    fun `should return all emails when querying the table`() {
        repository.addEmail("test1@example.com")
        repository.addEmail("test2@example.com")

        val result = repository.findAllEmails()
        assertTrue(result.contains("test1@example.com"))
        assertTrue(result.contains("test2@example.com"))
    }

    @Test
    fun `should add an email`() {
        val email = "newemail@example.com"
        repository.addEmail(email)

        val result = repository.findAllEmails()
        assertTrue(result.contains(email.lowercase()))
    }

    @Test
    fun `should delete an email`() {
        val email = "delete@example.com"
        repository.addEmail(email)
        repository.deleteEmail(email)

        val result = repository.findAllEmails()
        assertFalse(result.contains(email.lowercase()))
    }
}
