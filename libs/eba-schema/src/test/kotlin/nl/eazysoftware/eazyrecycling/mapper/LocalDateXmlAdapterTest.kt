package nl.eazysoftware.eazyrecycling.mapper

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.test.assertEquals

class LocalDateXmlAdapterTest {

    private val adapter = LocalDateXmlAdapter()

    @Test
    fun `unmarshal should convert valid ISO date string to LocalDate`() {
        // Given
        val dateString = "2023-12-25"
        val expectedDate = LocalDate.of(2023, 12, 25)

        // When
        val result = adapter.unmarshal(dateString)

        // Then
        assertEquals(expectedDate, result)
    }

    @Test
    fun `unmarshal should handle different valid date formats`() {
        // Given
        val testCases = mapOf(
            "2023-01-01" to LocalDate.of(2023, 1, 1),
            "2023-12-31" to LocalDate.of(2023, 12, 31),
            "2024-02-29" to LocalDate.of(2024, 2, 29), // Leap year
            "1999-06-15" to LocalDate.of(1999, 6, 15)
        )

        // When & Then
        testCases.forEach { (dateString, expectedDate) ->
            val result = adapter.unmarshal(dateString)
            assertEquals(expectedDate, result, "Failed for date string: $dateString")
        }
    }

    @Test
    fun `unmarshal should throw DateTimeParseException for invalid date string`() {
        // Given
        val invalidDateStrings = listOf(
            "invalid-date",
            "2023-13-01", // Invalid month
            "2023-02-30", // Invalid day for February
            "2023/12/25", // Wrong format
            "25-12-2023", // Wrong format
            "",
            "2023-1-1" // Missing leading zeros
        )

        // When & Then
        invalidDateStrings.forEach { invalidDate ->
            assertThrows<DateTimeParseException>("Should throw for: $invalidDate") {
                adapter.unmarshal(invalidDate)
            }
        }
    }

    @Test
    fun `marshal should convert LocalDate to ISO date string`() {
        // Given
        val date = LocalDate.of(2023, 12, 25)
        val expectedString = "2023-12-25"

        // When
        val result = adapter.marshal(date)

        // Then
        assertEquals(expectedString, result)
    }

    @Test
    fun `marshal should handle different LocalDate values`() {
        // Given
        val testCases = mapOf(
            LocalDate.of(2023, 1, 1) to "2023-01-01",
            LocalDate.of(2023, 12, 31) to "2023-12-31",
            LocalDate.of(2024, 2, 29) to "2024-02-29", // Leap year
            LocalDate.of(1999, 6, 15) to "1999-06-15",
            LocalDate.MIN to "-999999999-01-01",
            LocalDate.MAX to "+999999999-12-31"
        )

        // When & Then
        testCases.forEach { (date, expectedString) ->
            val result = adapter.marshal(date)
            assertEquals(expectedString, result, "Failed for date: $date")
        }
    }

    @Test
    fun `marshal should handle null LocalDate`() {
        // Given
        val nullDate: LocalDate? = null

        // When
        val result = adapter.marshal(nullDate)

        // Then
        assertEquals("null", result)
    }

    @Test
    fun `roundtrip conversion should preserve original date`() {
        // Given
        val originalDates = listOf(
            LocalDate.of(2023, 12, 25),
            LocalDate.of(2024, 2, 29),
            LocalDate.of(1999, 1, 1),
            LocalDate.now()
        )

        // When & Then
        originalDates.forEach { originalDate ->
            val marshalled = adapter.marshal(originalDate)
            val unmarshalled = adapter.unmarshal(marshalled)
            assertEquals(originalDate, unmarshalled, "Roundtrip failed for: $originalDate")
        }
    }
}
