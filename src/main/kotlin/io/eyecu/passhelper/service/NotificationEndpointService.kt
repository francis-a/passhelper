package io.eyecu.passhelper.service

import io.eyecu.passhelper.models.NotificationEndpointView
import io.eyecu.passhelper.repository.NotificationEndpointRepository
import java.util.Base64
import kotlin.text.Charsets.UTF_8

class NotificationEndpointService(
    private val notificationEndpointRepository: NotificationEndpointRepository
) {

    fun findAllEmails() = notificationEndpointRepository.findAllEmails().map {
        NotificationEndpointView(
            id = toId(it),
            email = it
        )
    }

    fun addEmail(email: String) = with(email.lowercase()) {
        notificationEndpointRepository.addEmail(this)
    }

    fun deleteEmail(id: String) = with(fromId(id)) {
        notificationEndpointRepository.deleteEmail(this)
    }

    private fun fromId(emailId: String) =
        String(Base64.getUrlDecoder().decode(emailId), charset = UTF_8)

    private fun toId(email: String) =
        Base64.getUrlEncoder().encodeToString(email.toByteArray())

}
