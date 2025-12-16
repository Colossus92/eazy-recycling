package nl.eazysoftware.eazyrecyclingservice.domain.model.invoice

import java.math.BigDecimal

data class InvoiceTotals(
    val totalExclVat: BigDecimal,
    val vatBreakdown: List<VatBreakdownLine>,
    val totalVat: BigDecimal,
    val totalInclVat: BigDecimal,
)

data class VatBreakdownLine(
    val vatCode: String,
    val vatPercentage: BigDecimal,
    val baseAmount: BigDecimal,
    val vatAmount: BigDecimal,
)
