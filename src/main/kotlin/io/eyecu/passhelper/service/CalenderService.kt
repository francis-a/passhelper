package io.eyecu.passhelper.service

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VAlarm
import biweekly.component.VEvent
import biweekly.property.Action
import biweekly.property.Trigger
import io.eyecu.passhelper.models.PassportView
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Date
import java.util.UUID

class CalenderService(
    private val icsBucket: String,
    private val passportService: PassportService,
    private val awsS3: S3Client,
    private val s3Presigner: S3Presigner = S3Presigner.create()

) {

    fun createPassportExpirationCalender(): String = with(ICalendar()) {
        setUid(UUID.randomUUID().toString())

        passportService.findAll().forEach {
            val expiresEvent = passportExpiresEvent(it)
            addEvent(expiresEvent)
            val willExpireEvent = passportWillExpireEvent(it, expiresEvent)
            addEvent(willExpireEvent)
        }

        saveToS3()
    }

    private fun ICalendar.saveToS3(): String {
        val passportCalender = Biweekly.write(this).go()
        awsS3.putObject(
            PutObjectRequest
                .builder()
                .bucket(icsBucket)
                .key(s3Key())
                .contentLength(passportCalender.toByteArray().size.toLong())
                .contentType("text/calendar")
                .build(),
            RequestBody.fromString(passportCalender)
        )

        return s3Presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(1))
                .getObjectRequest(
                    GetObjectRequest.builder()
                        .bucket(icsBucket)
                        .key(s3Key())
                        .build()
                ).build()
        ).url().toExternalForm()
    }

    private fun ICalendar.s3Key() = "ics/${uid.value}/reminders.ics"

    private fun passportWillExpireEvent(passport: PassportView, expiresEvent: VEvent) = VEvent().apply {
        val eventDate = passport.expiresDate.minusMonths(WARN_BEFORE_EXPIRATION_MONTHS)
        setUid(passport.eventId(eventDate))
        setSummary("${passport.formatName()} passport issued by ${passport.countryName} is expiring soon")
        setDescription("${passport.formatName()} passport issued by ${passport.countryName} will expire on ${passport.expiresDate.format()}")
        setDateStart(eventDate.toDate(), false)
        addRelatedTo(expiresEvent.uid.value)
    }

    private fun passportExpiresEvent(passport: PassportView) = VEvent().apply {
        val eventDate = passport.expiresDate
        setUid(passport.eventId(eventDate))
        setSummary("${passport.formatName()} passport issued by ${passport.countryName} expires today")
        setDescription("${passport.formatName()} passport issued by ${passport.countryName} expires today")
        setDateStart(eventDate.toDate(), false)

        val alarmDate = passport.expiresDate.minusMonths(WARN_BEFORE_EXPIRATION_MONTHS / 2)

        if (alarmDate.isBefore(LocalDate.now())) {
            addAlarm(
                VAlarm(
                    Action.display(),
                    Trigger(alarmDate.toDate())
                )
            )
        }
    }

    private fun PassportView.formatName() = with(this.fullName) {
        this + if (endsWith("s")) {
            "'"
        } else {
            "'s"
        }
    }

    private fun LocalDate.toDate() = Date.from(atStartOfDay().toInstant(ZoneOffset.UTC))

    private fun LocalDate.format() = eventDateFormatter.format(this)

    private fun PassportView.eventId(notificationDate: LocalDate) =
        Base64.getEncoder().encodeToString("$fullName-$notificationDate-$expiresDate-$countryCode".encodeToByteArray())

}

private val eventDateFormatter = DateTimeFormatter.ofPattern("EEEE, LLLL d yyyy")