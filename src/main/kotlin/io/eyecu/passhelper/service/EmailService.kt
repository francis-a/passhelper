package io.eyecu.passhelper.service

import io.eyecu.passhelper.util.templateEngine
import org.thymeleaf.context.Context
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.SendEmailRequest

class EmailService(
    private val sesClient: SesClient
) {

    fun sendEmail(
        from: String,
        to: String,
        template: String,
        source: String,
        subject: String,
        content: Map<String, Any>
    ) {
        val request = SendEmailRequest.builder()
            .source("\"PassHelper - $source\" <$from>")
            .destination(
                Destination.builder()
                    .toAddresses(to)
                    .build()
            ).message(
                Message.builder()
                    .subject {
                        it.data(subject).charset("UTF-8")
                    }.body { body ->
                        body.html {
                            it.data(toBody(template, content)).charset("UTF-8")
                        }
                    }.build()
            ).build()
        sesClient.sendEmail(request)
    }

    private fun toBody(template: String, content: Map<String, Any>): String {
        val context = Context()
        context.setVariables(content)
        return templateEngine.process(template, context)
    }
}