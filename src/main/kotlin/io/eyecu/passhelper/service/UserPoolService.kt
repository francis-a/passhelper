package io.eyecu.passhelper.service

import io.eyecu.passhelper.models.UserView
import io.eyecu.passhelper.web.DisplayException
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType

class UserPoolService(
    private val emailService: EmailService,
    private val cognitoClient: CognitoIdentityProviderClient,
    private val userPoolId: String,
    private val domainName: String
) {

    companion object {
        private const val EMAIL_ENABLED_ATTRIBUTE = "custom:emailEnabled"
        private const val OWNER_ATTRIBUTE = "custom:isOwner"
        private const val EMAIL_ATTRIBUTE = "email"
    }

    private val userLoader = { paginationToken: String? ->
        cognitoClient.listUsers(
            ListUsersRequest.builder()
                .paginationToken(paginationToken)
                .userPoolId(userPoolId)
                .build()
        )
    }

    fun listAllUsers() = generateSequence(userLoader(null)) {
        it.paginationToken()?.let(userLoader)
    }.flatMap {
        it.users()
    }.toList()
        .sortedBy {
            it.userCreateDate().toEpochMilli()
        }.map {
            UserView(
                username = it.username(),
                emailAddress = it.emailAddress(),
                emailEnabled = it.hasAttributeSetToTrue(EMAIL_ENABLED_ATTRIBUTE),
                owner = it.hasAttributeSetToTrue(OWNER_ATTRIBUTE),
                loginEnabled = it.enabled()
            )
        }

    fun listAllUsersWithEmailEnabled() = listAllUsers()
        .filter {
            it.emailEnabled
        }

    fun createUser(email: String) {
        val result = cognitoClient.adminCreateUser(
            AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                .username(email)
                .messageAction(MessageActionType.SUPPRESS)
                .userAttributes(
                    AttributeType.builder()
                        .name(EMAIL_ATTRIBUTE)
                        .value(email)
                        .build(),
                    AttributeType.builder()
                        .name(EMAIL_ENABLED_ATTRIBUTE)
                        .value("false")
                        .build(),
                    AttributeType.builder()
                        .name("email_verified")
                        .value("true")
                        .build()
                ).build()
        )

        enableOrDisableUser(result.user().username(), false)
    }

    fun toggleUserAttribute(username: String, attribute: String, newValue: String) {
        val boolValue =
            newValue.toBooleanStrictOrNull() ?: throw DisplayException("Invalid user attribute value $newValue")

        when (attribute) {
            "email" -> enableOrDisableEmail(username, boolValue)
            "login" -> enableOrDisableUser(username, boolValue)
            else -> throw DisplayException("Invalid attribute $attribute")
        }
    }

    private fun enableOrDisableEmail(username: String, enabled: Boolean) {
        cognitoClient.adminUpdateUserAttributes(
            AdminUpdateUserAttributesRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .userAttributes(
                    AttributeType.builder()
                        .name(EMAIL_ENABLED_ATTRIBUTE)
                        .value(enabled.toString())
                        .build()
                ).build()
        )
    }

    private fun enableOrDisableUser(username: String, enabled: Boolean) {
        val user = denyIfOwner(username)

        if (enabled) {
            cognitoClient.adminEnableUser(
                AdminEnableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build()
            )

            val newPassword = generateRandomPassword()

            cognitoClient.adminSetUserPassword(
                AdminSetUserPasswordRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .permanent(false)
                    .password(newPassword)
                    .build()
            )

            emailService.sendEmail(
                to = user.userAttributes().emailAddress(),
                from = "no-reply@$domainName",
                template = "emails/reset",
                source = "Reset Password",
                subject = "PassHelper Temporary Password",
                content = mapOf(
                    "temporaryPassword" to newPassword,
                    "loginUrl" to "https://$domainName"
                )
            )
        } else {
            cognitoClient.adminDisableUser(
                AdminDisableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build()
            )
        }
    }

    private fun generateRandomPassword(length: Int = 14): String {
        val upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowerCase = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val specialChars = "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/`~"

        val allChars = upperCase + lowerCase + digits + specialChars

        val mandatory = listOf(
            upperCase.random(),
            lowerCase.random(),
            digits.random(),
            specialChars.random()
        )

        val remaining = List(length - mandatory.size) { allChars.random() }

        return (mandatory + remaining).shuffled().joinToString("")
    }

    fun deleteUser(username: String) {
        denyIfOwner(username)

        cognitoClient.adminDeleteUser(
            AdminDeleteUserRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .build()
        )
    }

    private fun denyIfOwner(username: String): AdminGetUserResponse {
        val user = cognitoClient.adminGetUser(
            AdminGetUserRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .build()
        )

        if (user.userAttributes().any {
                it.name() == OWNER_ATTRIBUTE && it.value().toBooleanStrictOrNull() == true
            }
        ) {
            throw DisplayException("Can not modify user")
        } else {
            return user
        }
    }

    private fun UserType.emailAddress() = attributes().emailAddress()

    private fun List<AttributeType>.emailAddress() = first {
        it.name() == "email"
    }.value()

    private fun UserType.hasAttributeSetToTrue(name: String) =
        attributes()
            .firstOrNull {
                it.name() == name
            }?.value()
            ?.toBoolean() == true

}