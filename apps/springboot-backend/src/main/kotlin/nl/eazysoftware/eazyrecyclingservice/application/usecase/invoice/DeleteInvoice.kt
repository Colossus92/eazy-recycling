package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceStatus
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface DeleteInvoice {
    fun handle(invoiceId: UUID)
}

@Service
class DeleteInvoiceService(
    private val invoices: Invoices,
) : DeleteInvoice {

    @Transactional
    override fun handle(invoiceId: UUID) {
        val invoice = invoices.findById(InvoiceId(invoiceId))
            ?: throw IllegalArgumentException("Factuur niet gevonden: $invoiceId")

        require(invoice.status == InvoiceStatus.DRAFT) {
            "Alleen concept facturen kunnen worden verwijderd."
        }

        invoices.delete(InvoiceId(invoiceId))
    }
}
