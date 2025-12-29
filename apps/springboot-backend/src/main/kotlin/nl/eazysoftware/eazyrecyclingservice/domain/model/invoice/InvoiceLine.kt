package nl.eazysoftware.eazyrecyclingservice.domain.model.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class InvoiceLine(
    val id: InvoiceLineId,
    val lineNumber: Int,
    val date: LocalDate,
    val description: String,
    val orderReference: String?,
    val vatCode: String,
    val vatPercentage: BigDecimal,
    val glAccountCode: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val totalExclVat: BigDecimal,
    val catalogItemId: UUID,
    val catalogItemCode: String,
    val catalogItemName: String,
    val catalogItemType: CatalogItemType,
    val unitOfMeasure: String,
)

data class InvoiceLineId(val value: UUID)
