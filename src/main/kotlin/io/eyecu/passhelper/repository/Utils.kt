package io.eyecu.passhelper.repository

import io.eyecu.passhelper.web.DisplayException
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag
import java.util.Base64
import kotlin.reflect.KMutableProperty1

inline fun <reified T, reified D> KMutableProperty1<D, T>.attribute(vararg tags: StaticAttributeTag): StaticAttribute<D, T> =
    StaticAttribute
        .builder(D::class.java, T::class.java)
        .name(name)
        .getter(getter)
        .setter(setter)
        .tags(tags.toList())
        .build()

internal fun PassportId.passportId() =
    Base64.getUrlEncoder()
        .encodeToString("$name#$identifier".toByteArray(Charsets.UTF_8))

fun dynamoPartitionAndSortKey(id: String): Pair<PartitionKey, SortKey> = runCatching {
    Base64.getUrlDecoder()
        .decode(id)
        .decodeToString()
        .let {
            val (hash, range) = it.split("#")
            PartitionKey(hash) to SortKey(range)
        }
}.getOrElse {
    throw DisplayException("Invalid Passport ID")
}