package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "lma_declarations")
data class LmaDeclarationDto(
  @Id
  @Column(name = "id")
  val id: String,

  @Column(name = "amice_uuid", unique = true, nullable = true)
  val amiceUUID: UUID? = null,

  @Column(name = "waste_stream_number", nullable = false)
  val wasteStreamNumber: String,

  @Column(name = "period", nullable = false)
  val period: String,

  @Column(name = "transporters", nullable = false, columnDefinition = "text[]")
  val transporters: List<String>,

  @Column(name = "total_weight", nullable = false)
  val totalWeight: Long,

  @Column(name = "total_shipments", nullable = false)
  val totalShipments: Long,

  @Column(name = "created_at", nullable = false)
  val createdAt: Instant,

  @Column(name = "errors", nullable = true, columnDefinition = "text[]")
  val errors: List<String>? = null,

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  val status: Status,
) {

  enum class Status {
    PENDING,
    COMPLETED,
    FAILED,
  }
}
