package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Year

/**
 * Integration test for TransportDisplayNumberGenerator using real PostgreSQL.
 * Tests sequence generation with actual database sequences.
 */
class TransportDisplayNumberGeneratorIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var generator: TransportDisplayNumberGenerator

  @Test
  fun `should generate sequential display numbers`() {
    // When - Generate multiple display numbers
    val displayNumber1 = generator.generateDisplayNumber()
    val displayNumber2 = generator.generateDisplayNumber()
    val displayNumber3 = generator.generateDisplayNumber()

    // Then - Should be sequential
    val currentYear = Year.now().value
    val yearPrefix = (currentYear % 100).toString().padStart(2, '0')

    assertTrue(displayNumber1.value.startsWith(yearPrefix))
    assertTrue(displayNumber2.value.startsWith(yearPrefix))
    assertTrue(displayNumber3.value.startsWith(yearPrefix))

    // Extract sequence numbers
    val seq1 = displayNumber1.value.substringAfter('-').toInt()
    val seq2 = displayNumber2.value.substringAfter('-').toInt()
    val seq3 = displayNumber3.value.substringAfter('-').toInt()

    assertEquals(seq1 + 1, seq2)
    assertEquals(seq2 + 1, seq3)
  }

  @Test
  fun `should format with 6 digits`() {
    // When
    val displayNumber = generator.generateDisplayNumber()

    // Then - Should match format YY-NNNNNN
    assertTrue(displayNumber.value.matches(Regex("\\d{2}-\\d{6}")))
  }


  @Test
  fun `should create sequence if it does not exist`() {
    // Given - Fresh generator (sequence may or may not exist)
    val currentYear = Year.now().value
    val yearPrefix = (currentYear % 100).toString().padStart(2, '0')

    // When - Generate a display number
    val displayNumber = generator.generateDisplayNumber()

    // Then - Should successfully generate
    assertNotNull(displayNumber)
    assertTrue(displayNumber.value.startsWith(yearPrefix))
    assertEquals(9, displayNumber.value.length) // YY-NNNNNN = 8 characters
  }
}
