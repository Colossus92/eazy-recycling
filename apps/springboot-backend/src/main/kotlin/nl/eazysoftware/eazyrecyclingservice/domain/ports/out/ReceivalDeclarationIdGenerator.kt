package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

/**
 * Port for generating unique 12-digit IDs for receival declarations.
 * Implementations should ensure thread-safe, monotonically increasing IDs.
 */
interface ReceivalDeclarationIdGenerator {
  fun nextId(): String
}
