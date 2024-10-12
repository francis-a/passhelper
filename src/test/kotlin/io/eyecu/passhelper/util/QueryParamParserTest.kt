package io.eyecu.passhelper.util

import io.eyecu.passhelper.util.QueryParamParser.parse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class QueryParamParserTest {

    @Test
    fun `parse form url`() {
        val urlEncodedBody = "name=John&age=25&profile=500"
        val expectedParameters = mapOf(
            "name" to "John",
            "age" to "25",
            "profile" to "500"
        )

        val parameters = parse(urlEncodedBody)

        assertThat(parameters).isEqualTo(expectedParameters)
    }

    @Test
    fun `parse form url empty body`() {
        val urlEncodedBody = ""
        val expectedParameters = emptyMap<String, List<String>>()

        val parameters = parse(urlEncodedBody)

        assertThat(parameters).isEqualTo(expectedParameters)
    }

    @Test
    fun `parse form url with empty values`() {
        val urlEncodedBody = "name=&age=&profile="
        val expectedParameters = mapOf(
            "name" to "",
            "age" to "",
            "profile" to ""
        )

        val parameters = parse(urlEncodedBody)

        assertThat(parameters).isEqualTo(expectedParameters)
    }

    @Test
    fun `parse form url with missing values`() {
        val urlEncodedBody = "name=John&age=25&profile="
        val expectedParameters = mapOf(
            "name" to "John",
            "age" to "25",
            "profile" to ""
        )

        val parameters = parse(urlEncodedBody)

        assertThat(parameters).isEqualTo(expectedParameters)
    }

    @Test
    fun `should decode values`() {
        val urlEncodedBody = "name=John%20Doe&age=30%2B5&city=New%20York"
        val expectedParameters = mapOf(
            "name" to "John Doe",
            "age" to "30+5",
            "city" to "New York"
        )

        val parameters = parse(urlEncodedBody)

        assertThat(parameters).isEqualTo(expectedParameters)
    }

    @Test
    fun `should decode keys`() {
        val urlEncodedBody = "first%20name=John&last%20name=Doe"
        val expectedParameters = mapOf(
            "first name" to "John",
            "last name" to "Doe"
        )

        val parameters = parse(urlEncodedBody)

        assertThat(parameters).isEqualTo(expectedParameters)

    }
}