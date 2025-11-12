package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ReceivalDeclarationIdGenerator
import org.springframework.stereotype.Repository

@Repository
class PostgresReceivalDeclarationIdGenerator(
  private val entityManager: EntityManager
) : ReceivalDeclarationIdGenerator {

  override fun nextId(): String {
    val query = entityManager.createNativeQuery("SELECT nextval('receival_declaration_id_seq')")
    val nextValue = query.singleResult as Long
    return nextValue.toString().padStart(12, '0')
  }
}
