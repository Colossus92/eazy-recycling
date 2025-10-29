package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.repository.transport.SequenceManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.Year

@ExtendWith(MockitoExtension::class)
class TransportDisplayNumberGeneratorTest {

  @Mock
  private lateinit var sequenceManager: SequenceManager

  @InjectMocks
  private lateinit var generator: TransportDisplayNumberGenerator

  @Test
  fun `should generate first display number for year`() {
    // Given
    val currentYear = Year.now().value
    val yearPrefix = (currentYear % 100).toString().padStart(2, '0')
    val sequenceName = "transport_seq_$yearPrefix"

    whenever(sequenceManager.getNextSequenceValue(sequenceName)).thenReturn(1L)

    // When
    val displayNumber = generator.generateDisplayNumber()

    // Then
    assertEquals("$yearPrefix-000001", displayNumber.value)
    verify(sequenceManager).ensureSequenceExists(sequenceName)
    verify(sequenceManager).getNextSequenceValue(sequenceName)
  }

  @Test
  fun `should generate sequential display number`() {
    // Given
    val currentYear = Year.now().value
    val yearPrefix = (currentYear % 100).toString().padStart(2, '0')
    val sequenceName = "transport_seq_$yearPrefix"

    whenever(sequenceManager.getNextSequenceValue(sequenceName)).thenReturn(43L)

    // When
    val displayNumber = generator.generateDisplayNumber()

    // Then
    assertEquals("$yearPrefix-000043", displayNumber.value)
  }

  @Test
  fun `should pad sequence number with leading zeros`() {
    // Given
    val currentYear = Year.now().value
    val yearPrefix = (currentYear % 100).toString().padStart(2, '0')
    val sequenceName = "transport_seq_$yearPrefix"

    whenever(sequenceManager.getNextSequenceValue(sequenceName)).thenReturn(10L)

    // When
    val displayNumber = generator.generateDisplayNumber()

    // Then
    assertEquals("$yearPrefix-000010", displayNumber.value)
  }

  @Test
  fun `should handle large sequence numbers`() {
    // Given
    val currentYear = Year.now().value
    val yearPrefix = (currentYear % 100).toString().padStart(2, '0')
    val sequenceName = "transport_seq_$yearPrefix"

    whenever(sequenceManager.getNextSequenceValue(sequenceName)).thenReturn(1000000L)

    // When
    val displayNumber = generator.generateDisplayNumber()

    // Then
    assertEquals("$yearPrefix-1000000", displayNumber.value)
  }

  @Test
  fun `should format display number with exactly 6 digits for small numbers`() {
    // Given
    val currentYear = Year.now().value
    val yearPrefix = (currentYear % 100).toString().padStart(2, '0')
    val sequenceName = "transport_seq_$yearPrefix"

    whenever(sequenceManager.getNextSequenceValue(sequenceName)).thenReturn(2L)

    // When
    val displayNumber = generator.generateDisplayNumber()

    // Then
    assertTrue(displayNumber.value.matches(Regex("\\d{2}-\\d{6}")))
    assertEquals("$yearPrefix-000002", displayNumber.value)
  }
}
