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

interface UpdateInvoice {
    fun handle(cmd: UpdateInvoiceCommand): InvoiceResult
}

@Service
class UpdateInvoiceService(
    private val invoices: Invoices,
    private val companies: Companies,
    private val catalogItems: CatalogItems,
    private val vatRates: VatRates,
) : UpdateInvoice {

    @Transactional
    override fun handle(cmd: UpdateInvoiceCommand): InvoiceResult {
        val invoice = invoices.findById(InvoiceId(cmd.invoiceId))
            ?: throw IllegalArgumentException("Factuur niet gevonden: ${cmd.invoiceId}")

        val company = companies.findById(CompanyId(cmd.customerId))
            ?: throw IllegalArgumentException("Bedrijf niet gevonden: ${cmd.customerId}")

        val customerSnapshot = createCustomerSnapshot(company)

        val lines = cmd.lines.mapIndexed { index, lineCmd ->
            createInvoiceLine(index + 1, lineCmd)
        }

        invoice.update(
            customerSnapshot = customerSnapshot,
            invoiceType = cmd.invoiceType,
            invoiceDate = cmd.invoiceDate,
            lines = lines,
            updatedBy = null,
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
            vatNumber = null,
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
