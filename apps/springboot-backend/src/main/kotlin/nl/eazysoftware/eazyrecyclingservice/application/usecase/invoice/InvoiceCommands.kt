package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceDocumentType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceType
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class CreateInvoiceCommand(
    val invoiceType: InvoiceType,
    val documentType: InvoiceDocumentType,
    val customerId: UUID,
    val invoiceDate: LocalDate,
    val originalInvoiceId: UUID?,
    val creditedInvoiceNumber: String? = null,
    val sourceWeightTicketId: WeightTicketId? = null,
    val lines: List<InvoiceLineCommand>,
)

data class UpdateInvoiceCommand(
    val invoiceId: UUID,
    val invoiceDate: LocalDate,
    val lines: List<InvoiceLineCommand>,
)

data class InvoiceLineCommand(
    val id: UUID? = null,
    val date: LocalDate,
    val catalogItemId: UUID,
    val description: String? = null,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val orderReference: String?,
)

data class InvoiceResult(
    val invoiceId: UUID
)
