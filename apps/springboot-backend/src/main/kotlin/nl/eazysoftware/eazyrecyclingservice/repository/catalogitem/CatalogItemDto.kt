package nl.eazysoftware.eazyrecyclingservice.repository.catalogitem

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "catalog_items")
data class CatalogItemDto(
  @Id
  @Column(name = "id")
  val id: UUID,

  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  val type: CatalogItemType,

  @Column(name = "code", nullable = false)
  val code: String,

  @Column(name = "name", nullable = false)
  val name: String,

  @Column(name = "unit_of_measure", nullable = false)
  val unitOfMeasure: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vat_code", nullable = false, referencedColumnName = "vat_code")
  val vatRate: VatRateDto,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  val category: CatalogItemCategoryDto?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "consignor_party_id", nullable = true, referencedColumnName = "id")
  val consignorParty: CompanyDto?,

  @Column(name = "default_price")
  val defaultPrice: BigDecimal?,

  @Column(name = "status", nullable = false)
  val status: String,

  @Column(name = "purchase_account_number")
  val purchaseAccountNumber: String?,

  @Column(name = "sales_account_number")
  val salesAccountNumber: String?,
) : AuditableEntity()
