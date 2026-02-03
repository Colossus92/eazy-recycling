package nl.eazysoftware.eazyrecyclingservice.application.query

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

// ============ Waste Streams by Company ============

interface GetWasteStreamsByCompany {
    fun execute(companyId: UUID): List<WasteStreamByCompanyView>
}

data class WasteStreamByCompanyView(
    val wasteStreamNumber: String,
    val wasteName: String,
    val pickupLocation: String,
    val status: String,
)

// ============ Weight Tickets by Company ============

interface GetWeightTicketsByCompany {
    fun execute(companyId: UUID): List<WeightTicketByCompanyView>
}

data class WeightTicketByCompanyView(
    val id: Long,
    val totalWeight: Double?,
    val weighingDate: Instant?,
    val pickupLocation: String?,
    val status: String,
)

// ============ Transports by Company ============

interface GetTransportsByCompany {
    fun execute(companyId: UUID): List<TransportByCompanyView>
}

data class TransportByCompanyView(
    val id: UUID,
    val displayNumber: String?,
    val date: LocalDate?,
    val pickupLocation: String?,
    val status: String,
)

// ============ Invoices by Company ============

interface GetInvoicesByCompany {
    fun execute(companyId: UUID): List<InvoiceByCompanyView>
}

data class InvoiceByCompanyView(
    val id: UUID,
    val invoiceNumber: String?,
    val invoiceType: String,
    val totalInclVat: BigDecimal,
    val status: String,
)
