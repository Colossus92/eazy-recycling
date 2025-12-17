package nl.eazysoftware.eazyrecyclingservice.repository.invoice

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "invoice_lines")
class InvoiceLineDto(
  @Id
  @Column(name = "id")
  val id: Long,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  val invoice: InvoiceDto,

  @Column(name = "line_number", nullable = false)
  val lineNumber: Int,

  @Column(name = "line_date", nullable = false)
  val lineDate: LocalDate,

  @Column(name = "description", nullable = false)
  val description: String,

  @Column(name = "order_reference")
  val orderReference: String?,

  @Column(name = "vat_code", nullable = false)
  val vatCode: String,

  @Column(name = "vat_percentage", nullable = false)
  val vatPercentage: BigDecimal,

  @Column(name = "gl_account_code")
  val glAccountCode: String?,

  @Column(name = "quantity", nullable = false)
  val quantity: BigDecimal,

  @Column(name = "unit_price", nullable = false)
  val unitPrice: BigDecimal,

  @Column(name = "total_excl_vat", nullable = false)
  val totalExclVat: BigDecimal,

  @Column(name = "unit_of_measure", nullable = false)
  val unitOfMeasure: String,

  @Column(name = "catalog_item_id")
  val catalogItemId: Long,

  @Column(name = "catalog_item_code", nullable = false)
  val catalogItemCode: String,

  @Column(name = "catalog_item_name", nullable = false)
  val catalogItemName: String,

  @Enumerated(EnumType.STRING)
  @Column(name = "catalog_item_type", nullable = false)
  val catalogItemType: CatalogItemType,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is InvoiceLineDto) return false
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
