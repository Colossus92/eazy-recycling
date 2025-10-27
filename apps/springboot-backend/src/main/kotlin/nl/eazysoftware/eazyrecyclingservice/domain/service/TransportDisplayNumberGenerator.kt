package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportDisplayNumber
import nl.eazysoftware.eazyrecyclingservice.repository.transport.SequenceManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Year

/**
 * Domain service responsible for generating display numbers for transports.
 * Display numbers follow the format: YY-NNNNNN where:
 * - YY is the 2-digit year
 * - NNNNNN is a 6-digit sequence number with leading zeros
 *
 * The sequence resets each calendar year, starting from 1.
 * Uses database sequences for thread-safe concurrent number generation.
 */
@Service
class TransportDisplayNumberGenerator(
  private val sequenceManager: SequenceManager
) {

  /**
   * Generates a new display number for the current year.
   * This method is thread-safe using database sequences to prevent duplicate numbers
   * during concurrent transport creation.
   *
   * @return A new TransportDisplayNumber with the next sequence number for the current year
   */
  @Transactional
  fun generateDisplayNumber(): TransportDisplayNumber {
    val currentYear = Year.now().value
    val yearPrefix = (currentYear % 100).toString().padStart(2, '0')

    // Ensure sequence exists for current year
    val sequenceName = "transport_seq_$yearPrefix"
    sequenceManager.ensureSequenceExists(sequenceName)

    // Get next value from sequence (thread-safe)
    val nextSequence = sequenceManager.getNextSequenceValue(sequenceName)

    val displayNumber = formatDisplayNumber(yearPrefix, nextSequence)
    return TransportDisplayNumber(displayNumber)
  }

  /**
   * Formats a display number according to the YY-NNNNNN format.
   *
   * @param yearPrefix The 2-digit year prefix
   * @param sequence The sequence number
   * @return Formatted display number string
   */
  private fun formatDisplayNumber(yearPrefix: String, sequence: Long): String {
    val sequenceFormatted = sequence.toString().padStart(6, '0')
    return "$yearPrefix-$sequenceFormatted"
  }
}
