package nl.eazysoftware.eazyrecycling.mapper

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalTime
import java.time.format.DateTimeParseException
import kotlin.test.assertEquals

class LocalTimeXmlAdapterTest {

    private val adapter = LocalTimeXmlAdapter()

    @Test
    fun `unmarshal should convert valid ISO time string to LocalTime`() {
        // Given
        val timeString = "14:30:45"
        val expectedTime = LocalTime.of(14, 30, 45)

        // When
        val result = adapter.unmarshal(timeString)

        // Then
        assertEquals(expectedTime, result)
    }

    @Test
    fun `unmarshal should handle different valid time formats`() {
        // Given
        val testCases = mapOf(
            "00:00:00" to LocalTime.of(0, 0, 0),
            "12:00:00" to LocalTime.of(12, 0, 0),
            "23:59:59" to LocalTime.of(23, 59, 59),
            "09:15:30" to LocalTime.of(9, 15, 30),
            "14:30:45.123" to LocalTime.of(14, 30, 45, 123_000_000),
            "14:30:45.123456789" to LocalTime.of(14, 30, 45, 123_456_789),
            "14:30" to LocalTime.of(14, 30, 0) // Without seconds
        )

        // When & Then
        testCases.forEach { (timeString, expectedTime) ->
            val result = adapter.unmarshal(timeString)
            assertEquals(expectedTime, result, "Failed for time string: $timeString")
        }
    }

    @Test
    fun `unmarshal should throw DateTimeParseException for invalid time string`() {
        // Given
        val invalidTimeStrings = listOf(
            "invalid-time",
            "25:00:00", // Invalid hour
            "14:60:00", // Invalid minute
            "14:30:60", // Invalid second
            "14/30/45", // Wrong format
            "2:30:45 PM", // Wrong format
            "",
            "14:3:45", // Missing leading zero
            "14:30:45:123" // Too many colons
        )

        // When & Then
        invalidTimeStrings.forEach { invalidTime ->
            assertThrows<DateTimeParseException>("Should throw for: $invalidTime") {
                adapter.unmarshal(invalidTime)
            }
        }
    }

    @Test
    fun `marshal should convert LocalTime to ISO time string`() {
        // Given
        val time = LocalTime.of(14, 30, 45)
        val expectedString = "14:30:45"

        // When
        val result = adapter.marshal(time)

        // Then
        assertEquals(expectedString, result)
    }

    @Test
    fun `marshal should handle different LocalTime values`() {
        // Given
        val testCases = mapOf(
            LocalTime.of(0, 0, 0) to "00:00",
            LocalTime.of(12, 0, 0) to "12:00",
            LocalTime.of(23, 59, 59) to "23:59:59",
            LocalTime.of(9, 15, 30) to "09:15:30",
            LocalTime.of(14, 30, 45, 123_000_000) to "14:30:45.123",
            LocalTime.of(14, 30, 45, 123_456_789) to "14:30:45.123456789",
            LocalTime.MIN to "00:00",
            LocalTime.MAX to "23:59:59.999999999",
            LocalTime.MIDNIGHT to "00:00",
            LocalTime.NOON to "12:00"
        )

        // When & Then
        testCases.forEach { (time, expectedString) ->
            val result = adapter.marshal(time)
            assertEquals(expectedString, result, "Failed for time: $time")
        }
    }

    @Test
    fun `marshal should handle null LocalTime`() {
        // Given
        val nullTime: LocalTime? = null

        // When
        val result = adapter.marshal(nullTime)

        // Then
        assertEquals("null", result)
    }

    @Test
    fun `roundtrip conversion should preserve original time`() {
        // Given
        val originalTimes = listOf(
            LocalTime.of(14, 30, 45),
            LocalTime.of(0, 0, 0),
            LocalTime.of(23, 59, 59),
            LocalTime.of(12, 15, 30, 500_000_000),
            LocalTime.now()
        )

        // When & Then
        originalTimes.forEach { originalTime ->
            val marshalled = adapter.marshal(originalTime)
            val unmarshalled = adapter.unmarshal(marshalled)
            assertEquals(originalTime, unmarshalled, "Roundtrip failed for: $originalTime")
        }
    }

    @Test
    fun `unmarshal should handle time with nanoseconds precision`() {
        // Given
        val timeString = "14:30:45.123456789"
        val expectedTime = LocalTime.of(14, 30, 45, 123_456_789)

        // When
        val result = adapter.unmarshal(timeString)

        // Then
        assertEquals(expectedTime, result)
    }

    @Test
    fun `marshal should preserve nanoseconds when present`() {
        // Given
        val time = LocalTime.of(14, 30, 45, 123_456_789)
        val expectedString = "14:30:45.123456789"

        // When
        val result = adapter.marshal(time)

        // Then
        assertEquals(expectedString, result)
    }

    @Test
    fun `unmarshal should handle edge case times`() {
        // Given
        val testCases = mapOf(
            "00:00:00.000000001" to LocalTime.of(0, 0, 0, 1),
            "23:59:59.999999999" to LocalTime.of(23, 59, 59, 999_999_999)
        )

        // When & Then
        testCases.forEach { (timeString, expectedTime) ->
            val result = adapter.unmarshal(timeString)
            assertEquals(expectedTime, result, "Failed for edge case: $timeString")
        }
    }
}
