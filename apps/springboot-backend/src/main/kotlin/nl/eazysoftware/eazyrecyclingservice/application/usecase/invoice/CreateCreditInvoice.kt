package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*
import kotlin.time.Clock

interface CreateCreditInvoice {
    fun handle(cmd: CreateCreditInvoiceCommand): InvoiceResult
}

data class CreateCreditInvoiceCommand(
    val originalInvoiceId: UUID,
    val invoiceDate: LocalDate,
)

@Service
class CreateCreditInvoiceService(
    private val invoices: Invoices,
) : CreateCreditInvoice {

    @Transactional
    override fun handle(cmd: CreateCreditInvoiceCommand): InvoiceResult {
        val originalInvoice = invoices.findById(InvoiceId(cmd.originalInvoiceId))
            ?: throw IllegalArgumentException("Originele factuur niet gevonden: ${cmd.originalInvoiceId}")

        require(originalInvoice.documentType == InvoiceDocumentType.INVOICE) {
            "Alleen facturen kunnen worden gecrediteerd, geen creditnota's"
        }

        require(originalInvoice.status != InvoiceStatus.DRAFT) {
            "Alleen definitieve of verzonden facturen kunnen worden gecrediteerd"
        }

        require(originalInvoice.invoiceNumber != null) {
            "Originele factuur heeft geen factuurnummer"
        }

        val creditInvoiceId = invoices.nextId()

        val creditLines = originalInvoice.lines.mapIndexed { index, line ->
            InvoiceLine(
                id = invoices.nextLineId(),
                lineNumber = index + 1,
                date = cmd.invoiceDate,
                description = line.description,
                orderReference = line.orderReference,
                vatCode = line.vatCode,
                vatPercentage = line.vatPercentage,
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

        val creditInvoice = Invoice(
            id = creditInvoiceId,
            invoiceNumber = null,
            invoiceType = originalInvoice.invoiceType,
            documentType = InvoiceDocumentType.CREDIT_NOTE,
            status = InvoiceStatus.DRAFT,
            invoiceDate = cmd.invoiceDate,
            customerSnapshot = originalInvoice.customerSnapshot,
            originalInvoiceId = InvoiceId(cmd.originalInvoiceId),
            creditedInvoiceNumber = originalInvoice.invoiceNumber!!.value,
            sourceWeightTicketId = null,
            lines = creditLines,
            createdAt = Clock.System.now(),
            createdBy = null,
            updatedAt = null,
            updatedBy = null,
            finalizedAt = null,
            finalizedBy = null,
        )

        val saved = invoices.save(creditInvoice)
        return InvoiceResult(invoiceId = saved.id.value)
    }
}
