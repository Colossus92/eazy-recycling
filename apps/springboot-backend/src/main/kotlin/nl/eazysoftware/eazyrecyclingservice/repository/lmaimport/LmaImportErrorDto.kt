package nl.eazysoftware.eazyrecyclingservice.repository.lmaimport

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "lma_import_errors")
data class LmaImportErrorDto(
  @Id
  @Column(name = "id")
  val id: UUID,

  @Column(name = "import_batch_id", nullable = false)
  val importBatchId: UUID,

  @Column(name = "row_number", nullable = false)
  val rowNumber: Int,

  @Column(name = "waste_stream_number")
  val wasteStreamNumber: String?,

  @Column(name = "error_code", nullable = false)
  val errorCode: String,

  @Column(name = "error_message", nullable = false)
  val errorMessage: String,

  @Column(name = "raw_data", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  val rawData: Map<String, String>?,

  @Column(name = "created_at", nullable = false)
  val createdAt: Instant,

  @Column(name = "resolved_at")
  val resolvedAt: Instant?,

  @Column(name = "resolved_by")
  val resolvedBy: String?
)
