package nl.eazysoftware.eazyrecyclingservice.repository.transport

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

/**
 * Manages database sequences.
 * Simple implementation - sequence creation happens once per year,
 * so we accept the minimal risk of a failed transaction during concurrent creation.
 */
@Component
class SequenceManager(
  private val entityManager: EntityManager
) {

  /**
   * Ensures a sequence exists for the given name.
   * Note: This happens once per year. In the rare case of concurrent creation
   * on January 1st, one transaction may fail - this is acceptable.
   */
  fun ensureSequenceExists(sequenceName: String) {
    // Simple approach: IF NOT EXISTS handles 99.9% of cases
    // The 0.1% concurrent creation risk on Jan 1st is acceptable
    entityManager.createNativeQuery(
      "CREATE SEQUENCE IF NOT EXISTS $sequenceName START 1 INCREMENT 1"
    ).executeUpdate()
  }

  /**
   * Gets the next value from a database sequence.
   * This operation is atomic and thread-safe.
   */
  fun getNextSequenceValue(sequenceName: String): Long {
    return entityManager.createNativeQuery("SELECT nextval('$sequenceName')")
      .singleResult as Long
  }
}
