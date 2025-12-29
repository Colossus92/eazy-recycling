package nl.eazysoftware.eazyrecyclingservice.repository.invoice

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.Invoice
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceLineId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

interface InvoiceJpaRepository : JpaRepository<InvoiceDto, UUID>

@Repository
class InvoiceRepository(
    private val jpaRepository: InvoiceJpaRepository,
    private val mapper: InvoiceMapper,
    private val entityManager: EntityManager,
) : Invoices {

    override fun nextId(): InvoiceId {
        return InvoiceId(UUID.randomUUID())
    }

    override fun nextLineId(): InvoiceLineId {
        return InvoiceLineId(UUID.randomUUID())
    }

    override fun nextInvoiceNumberSequence(year: Int): Long {
        val sql = """
            INSERT INTO invoice_number_sequences (year, last_sequence)
            VALUES (:year, 1)
            ON CONFLICT (year)
            DO UPDATE SET last_sequence = invoice_number_sequences.last_sequence + 1
            RETURNING last_sequence
        """
        return entityManager.createNativeQuery(sql)
            .setParameter("year", year)
            .singleResult as Long
    }

    override fun save(aggregate: Invoice): Invoice {
        val dto = mapper.toDto(aggregate)
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun findById(id: InvoiceId): Invoice? {
        return jpaRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun findAll(): List<Invoice> {
        return jpaRepository.findAll().map { mapper.toDomain(it) }
    }

    override fun delete(id: InvoiceId) {
        jpaRepository.deleteById(id.value)
    }
}
