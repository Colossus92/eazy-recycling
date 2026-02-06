package nl.eazysoftware.eazyrecyclingservice.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.Invoice
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceDocumentType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceType
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class InvoicePdfPayloadBuilder(
    private val objectMapper: ObjectMapper,
) {

    private val nlLocale = Locale.of("nl", "NL")

    private fun formatCurrency(amount: BigDecimal): String {
        val formatter = NumberFormat.getCurrencyInstance(nlLocale)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return formatter.format(amount.setScale(2, RoundingMode.HALF_UP))
    }

    private fun formatNumber(amount: BigDecimal): String {
        val formatter = NumberFormat.getNumberInstance(nlLocale)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return formatter.format(amount.setScale(2, RoundingMode.HALF_UP))
    }

    fun buildPayload(invoice: Invoice): String {
        val isCreditNote = invoice.documentType == InvoiceDocumentType.CREDIT_NOTE

        val payload = InvoicePdfPayload(
            invoiceType = mapInvoiceType(invoice),
            invoiceNumber = invoice.invoiceNumber?.value ?: "",
            invoiceDate = invoice.invoiceDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            creditedInvoiceNumber = invoice.creditedInvoiceNumber,
            paymentTermDays = 9,
            companyCode = invoice.customerSnapshot.customerNumber ?: invoice.customerSnapshot.companyId.uuid.toString(),
            tenant = TenantInfoPayload(
                name = "WHD Kabel- en metaalrecycling",
                address = AddressPayload(
                    street = "Lotsweg",
                    buildingNumber = "10",
                    postalCode = "2635 NB",
                    city = "Den Hoorn",
                    country = "Nederland",
                ),
                phone = "+31 (0)85 902 4660",
                email = "info@hetwestlandbv.nl",
                website = "www.hetwestlandbv.nl",
                kvkNumber = "80892179",
                ibanNumber = "NL91 ABNA 0417 1649 09",
                vatNumber = "NL861288797B01",
            ),
            customer = CustomerInfoPayload(
                name = invoice.customerSnapshot.name,
                address = AddressPayload(
                    street = invoice.customerSnapshot.address.streetName,
                    buildingNumber = buildBuildingNumber(
                        invoice.customerSnapshot.address.buildingNumber,
                        invoice.customerSnapshot.address.buildingNumberAddition,
                    ),
                    postalCode = invoice.customerSnapshot.address.postalCode,
                    city = invoice.customerSnapshot.address.city,
                    country = invoice.customerSnapshot.address.country,
                ),
                creditorNumber = invoice.customerSnapshot.customerNumber ?: "",
                vatNumber = invoice.customerSnapshot.vatNumber,
            ),
            lines = invoice.lines.map { line ->
                val lineTotal = if (isCreditNote) line.totalExclVat.negate() else line.totalExclVat
                InvoiceLinePayload(
                    date = line.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    description = buildDescription(line.description, line.catalogItemName),
                    orderNumber = line.orderReference,
                    quantity = formatNumber(line.quantity),
                    unit = line.unitOfMeasure,
                    vatCode = line.vatCode,
                    vatPercentage = if (line.vatCode == "G") "G" else line.vatPercentage.toInt(),
                    isReverseCharge = line.isReverseCharge,
                    pricePerUnit = formatCurrency(line.unitPrice),
                    totalAmount = formatCurrency(lineTotal),
                )
            },
            materialTotals = calculateMaterialTotals(invoice, isCreditNote),
            totals = buildTotals(invoice, isCreditNote),
        )

        return objectMapper.writeValueAsString(payload)
    }

    private fun mapInvoiceType(invoice: Invoice): String {
      return  if (invoice.documentType == InvoiceDocumentType.CREDIT_NOTE) "CREDITFACTUUR"
              else if (invoice.invoiceType == InvoiceType.PURCHASE) "INKOOPFACTUUR"
              else "VERKOOPFACTUUR"
    }

    private fun buildBuildingNumber(number: String?, addition: String?): String {
        return listOfNotNull(number, addition).joinToString(" ")
    }

    private fun buildDescription(description: String, catalogItemName: String): List<String> {
        val lines = mutableListOf(catalogItemName)
        if (description.isNotBlank() && description != catalogItemName) {
            lines.add(description)
        }
        return lines
    }

    private fun buildTotals(invoice: Invoice, isCreditNote: Boolean): InvoiceTotalsPayload {
        val totals = invoice.calculateTotals()
        val vatBreakdown = totals.vatBreakdown
            .filter { it.vatAmount.compareTo(BigDecimal.ZERO) != 0 }
            .map { line ->
                val amount = if (isCreditNote) line.vatAmount.negate() else line.vatAmount
                VatBreakdownPayload(
                    vatPercentage = line.vatPercentage.toInt(),
                    amount = formatCurrency(amount),
                )
            }
        val exclVat = if (isCreditNote) totals.totalExclVat.negate() else totals.totalExclVat
        val vatAmount = if (isCreditNote) totals.totalVat.negate() else totals.totalVat
        val inclVat = if (isCreditNote) totals.totalInclVat.negate() else totals.totalInclVat
        return InvoiceTotalsPayload(
            totalExclVat = formatCurrency(exclVat),
            vatBreakdown = vatBreakdown,
            vatAmount = formatCurrency(vatAmount),
            totalInclVat = formatCurrency(inclVat),
        )
    }

    private fun calculateMaterialTotals(invoice: Invoice, isCreditNote: Boolean): List<MaterialTotalPayload> {
        return invoice.lines
            .filter { it.catalogItemType == CatalogItemType.MATERIAL }
            .groupBy { it.catalogItemName }
            .map { (materialName, lines) ->
                val totalAmount = lines.sumOf { it.totalExclVat }
                val displayAmount = if (isCreditNote) totalAmount.negate() else totalAmount
                MaterialTotalPayload(
                    material = materialName,
                    totalWeight = formatNumber(lines.sumOf { it.quantity }),
                    unit = lines.first().unitOfMeasure,
                    totalAmount = formatCurrency(displayAmount),
                )
            }
    }
}

data class InvoicePdfPayload(
    val invoiceType: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val creditedInvoiceNumber: String?,
    val paymentTermDays: Int,
    val companyCode: String,
    val tenant: TenantInfoPayload,
    val customer: CustomerInfoPayload,
    val lines: List<InvoiceLinePayload>,
    val materialTotals: List<MaterialTotalPayload>,
    val totals: InvoiceTotalsPayload,
)

data class TenantInfoPayload(
    val name: String,
    val address: AddressPayload,
    val phone: String,
    val email: String,
    val website: String,
    val kvkNumber: String,
    val ibanNumber: String,
    val vatNumber: String,
)

data class AddressPayload(
    val street: String,
    val buildingNumber: String,
    val postalCode: String,
    val city: String,
    val country: String?,
)

data class CustomerInfoPayload(
    val name: String,
    val address: AddressPayload,
    val creditorNumber: String,
    val vatNumber: String?,
)

data class InvoiceLinePayload(
    val date: String,
    val description: List<String>,
    val orderNumber: String?,
    val quantity: String,
    val unit: String,
    val vatCode: String,
    val vatPercentage: Any,
    val isReverseCharge: Boolean,
    val pricePerUnit: String,
    val totalAmount: String,
)

data class MaterialTotalPayload(
    val material: String,
    val totalWeight: String,
    val unit: String,
    val totalAmount: String,
)

data class InvoiceTotalsPayload(
    val totalExclVat: String,
    val vatBreakdown: List<VatBreakdownPayload>,
    val vatAmount: String,
    val totalInclVat: String,
)

data class VatBreakdownPayload(
    val vatPercentage: Int,
    val amount: String,
)
