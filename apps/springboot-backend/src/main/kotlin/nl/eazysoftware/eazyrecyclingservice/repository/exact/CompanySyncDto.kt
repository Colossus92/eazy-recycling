package nl.eazysoftware.eazyrecyclingservice.repository.exact

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "companies_sync")
data class CompanySyncDto(
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  val id: UUID? = null,

  @Column(name = "company_id", nullable = false)
  val companyId: UUID,

  @Column(name = "external_id", nullable = true)
  val externalId: String? = null,

  @Column(name = "sync_status", nullable = false)
  @Enumerated(EnumType.STRING)
  val syncStatus: SyncStatus,

  @Column(name = "synced_from_source_at")
  val syncedFromSourceAt: Instant,

  @Column(name = "deleted_in_source")
  val deletedInSource: Boolean = false,

  @Column(name = "sync_error_message", nullable = true)
  val syncErrorMessage: String? = null,

  @Column(name = "created_at", nullable = false)
  val createdAt: Instant = Instant.now(),

  @Column(name = "updated_at", nullable = true)
  var updatedAt: Instant? = null,
)

enum class SyncStatus {
  OK,
  FAILED
}
