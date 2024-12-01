package io.eyecu.passhelper.service

import io.eyecu.passhelper.models.UserView
import io.eyecu.passhelper.web.DisplayException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminResetUserPasswordRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType
import java.time.Instant

class UserPoolServiceTest {

    private val cognitoClient = mock<CognitoIdentityProviderClient>()
    private val emailService = mock<EmailService>()
    private val userPoolId = "test-pool"

    private val userPoolService = UserPoolService(
        emailService = emailService,
        cognitoClient = cognitoClient,
        userPoolId = userPoolId,
        domainName = "example.com"
    )

    @Test
    fun `should list all users`() {
        whenever(cognitoClient.listUsers(any<ListUsersRequest>())).thenReturn(
            createUserResponse(
                listOf(createUser("a"), createUser("b"))
            )
        )

        val results = userPoolService.listAllUsers()

        assertThat(results)
            .containsExactly(
                createUserView("a"),
                createUserView("b")
            )
    }

    @Test
    fun `list users should cycle through all pages`() {
        val firstResponse = createUserResponse(
            listOf(createUser("a")),
            nextPage = "next"
        )

        val secondResponse = createUserResponse(
            listOf(createUser("b")),
        )

        whenever(cognitoClient.listUsers(any<ListUsersRequest>())).thenReturn(
            firstResponse, secondResponse
        )


        val results = userPoolService.listAllUsers()

        assertThat(results)
            .containsExactly(
                createUserView("a"),
                createUserView("b")
            )

        verify(cognitoClient, times(2)).listUsers(any<ListUsersRequest>())
    }

    @Test
    fun `list users with emails enabled should only return users with email enabled`() {
        whenever(cognitoClient.listUsers(any<ListUsersRequest>())).thenReturn(
            createUserResponse(
                listOf(createUser("a", emailEnabled = true), createUser("b"))
            )
        )

        val results = userPoolService.listAllUsersWithEmailEnabled()

        assertThat(results)
            .containsExactly(
                createUserView("a", emailEnabled = true)
            )
    }

    @Test
    fun `createUser should invoke create and disable user`() {
        val userType = createUser("newUser@example.com")
        whenever(cognitoClient.adminCreateUser(any<AdminCreateUserRequest>())).thenReturn(
            AdminCreateUserResponse.builder().user(userType).build()
        )
        whenever(cognitoClient.adminGetUser(any<AdminGetUserRequest>())).thenReturn(
            AdminGetUserResponse.builder()
                .userAttributes(userType.attributes())
                .build()
        )

        userPoolService.createUser("newUser@example.com")

        verify(cognitoClient).adminCreateUser(any<AdminCreateUserRequest>())
        verify(cognitoClient).adminDisableUser(any<AdminDisableUserRequest>())
    }

    @Test
    fun `toggleUserAttribute should enable or disable email`() {
        userPoolService.toggleUserAttribute("testUser", "email", "true")

        verify(cognitoClient).adminUpdateUserAttributes(any<AdminUpdateUserAttributesRequest>())
        verify(cognitoClient, never()).adminEnableUser(any<AdminEnableUserRequest>())
        verify(cognitoClient, never()).adminDisableUser(any<AdminDisableUserRequest>())
    }

    @Test
    fun `toggleUserAttribute should enable or disable login`() {
        val userType = createUser("testUser", owner = false)
        whenever(cognitoClient.adminGetUser(any<AdminGetUserRequest>())).thenReturn(
            AdminGetUserResponse.builder()
                .userAttributes(userType.attributes())
                .build()
        )

        userPoolService.toggleUserAttribute("testUser", "login", "false")

        verify(cognitoClient).adminDisableUser(any<AdminDisableUserRequest>())
        verify(cognitoClient, never()).adminUpdateUserAttributes(any<AdminUpdateUserAttributesRequest>())
    }

    @Test
    fun `toggleUserAttribute should throw for invalid attribute`() {
        val exception = assertThrows<DisplayException> {
            userPoolService.toggleUserAttribute("testUser", "invalid", "true")
        }

        assertThat(exception.message).isEqualTo("Invalid attribute invalid")
    }

    @Test
    fun `deleteUser should call delete for non-owner`() {
        val userType = createUser("user@example.com", owner = false)
        whenever(cognitoClient.adminGetUser(any<AdminGetUserRequest>())).thenReturn(
            AdminGetUserResponse.builder().userAttributes(userType.attributes()).build()
        )

        userPoolService.deleteUser("user@example.com")

        verify(cognitoClient).adminDeleteUser(any<AdminDeleteUserRequest>())
    }

    @Test
    fun `deleteUser should throw for owner`() {
        val userType = createUser("owner@example.com", owner = true)
        whenever(cognitoClient.adminGetUser(any<AdminGetUserRequest>())).thenReturn(
            AdminGetUserResponse.builder().userAttributes(userType.attributes()).build()
        )

        val exception = assertThrows<DisplayException> {
            userPoolService.deleteUser("owner@example.com")
        }

        assertThat(exception.message).isEqualTo("Can not modify user")
        verify(cognitoClient, never()).adminDeleteUser(any<AdminDeleteUserRequest>())
    }

    @Test
    fun `enableOrDisableUser should enable user and reset password`() {
        val userType = createUser("user@example.com", owner = false)
        whenever(cognitoClient.adminGetUser(any<AdminGetUserRequest>())).thenReturn(
            AdminGetUserResponse.builder().userAttributes(userType.attributes()).build()
        )

        userPoolService.toggleUserAttribute("user@example.com", "login", "true")

        verify(cognitoClient).adminEnableUser(any<AdminEnableUserRequest>())
        verify(cognitoClient).adminSetUserPassword(any<AdminSetUserPasswordRequest>())
        verify(emailService).sendEmail(eq("no-reply@example.com"), eq("user@example.com"), eq("emails/reset"), any(), any(), any())
    }

    @Test
    fun `enableOrDisableUser should disable user`() {
        val userType = createUser("user@example.com", owner = false)
        whenever(cognitoClient.adminGetUser(any<AdminGetUserRequest>())).thenReturn(
            AdminGetUserResponse.builder().userAttributes(userType.attributes()).build()
        )

        userPoolService.toggleUserAttribute("user@example.com", "login", "false")

        verify(cognitoClient).adminDisableUser(any<AdminDisableUserRequest>())
        verify(cognitoClient, never()).adminResetUserPassword(any<AdminResetUserPasswordRequest>())
    }

    @Test
    fun `enableOrDisableUser should throw for owner`() {
        val userType = createUser("owner@example.com", owner = true)
        whenever(cognitoClient.adminGetUser(any<AdminGetUserRequest>())).thenReturn(
            AdminGetUserResponse.builder().userAttributes(userType.attributes()).build()
        )

        val exception = assertThrows<DisplayException> {
            userPoolService.toggleUserAttribute("owner@example.com", "login", "false")
        }

        assertThat(exception.message).isEqualTo("Can not modify user")
        verify(cognitoClient, never()).adminDisableUser(any<AdminDisableUserRequest>())
    }


    private fun createUserResponse(
        users: List<UserType>,
        nextPage: String? = null
    ) = ListUsersResponse.builder()
        .users(users)
        .paginationToken(nextPage)
        .build()

    private fun createUser(
        emailAddress: String,
        owner: Boolean = false,
        emailEnabled: Boolean = false,
        loginEnabled: Boolean = false
    ) = UserType.builder()
        .userCreateDate(Instant.now())
        .username(emailAddress)
        .enabled(loginEnabled)
        .attributes(
            AttributeType.builder()
                .name("custom:isOwner")
                .value("$owner")
                .build(),
            AttributeType.builder()
                .name("custom:emailEnabled")
                .value("$emailEnabled")
                .build(),
            AttributeType.builder()
                .name("email")
                .value(emailAddress)
                .build(),
        ).build()

    private fun createUserView(
        emailAddress: String,
        owner: Boolean = false,
        emailEnabled: Boolean = false,
        loginEnabled: Boolean = false
    ) = UserView(
        username = emailAddress,
        emailAddress = emailAddress,
        emailEnabled = emailEnabled,
        loginEnabled = loginEnabled,
        owner = owner
    )

}