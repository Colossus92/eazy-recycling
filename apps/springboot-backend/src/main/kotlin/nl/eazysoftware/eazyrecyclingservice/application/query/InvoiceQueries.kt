package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

interface GetAllInvoices {
    fun handle(): List<InvoiceView>
}

interface GetInvoiceById {
    fun handle(invoiceId: UUID): InvoiceDetailView?
}

@Service
class GetAllInvoicesQuery(
    private val invoices: Invoices,
) : GetAllInvoices {

    override fun handle(): List<InvoiceView> {
        return invoices.findAll().map { invoice ->
            val totals = invoice.calculateTotals()
            InvoiceView(
                id = invoice.id.value,
                invoiceNumber = invoice.invoiceNumber?.value,
                invoiceType = invoice.invoiceType.name,
                documentType = invoice.documentType.name,
                status = invoice.status.name,
                invoiceDate = invoice.invoiceDate,
                customerName = invoice.customerSnapshot.name,
                customerNumber = invoice.customerSnapshot.customerNumber,
                totalExclVat = totals.totalExclVat,
                totalInclVat = totals.totalInclVat,
            )
        }.sortedBy { it.invoiceNumber }
    }
}

@Service
class GetInvoiceByIdQuery(
    private val invoices: Invoices,
) : GetInvoiceById {

    override fun handle(invoiceId: UUID): InvoiceDetailView? {
        val invoice = invoices.findById(InvoiceId(invoiceId)) ?: return null
        val totals = invoice.calculateTotals()

        return InvoiceDetailView(
            id = invoice.id.value,
            invoiceNumber = invoice.invoiceNumber?.value,
            invoiceType = invoice.invoiceType.name,
            documentType = invoice.documentType.name,
            status = invoice.status.name,
            invoiceDate = invoice.invoiceDate,
            customer = CustomerSnapshotView(
                companyId = invoice.customerSnapshot.companyId.uuid,
                customerNumber = invoice.customerSnapshot.customerNumber,
                name = invoice.customerSnapshot.name,
                address = AddressSnapshotView(
                    streetName = invoice.customerSnapshot.address.streetName,
                    buildingNumber = invoice.customerSnapshot.address.buildingNumber,
                    buildingNumberAddition = invoice.customerSnapshot.address.buildingNumberAddition,
                    postalCode = invoice.customerSnapshot.address.postalCode,
                    city = invoice.customerSnapshot.address.city,
                    country = invoice.customerSnapshot.address.country,
                ),
                vatNumber = invoice.customerSnapshot.vatNumber,
            ),
            originalInvoiceId = invoice.originalInvoiceId?.value,
            sourceWeightTicketId = invoice.sourceWeightTicketId,
            lines = invoice.lines.map { line ->
                InvoiceLineView(
                    id = line.id.value,
                    lineNumber = line.lineNumber,
                    date = line.date,
                    orderReference = line.orderReference,
                    vatCode = line.vatCode,
                    vatPercentage = line.vatPercentage,
                    glAccountCode = line.glAccountCode,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    totalExclVat = line.totalExclVat,
                    catalogItemId = line.catalogItemId,
                    catalogItemCode = line.catalogItemCode,
                    catalogItemName = line.catalogItemName,
                    catalogItemType = line.catalogItemType.name,
                    unitOfMeasure = line.unitOfMeasure,
                )
            },
            totals = InvoiceTotalsView(
                totalExclVat = totals.totalExclVat,
                vatBreakdown = totals.vatBreakdown.map { vat ->
                    VatBreakdownLineView(
                        vatCode = vat.vatCode,
                        vatPercentage = vat.vatPercentage,
                        baseAmount = vat.baseAmount,
                        vatAmount = vat.vatAmount,
                    )
                },
                totalVat = totals.totalVat,
                totalInclVat = totals.totalInclVat,
            ),
            createdAt = invoice.createdAt.toString(),
            createdBy = invoice.createdBy,
            updatedAt = invoice.updatedAt?.toString(),
            updatedBy = invoice.updatedBy,
            finalizedAt = invoice.finalizedAt?.toString(),
            finalizedBy = invoice.finalizedBy,
            pdfUrl = invoice.pdfUrl,
        )
    }
}

data class InvoiceView(
    val id: UUID,
    val invoiceNumber: String?,
    val invoiceType: String,
    val documentType: String,
    val status: String,
    val invoiceDate: LocalDate,
    val customerName: String,
    val customerNumber: String?,
    val totalExclVat: BigDecimal,
    val totalInclVat: BigDecimal,
)

data class InvoiceDetailView(
    val id: UUID,
    val invoiceNumber: String?,
    val invoiceType: String,
    val documentType: String,
    val status: String,
    val invoiceDate: LocalDate,
    val customer: CustomerSnapshotView,
    val originalInvoiceId: UUID?,
    val sourceWeightTicketId: Long?,
    val lines: List<InvoiceLineView>,
    val totals: InvoiceTotalsView,
    val createdAt: String,
    val createdBy: String?,
    val updatedAt: String?,
    val updatedBy: String?,
    val finalizedAt: String?,
    val finalizedBy: String?,
    val pdfUrl: String?,
)

data class CustomerSnapshotView(
    val companyId: UUID,
    val customerNumber: String?,
    val name: String,
    val address: AddressSnapshotView,
    val vatNumber: String?,
)

data class AddressSnapshotView(
    val streetName: String,
    val buildingNumber: String?,
    val buildingNumberAddition: String?,
    val postalCode: String,
    val city: String,
    val country: String?,
)

data class InvoiceLineView(
    val id: UUID,
    val lineNumber: Int,
    val date: LocalDate,
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
    val catalogItemType: String,
    val unitOfMeasure: String,
)

data class InvoiceTotalsView(
    val totalExclVat: BigDecimal,
    val vatBreakdown: List<VatBreakdownLineView>,
    val totalVat: BigDecimal,
    val totalInclVat: BigDecimal,
)

data class VatBreakdownLineView(
    val vatCode: String,
    val vatPercentage: BigDecimal,
    val baseAmount: BigDecimal,
    val vatAmount: BigDecimal,
)
