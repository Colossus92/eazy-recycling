package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.CatalogItems
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.VatRates
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.time.Clock

interface CreateInvoice {
    fun handle(cmd: CreateInvoiceCommand): InvoiceResult
}

@Service
class CreateInvoiceService(
    private val invoices: Invoices,
    private val companies: Companies,
    private val catalogItems: CatalogItems,
    private val vatRates: VatRates,
) : CreateInvoice {

    @Transactional
    override fun handle(cmd: CreateInvoiceCommand): InvoiceResult {
        val company = companies.findById(CompanyId(cmd.customerId))
            ?: throw IllegalArgumentException("Bedrijf niet gevonden: ${cmd.customerId}")

        val customerSnapshot = createCustomerSnapshot(company)

        val invoiceId = invoices.nextId()
        val lines = cmd.lines.mapIndexed { index, lineCmd ->
            createInvoiceLine(index + 1, lineCmd)
        }.toMutableList()

        val originalInvoiceId = cmd.originalInvoiceId?.let { InvoiceId(it) }

        val invoice = Invoice(
            id = invoiceId,
            invoiceNumber = null,
            invoiceType = cmd.invoiceType,
            documentType = cmd.documentType,
            status = InvoiceStatus.DRAFT,
            invoiceDate = cmd.invoiceDate,
            customerSnapshot = customerSnapshot,
            originalInvoiceId = originalInvoiceId,
            creditedInvoiceNumber = cmd.creditedInvoiceNumber,
            sourceWeightTicketId = cmd.sourceWeightTicketId,
            lines = lines,
            createdAt = Clock.System.now(),
            createdBy = null,
            updatedAt = null,
            updatedBy = null,
            finalizedAt = null,
            finalizedBy = null,
        )

        val saved = invoices.save(invoice)
        return InvoiceResult(invoiceId = saved.id.value)
    }

    private fun createCustomerSnapshot(company: Company): CustomerSnapshot {
        return CustomerSnapshot(
            companyId = company.companyId,
            customerNumber = company.code,
            name = company.name,
            address = AddressSnapshot(
                streetName = company.address.streetName.value,
                buildingNumber = company.address.buildingNumber,
                buildingNumberAddition = company.address.buildingNumberAddition,
                postalCode = company.address.postalCode.value,
                city = company.address.city.value,
                country = company.address.country,
            ),
            vatNumber = company.vatNumber,
        )
    }

    private fun createInvoiceLine(lineNumber: Int, cmd: InvoiceLineCommand): InvoiceLine {
        val allCatalogItems = catalogItems.findAll(null, null)
        val catalogItem = allCatalogItems.find { it.id == cmd.catalogItemId }
            ?: throw IllegalArgumentException("Catalogus item niet gevonden: ${cmd.catalogItemId}")

        val vatRate = vatRates.getVatRateByCode(catalogItem.vatCode)
            ?: throw IllegalArgumentException("BTW code niet gevonden: ${catalogItem.vatCode}")

        val totalExclVat = cmd.quantity.multiply(cmd.unitPrice)
        val vatPercentage = BigDecimal(vatRate.percentage)

        val lineId = cmd.id?.let { InvoiceLineId(it) } ?: invoices.nextLineId()

        return InvoiceLine(
            id = lineId,
            lineNumber = lineNumber,
            date = cmd.date,
            description = cmd.description ?: catalogItem.name,
            orderReference = cmd.orderReference,
            vatCode = catalogItem.vatCode,
            vatPercentage = vatPercentage,
            isReverseCharge = vatRate.isReverseCharge(),
            glAccountCode = catalogItem.salesAccountNumber,
            quantity = cmd.quantity,
            unitPrice = cmd.unitPrice,
            totalExclVat = totalExclVat,
            catalogItemId = catalogItem.id,
            catalogItemCode = catalogItem.code,
            catalogItemName = catalogItem.name,
            catalogItemType = catalogItem.type,
            unitOfMeasure = catalogItem.unitOfMeasure,
        )
    }
}
