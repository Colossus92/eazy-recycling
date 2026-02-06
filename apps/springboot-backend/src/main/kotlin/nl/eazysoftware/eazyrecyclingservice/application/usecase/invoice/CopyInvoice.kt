package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*
import kotlin.time.Clock

interface CopyInvoice {
    fun handle(cmd: CopyInvoiceCommand): InvoiceResult
}

/**
 * Command for copying an invoice.
 * It will create a new invoice with the same data as the original,
 * but with a new ID, no invoice number, and status set to DRAFT.
 * The invoice date will be set to today.
 */
data class CopyInvoiceCommand(
    val originalInvoiceId: UUID,
)

@Service
class CopyInvoiceService(
    private val invoices: Invoices,
) : CopyInvoice {

    @Transactional
    override fun handle(cmd: CopyInvoiceCommand): InvoiceResult {
        // 1. Load original invoice
        val originalInvoice = invoices.findById(InvoiceId(cmd.originalInvoiceId))
            ?: throw EntityNotFoundException("Factuur met ID ${cmd.originalInvoiceId} bestaat niet")

        // 2. Create new invoice ID and line IDs
        val newInvoiceId = invoices.nextId()
        val copiedLines = originalInvoice.lines.mapIndexed { index, line ->
            InvoiceLine(
                id = invoices.nextLineId(),
                lineNumber = index + 1,
                date = LocalDate.now(),
                description = line.description,
                orderReference = line.orderReference,
                vatCode = line.vatCode,
                vatPercentage = line.vatPercentage,
                isReverseCharge = line.isReverseCharge,
                glAccountCode = line.glAccountCode,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                totalExclVat = line.totalExclVat,
                catalogItemId = line.catalogItemId,
                catalogItemCode = line.catalogItemCode,
                catalogItemName = line.catalogItemName,
                catalogItemType = line.catalogItemType,
                unitOfMeasure = line.unitOfMeasure,
            )
        }.toMutableList()

        // 3. Create new invoice as DRAFT with today's date
        val copiedInvoice = Invoice(
            id = newInvoiceId,
            invoiceNumber = null,
            invoiceType = originalInvoice.invoiceType,
            documentType = InvoiceDocumentType.INVOICE,
            status = InvoiceStatus.DRAFT,
            invoiceDate = LocalDate.now(),
            customerSnapshot = originalInvoice.customerSnapshot,
            originalInvoiceId = null,
            creditedInvoiceNumber = null,
            sourceWeightTicketId = null,
            lines = copiedLines,
            createdAt = Clock.System.now(),
            createdBy = null,
            updatedAt = null,
            updatedBy = null,
            finalizedAt = null,
            finalizedBy = null,
        )

        // 4. Persist new invoice
        val savedInvoice = invoices.save(copiedInvoice)

        return InvoiceResult(invoiceId = savedInvoice.id.value)
    }
}
