package io.eyecu.passhelper.util

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS
import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES
import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

val jacksonObjectMapper: ObjectMapper = JsonMapper.builder()
    .addModules(kotlinModule())
    .addModules(JavaTimeModule())
    .enable(ACCEPT_CASE_INSENSITIVE_PROPERTIES)
    .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
    .enable(ACCEPT_CASE_INSENSITIVE_VALUES)
    .disable(FAIL_ON_UNKNOWN_PROPERTIES)
    .build()

val templateEngine = TemplateEngine().apply {
    setTemplateResolver(
        ClassLoaderTemplateResolver().apply {
            templateMode = TemplateMode.HTML
            characterEncoding = "UTF-8"
            prefix = "/templates/"
            suffix = ".html"
        }
    )
}

typealias ValueMap = Map<String, String>

fun LocalDate.toTimestamp(): Long =
    atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

fun millisecondsToLocalDate(ms: Long): LocalDate =
    Instant.ofEpochMilli(ms)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()

fun secondsToLocalDate(seconds: Long): LocalDate =
    Instant.ofEpochSecond(seconds)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()


object QueryParamParser {
    fun parse(params: String): ValueMap = params.split("&")
        .mapNotNull { parameter ->
            val keyValue = parameter.split("=")
            val key = URLDecoder.decode(keyValue[0], "UTF-8")
            val value = if (keyValue.size > 1) {
                URLDecoder.decode(keyValue[1], "UTF-8")
            } else {
                return@mapNotNull null
            }
            key to value
        }.toMap()
}