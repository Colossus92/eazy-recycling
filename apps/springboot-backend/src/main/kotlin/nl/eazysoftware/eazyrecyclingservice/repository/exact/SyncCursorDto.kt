package nl.eazysoftware.eazyrecyclingservice.repository.exact

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "companies_sync_cursor")
data class SyncCursorDto(
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  val id: UUID? = null,

  @Column(name = "entity", nullable = false)
  val entity: String,

  @Column(name = "last_timestamp", nullable = false)
  val lastTimestamp: Long,

  @Column(name = "updated_at", nullable = false)
  val updatedAt: Instant = Instant.now(),
)
