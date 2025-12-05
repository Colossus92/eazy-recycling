package nl.eazysoftware.eazyrecyclingservice.repository.declaration

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "weight_ticket_declaration_snapshots")
data class WeightTicketDeclarationSnapshotDto(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  val id: Long = 0,

  @Column(name = "weight_ticket_id", nullable = false)
  val weightTicketId: Long,

  @Column(name = "weight_ticket_line_index", nullable = false)
  val weightTicketLineIndex: Int,

  @Column(name = "waste_stream_number", nullable = false)
  val wasteStreamNumber: String,

  @Column(name = "declared_weight_value", nullable = false)
  val declaredWeightValue: BigDecimal,

  @Column(name = "declaration_id", nullable = false)
  val declarationId: String,

  @Column(name = "declared_at", nullable = false)
  val declaredAt: Instant,

  @Column(name = "declaration_period", nullable = false)
  val declarationPeriod: String, // Format: MMYYYY
)
