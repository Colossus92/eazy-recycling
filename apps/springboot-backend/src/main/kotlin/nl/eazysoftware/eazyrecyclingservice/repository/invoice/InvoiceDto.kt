package nl.eazysoftware.eazyrecyclingservice.repository.invoice

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceDocumentType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceStatus
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceType
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.WeightTicketDto
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "invoices")
class InvoiceDto(
  @Id
  @Column(name = "id")
  val id: UUID,

  @Column(name = "invoice_number")
  val invoiceNumber: String?,

  @Enumerated(EnumType.STRING)
  @Column(name = "invoice_type", nullable = false)
  val invoiceType: InvoiceType,

  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false)
  val documentType: InvoiceDocumentType,

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  val status: InvoiceStatus,

  @Column(name = "invoice_date", nullable = false)
  val invoiceDate: LocalDate,

  @Column(name = "customer_company_id", nullable = false)
  val customerCompanyId: UUID,

  @Column(name = "customer_number")
  val customerNumber: String?,

  @Column(name = "customer_name", nullable = false)
  val customerName: String,

  @Column(name = "customer_street_name", nullable = false)
  val customerStreetName: String,

  @Column(name = "customer_building_number")
  val customerBuildingNumber: String?,

  @Column(name = "customer_building_number_addition")
  val customerBuildingNumberAddition: String?,

  @Column(name = "customer_postal_code", nullable = false)
  val customerPostalCode: String,

  @Column(name = "customer_city", nullable = false)
  val customerCity: String,

  @Column(name = "customer_country")
  val customerCountry: String?,

  @Column(name = "customer_vat_number")
  val customerVatNumber: String?,

  @Column(name = "original_invoice_id")
  val originalInvoiceId: UUID?,

  @Column(name = "credited_invoice_number")
  val creditedInvoiceNumber: String?,

  @OneToOne
  @JoinColumn(name = "source_weight_ticket_id", referencedColumnName = "id", nullable = true)
  val weightTicket: WeightTicketDto? = null,

  @OneToMany(mappedBy = "invoice", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  val lines: MutableList<InvoiceLineDto> = mutableListOf(),

  @Column(name = "finalized_at")
  val finalizedAt: Instant?,

  @Column(name = "finalized_by")
  val finalizedBy: String?,

  @Column(name = "pdf_url")
  var pdfUrl: String? = null,
) : AuditableEntity() {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is InvoiceLineDto) return false
      return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
