package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceLine
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceLineId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.CatalogItems
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
    private val catalogItems: CatalogItems,
    private val vatRates: VatRates,
) : UpdateInvoice {

    @Transactional
    override fun handle(cmd: UpdateInvoiceCommand): InvoiceResult {
        val invoice = invoices.findById(InvoiceId(cmd.invoiceId))
            ?: throw IllegalArgumentException("Factuur niet gevonden: ${cmd.invoiceId}")

        val lines = cmd.lines.mapIndexed { index, lineCmd ->
            createInvoiceLine(index + 1, lineCmd)
        }

        invoice.update(
            invoiceDate = cmd.invoiceDate,
            lines = lines,
            updatedBy = null,
        )

        val saved = invoices.save(invoice)
        return InvoiceResult(invoiceId = saved.id.value)
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
