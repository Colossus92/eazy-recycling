package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "weight_ticket_product_lines")
data class WeightTicketProductLineDto(
  @Id
  @Column(name = "id")
  val id: UUID,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "weight_ticket_id", nullable = false)
  val weightTicket: WeightTicketDto? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "catalog_item_id", nullable = false)
  val catalogItem: CatalogItemDto,

  @Column(name = "catalog_item_id", nullable = false, insertable = false, updatable = false)
  val catalogItemId: UUID,

  @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
  val quantity: BigDecimal,

  @Column(name = "unit", nullable = false)
  val unit: String,
)
