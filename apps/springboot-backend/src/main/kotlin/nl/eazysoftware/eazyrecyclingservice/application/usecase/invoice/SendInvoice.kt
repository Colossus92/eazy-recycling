package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.application.jobs.EdgeFunctionJobService
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.Invoice
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceStatus
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import org.jobrunr.scheduling.BackgroundJob
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface SendInvoice {
    fun handle(command: SendInvoiceCommand): InvoiceResult
}

data class SendInvoiceCommand(
    val invoiceId: InvoiceId,
    val to: String,
    val bcc: String?,
    val subject: String,
    val body: String,
)

@Service
class SendInvoiceService(
    private val invoices: Invoices,
    private val objectMapper: ObjectMapper,
) : SendInvoice {

    @Transactional
    override fun handle(command: SendInvoiceCommand): InvoiceResult {
        val invoice = invoices.findById(command.invoiceId)
            ?: throw IllegalArgumentException("Factuur niet gevonden")

        require(invoice.status == InvoiceStatus.FINAL) {
            "Alleen definitieve facturen kunnen worden verzonden"
        }

        require(invoice.pdfUrl != null) {
            "Factuur PDF moet eerst worden gegenereerd"
        }

        scheduleInvoiceEmail(invoice, command)

        return InvoiceResult(
            invoiceId = invoice.id.value
        )
    }

    private fun scheduleInvoiceEmail(invoice: Invoice, command: SendInvoiceCommand) {
        val payload = buildEmailPayload(invoice, command)
        val invoiceId = invoice.id.value.toString()

        BackgroundJob.enqueue<EdgeFunctionJobService> {
            it.executeSendEmailJob(invoiceId, payload)
        }
    }

    private fun buildEmailPayload(invoice: Invoice, command: SendInvoiceCommand): String {
        val pdfFileName = "factuur-${invoice.invoiceNumber?.value?.lowercase()}.pdf"

        val payload = mapOf(
            "to" to command.to,
            "bcc" to command.bcc,
            "subject" to command.subject,
            "body" to command.body,
            "attachmentFileName" to pdfFileName,
            "attachmentStorageBucket" to "invoices",
            "attachmentStoragePath" to invoice.pdfUrl,
        )

        return objectMapper.writeValueAsString(payload)
    }
}
