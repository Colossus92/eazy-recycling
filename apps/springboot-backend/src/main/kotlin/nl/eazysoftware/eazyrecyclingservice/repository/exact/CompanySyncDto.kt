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

  @Column(name = "company_id", nullable = false)
  val companyId: UUID,

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

  /**
   * Flag indicating this record requires manual review before sync can proceed.
   */
  @Column(name = "requires_manual_review", nullable = false)
  val requiresManualReview: Boolean = false,

  @Column(name = "created_at", nullable = false)
  val createdAt: Instant = Instant.now(),

  @Column(name = "updated_at", nullable = true)
  var updatedAt: Instant? = null,
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
  PENDING_REVIEW
}
