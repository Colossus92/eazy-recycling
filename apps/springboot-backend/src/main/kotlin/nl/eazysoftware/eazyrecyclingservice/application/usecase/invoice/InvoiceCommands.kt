package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceDocumentType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreateInvoiceCommand(
    val invoiceType: InvoiceType,
    val documentType: InvoiceDocumentType,
    val customerId: UUID,
    val invoiceDate: LocalDate,
    val originalInvoiceId: Long?,
    val lines: List<InvoiceLineCommand>,
)

data class UpdateInvoiceCommand(
    val invoiceId: Long,
    val invoiceDate: LocalDate,
    val lines: List<InvoiceLineCommand>,
)

data class InvoiceLineCommand(
    val date: LocalDate,
    val catalogItemId: Long,
    val description: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val orderReference: String?,
)

data class InvoiceResult(
    val invoiceId: Long
)
