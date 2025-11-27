package nl.eazysoftware.eazyrecyclingservice.repository.exact

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "companies_sync")
data class CompanySyncDto(
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  val id: UUID? = null,

  /**
   * The company ID in Eazy Recycling.
   * Can be null for conflicts where no company could be created (e.g., domain invariant failures).
   */
  @Column(name = "company_id", nullable = true)
  val companyId: UUID? = null,

  /**
   * The Exact Online Code (auto-generated sequential number).
   * Used as secondary identifier for matching.
   */
  @Column(name = "external_id", nullable = true)
  val externalId: String? = null,

  /**
   * The Exact Online GUID (primary key in Exact).
   * This is the authoritative link between systems.
   */
  @Column(name = "exact_guid", nullable = true)
  val exactGuid: UUID? = null,

  @Column(name = "sync_status", nullable = false)
  @Enumerated(EnumType.STRING)
  val syncStatus: SyncStatus,

  @Column(name = "synced_from_source_at")
  val syncedFromSourceAt: Instant,

  @Column(name = "deleted_in_source")
  val deletedInSource: Boolean = false,

  @Column(name = "sync_error_message", nullable = true)
  val syncErrorMessage: String? = null,

  /**
   * JSON details about the conflict when sync_status is CONFLICT.
   * Contains information about the conflicting field and values.
   */
  @Column(name = "conflict_details", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  val conflictDetails: Map<String, Any>? = null,

  @Column(name = "created_at", nullable = false)
  val createdAt: Instant = Instant.now(),

  @Column(name = "updated_at", nullable = true)
  var updatedAt: Instant? = null,

  /**
   * Timestamp when the record was deleted in Exact Online.
   */
  @Column(name = "deleted_in_exact_at", nullable = true)
  val deletedInExactAt: Instant? = null,

  /**
   * User ID who deleted the record in Exact Online.
   */
  @Column(name = "deleted_in_exact_by", nullable = true)
  val deletedInExactBy: UUID? = null,

  /**
   * Timestamp when the record was deleted locally in Eazy Recycling.
   */
  @Column(name = "deleted_locally_at", nullable = true)
  val deletedLocallyAt: Instant? = null,
)

/**
 * Status of the synchronization between Eazy Recycling and Exact Online.
 */
enum class SyncStatus {
  /** Sync completed successfully */
  OK,
  /** Sync failed due to technical error */
  FAILED,
  /** Sync blocked due to data conflict requiring manual resolution */
  CONFLICT,
  /** Record matched via fuzzy matching and needs manual confirmation */
  PENDING_REVIEW,
  /** Record was deleted in Exact Online and soft-deleted locally */
  DELETED,
  /** Record was deleted locally but NOT propagated to Exact Online (by design) */
  DELETED_LOCALLY
}
