package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import nl.eazysoftware.eazyrecyclingservice.application.jobs.EdgeFunctionJobService
import nl.eazysoftware.eazyrecyclingservice.application.service.InvoicePdfPayloadBuilder
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.Invoice
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import org.jobrunr.scheduling.BackgroundJob
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface FinalizeInvoice {
    fun handle(cmd: FinalizeInvoiceCommand): InvoiceResult
}

data class FinalizeInvoiceCommand(
    val invoiceId: UUID,
)

@Service
class FinalizeInvoiceService(
    private val invoices: Invoices,
    private val invoicePdfPayloadBuilder: InvoicePdfPayloadBuilder,
) : FinalizeInvoice {

    @Transactional
    override fun handle(cmd: FinalizeInvoiceCommand): InvoiceResult {
        val invoice = invoices.findById(InvoiceId(cmd.invoiceId))
            ?: throw IllegalArgumentException("Factuur niet gevonden: ${cmd.invoiceId}")

        val invoiceNumber = generateInvoiceNumber(invoice.invoiceDate.year)
        invoice.finalize(invoiceNumber, null)

        val saved = invoices.save(invoice)

        scheduleInvoicePdfGeneration(saved)

        return InvoiceResult(invoiceId = saved.id.value)
    }

    private fun generateInvoiceNumber(year: Int): InvoiceNumber {
        val sequence = invoices.nextInvoiceNumberSequence(year)
        return InvoiceNumber.generate("ER", year, sequence)
    }

    private fun scheduleInvoicePdfGeneration(invoice: Invoice) {
        val payload = invoicePdfPayloadBuilder.buildPayload(invoice)
        val invoiceId = invoice.id.value.toString()

        BackgroundJob.enqueue<EdgeFunctionJobService> {
            it.executeGeneratePdfJob(invoiceId, payload)
        }
    }
}
