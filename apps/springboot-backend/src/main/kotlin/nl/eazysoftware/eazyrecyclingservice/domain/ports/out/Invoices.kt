package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.Invoice
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId

interface Invoices {
    fun nextId(): InvoiceId
    fun nextLineId(): Long
    fun nextInvoiceNumberSequence(year: Int): Long
    fun save(aggregate: Invoice): Invoice
    fun findById(id: InvoiceId): Invoice?
    fun findAll(): List<Invoice>
    fun delete(id: InvoiceId)
}
