package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceStatus
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface DeleteInvoice {
    fun handle(invoiceId: Long)
}

@Service
class DeleteInvoiceService(
    private val invoices: Invoices,
) : DeleteInvoice {

    @Transactional
    override fun handle(invoiceId: Long) {
        val invoice = invoices.findById(InvoiceId(invoiceId))
            ?: throw IllegalArgumentException("Factuur niet gevonden: $invoiceId")

        require(invoice.status == InvoiceStatus.DRAFT) {
            "Alleen concept facturen kunnen worden verwijderd."
        }

        invoices.delete(InvoiceId(invoiceId))
    }
}
