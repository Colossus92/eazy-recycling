package nl.eazysoftware.eazyrecyclingservice.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.Invoice
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceDocumentType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceType
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class InvoicePdfPayloadBuilder(
    private val objectMapper: ObjectMapper,
) {

    fun buildPayload(invoice: Invoice): String {
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
                InvoiceLinePayload(
                    date = line.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    description = buildDescription(line.description, line.catalogItemName),
                    orderNumber = line.orderReference,
                    quantity = line.quantity.toDouble(),
                    unit = line.unitOfMeasure,
                    vatPercentage = if (line.vatCode == "G") "G" else line.vatPercentage.toInt(),
                    pricePerUnit = line.unitPrice.toDouble(),
                    totalAmount = line.totalExclVat.toDouble(),
                )
            },
            materialTotals = calculateMaterialTotals(invoice),
            totals = InvoiceTotalsPayload(
                totalExclVat = invoice.calculateTotals().totalExclVat.toDouble(),
                vatAmount = invoice.calculateTotals().totalVat.toDouble(),
                totalInclVat = invoice.calculateTotals().totalInclVat.toDouble(),
            ),
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

    private fun calculateMaterialTotals(invoice: Invoice): List<MaterialTotalPayload> {
        return invoice.lines
            .filter { it.catalogItemType == CatalogItemType.MATERIAL }
            .groupBy { it.catalogItemName }
            .map { (materialName, lines) ->
                MaterialTotalPayload(
                    material = materialName,
                    totalWeight = lines.sumOf { it.quantity }.toDouble(),
                    unit = lines.first().unitOfMeasure,
                    totalAmount = lines.sumOf { it.totalExclVat }.toDouble(),
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
    val quantity: Double,
    val unit: String,
    val vatPercentage: Any,
    val pricePerUnit: Double,
    val totalAmount: Double,
)

data class MaterialTotalPayload(
    val material: String,
    val totalWeight: Double,
    val unit: String,
    val totalAmount: Double,
)

data class InvoiceTotalsPayload(
    val totalExclVat: Double,
    val vatAmount: Double,
    val totalInclVat: Double,
)
