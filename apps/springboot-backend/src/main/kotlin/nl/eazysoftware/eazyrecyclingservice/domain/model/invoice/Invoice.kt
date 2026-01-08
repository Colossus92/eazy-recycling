package nl.eazysoftware.eazyrecyclingservice.domain.model.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

class Invoice(
    val id: InvoiceId,
    var invoiceNumber: InvoiceNumber?,
    var invoiceType: InvoiceType,
    val documentType: InvoiceDocumentType,
    var status: InvoiceStatus,
    var invoiceDate: LocalDate,
    var customerSnapshot: CustomerSnapshot,
    val originalInvoiceId: InvoiceId?,
    val creditedInvoiceNumber: String?,
    val sourceWeightTicketId: WeightTicketId?,
    val lines: MutableList<InvoiceLine>,
    val createdAt: Instant,
    val createdBy: String?,
    var updatedAt: Instant?,
    var updatedBy: String?,
    var finalizedAt: Instant?,
    var finalizedBy: String?,
    var pdfUrl: String? = null,
) {
    fun finalize(invoiceNumber: InvoiceNumber, finalizedBy: String?) {
        require(status == InvoiceStatus.DRAFT) { "Alleen concept facturen kunnen worden gefinaliseerd." }
        this.invoiceNumber = invoiceNumber
        this.status = InvoiceStatus.FINAL
        this.finalizedAt = Clock.System.now()
        this.finalizedBy = finalizedBy
    }

    fun markAsSent() {
        require(status == InvoiceStatus.FINAL) { "Alleen definitieve facturen kunnen worden verzonden." }
        this.status = InvoiceStatus.SENT
    }

    fun update(
        customerSnapshot: CustomerSnapshot = this.customerSnapshot,
        invoiceType: InvoiceType = this.invoiceType,
        invoiceDate: LocalDate = this.invoiceDate,
        lines: List<InvoiceLine> = this.lines,
        updatedBy: String? = null,
    ) {
        require(status == InvoiceStatus.DRAFT) { "Alleen concept facturen kunnen worden gewijzigd." }
        this.customerSnapshot = customerSnapshot
        this.invoiceType = invoiceType
        this.invoiceDate = invoiceDate
        this.lines.clear()
        this.lines.addAll(lines)
        this.updatedAt = Clock.System.now()
        this.updatedBy = updatedBy
    }

    fun calculateTotals(): InvoiceTotals {
        val totalExclVat = lines.sumOf { it.totalExclVat }

        val vatBreakdown = lines
            .groupBy { it.vatCode to it.vatPercentage }
            .map { (key, groupLines) ->
                val (vatCode, vatPercentage) = key
                val baseAmount = groupLines.sumOf { it.totalExclVat }
                val vatAmount = baseAmount * vatPercentage / BigDecimal(100)
                VatBreakdownLine(vatCode, vatPercentage, baseAmount, vatAmount)
            }

        val totalVat = vatBreakdown
            .sumOf { it.vatAmount }
            .setScale(2, RoundingMode.HALF_UP)
        val totalInclVat = totalExclVat + totalVat

        return InvoiceTotals(totalExclVat, vatBreakdown, totalVat, totalInclVat)
    }
}

data class InvoiceId(val value: UUID)

data class InvoiceNumber(val value: String) {
    companion object {
        fun generate(prefix: String, year: Int, sequence: Long): InvoiceNumber {
            val formatted = "$prefix-$year-${sequence.toString().padStart(5, '0')}"
            return InvoiceNumber(formatted)
        }

        fun parse(value: String): InvoiceNumber = InvoiceNumber(value)
    }
}

enum class InvoiceType {
    PURCHASE,
    SALE
}

enum class InvoiceDocumentType {
    INVOICE,
    CREDIT_NOTE
}

enum class InvoiceStatus {
    DRAFT,
    FINAL,
    SENT
}
