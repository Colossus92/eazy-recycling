package nl.eazysoftware.eazyrecyclingservice.config.clock

import kotlinx.datetime.YearMonth
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class KotlinYearMonthExtensionsTest {

  @Test
  fun `toYearMonth should parse valid MMyyyy format strings`() {
    val testCases = mapOf(
      "012025" to YearMonth(2025, 1),
      "022025" to YearMonth(2025, 2),
      "032025" to YearMonth(2025, 3),
      "042025" to YearMonth(2025, 4),
      "052025" to YearMonth(2025, 5),
      "062025" to YearMonth(2025, 6),
      "072025" to YearMonth(2025, 7),
      "082025" to YearMonth(2025, 8),
      "092025" to YearMonth(2025, 9),
      "102025" to YearMonth(2025, 10),
      "112025" to YearMonth(2025, 11),
      "122025" to YearMonth(2025, 12)
    )

    testCases.forEach { (input, expected) ->
      val result = input.toYearMonth()
      assertEquals(expected, result, "Failed to parse: $input")
    }
  }

  @Test
  fun `toYearMonth should parse edge case years`() {
    assertEquals(YearMonth(2005, 1), "012005".toYearMonth())
    assertEquals(YearMonth(2099, 12), "122099".toYearMonth())
  }

  @Test
  fun `toYearMonth should throw exception for invalid length`() {
    val invalidInputs = listOf(
      "1120",      // Too short
      "11202",     // Too short
      "1120255",   // Too long
      "112025 ",   // Extra space
      " 112025"    // Leading space
    )

    invalidInputs.forEach { input ->
      val exception = assertThrows<IllegalArgumentException> {
        input.toYearMonth()
      }
      assertEquals("Period moet 6 cijfers bevatten (MMyyyy formaat), maar was: $input", exception.message)
    }
  }

  @Test
  fun `toYearMonth should throw exception for invalid month`() {
    val invalidMonths = listOf(
      "002025",  // Month 0
      "132025",  // Month 13
      "252025",  // Month 25
      "992025"   // Month 99
    )

    invalidMonths.forEach { input ->
      val exception = assertThrows<IllegalArgumentException> {
        input.toYearMonth()
      }
      assert(exception.message!!.contains("Maand moet tussen 1 en 12 zijn"))
    }
  }

  @Test
  fun `toYearMonth should throw exception for non-numeric month`() {
    val exception = assertThrows<IllegalArgumentException> {
      "AB2025".toYearMonth()
    }
    assertEquals("Ongeldige maand in periode: AB2025", exception.message)
  }

  @Test
  fun `toYearMonth should throw exception for non-numeric year`() {
    val exception = assertThrows<IllegalArgumentException> {
      "01ABCD".toYearMonth()
    }
    assertEquals("Ongeldig jaar in periode: 01ABCD", exception.message)
  }

  @Test
  fun `toYearMonth should throw exception for year before 2005`() {
    val invalidYears = listOf(
      "012004",
      "012000",
      "011999"
    )

    invalidYears.forEach { input ->
      val exception = assertThrows<IllegalArgumentException> {
        input.toYearMonth()
      }
      assert(exception.message!!.contains("Jaar moet in of na 2005 zijn"))
    }
  }

  @Test
  fun `toLmaPeriod should format YearMonth to MMyyyy format`() {
    val testCases = mapOf(
      YearMonth(2025, 1) to "012025",
      YearMonth(2025, 2) to "022025",
      YearMonth(2025, 3) to "032025",
      YearMonth(2025, 4) to "042025",
      YearMonth(2025, 5) to "052025",
      YearMonth(2025, 6) to "062025",
      YearMonth(2025, 7) to "072025",
      YearMonth(2025, 8) to "082025",
      YearMonth(2025, 9) to "092025",
      YearMonth(2025, 10) to "102025",
      YearMonth(2025, 11) to "112025",
      YearMonth(2025, 12) to "122025"
    )

    testCases.forEach { (input, expected) ->
      val result = input.toLmaPeriod()
      assertEquals(expected, result, "Failed to format: $input")
    }
  }

  @Test
  fun `toLmaPeriod should format edge case years`() {
    assertEquals("012005", YearMonth(2005, 1).toLmaPeriod())
    assertEquals("122099", YearMonth(2099, 12).toLmaPeriod())
  }

  @Test
  fun `toYearMonth and toLmaPeriod should be reversible`() {
    val testCases = listOf(
      "012025",
      "062024",
      "122023",
      "112025"
    )

    testCases.forEach { original ->
      val yearMonth = original.toYearMonth()
      val backToString = yearMonth.toLmaPeriod()
      assertEquals(original, backToString, "Round-trip failed for: $original")
    }
  }

  @Test
  fun `toYearMonth should handle the specific error case from production`() {
    // This is the exact case that caused the production error
    val result = "112025".toYearMonth()
    assertEquals(YearMonth(2025, 11), result)
    assertEquals(2025, result.year)
  }
}
