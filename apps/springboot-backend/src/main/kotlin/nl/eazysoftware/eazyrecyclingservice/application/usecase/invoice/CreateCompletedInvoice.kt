package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CreateCompletedInvoice {
    fun handle(cmd: CreateInvoiceCommand): InvoiceResult
}

@Service
class CreateCompletedInvoiceService(
    private val createInvoice: CreateInvoice,
    private val finalizeInvoice: FinalizeInvoice,
) : CreateCompletedInvoice {

    @Transactional
    override fun handle(cmd: CreateInvoiceCommand): InvoiceResult {
        val created = createInvoice.handle(cmd)
        return finalizeInvoice.handle(FinalizeInvoiceCommand(created.invoiceId))
    }
}
