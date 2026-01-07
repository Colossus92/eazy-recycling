package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketDirection
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketStatus
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import kotlin.time.Clock

interface CreateInvoiceFromWeightTicket {
    fun handle(cmd: CreateInvoiceFromWeightTicketCommand): CreateInvoiceFromWeightTicketResult
}

data class CreateInvoiceFromWeightTicketCommand(
    val weightTicketNumber: Long,
)

data class CreateInvoiceFromWeightTicketResult(
    val invoiceId: UUID,
    val weightTicketId: Long,
)

@Service
class CreateInvoiceFromWeightTicketService(
    private val weightTickets: WeightTickets,
    private val invoices: Invoices,
    private val companies: Companies,
    private val catalogItems: CatalogItems,
    private val vatRates: VatRates,
) : CreateInvoiceFromWeightTicket {

    @Transactional
    override fun handle(cmd: CreateInvoiceFromWeightTicketCommand): CreateInvoiceFromWeightTicketResult {
        val weightTicket = weightTickets.findByNumber(cmd.weightTicketNumber)
            ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.weightTicketNumber} bestaat niet")

        require(weightTicket.status == WeightTicketStatus.COMPLETED) {
            "Factuur kan alleen worden aangemaakt van een voltooide weegbon. Huidige status: ${weightTicket.status}"
        }

        require(weightTicket.linkedInvoiceId == null) {
            "Er is al een factuur gekoppeld aan deze weegbon (factuur ID: ${weightTicket.linkedInvoiceId})"
        }

        val consignorCompanyId = when (val consignor = weightTicket.consignorParty) {
            is Consignor.Company -> consignor.id
            Consignor.Person -> throw IllegalArgumentException("Particuliere opdrachtgevers worden nog niet ondersteund voor facturatie")
        }

        val company = companies.findById(consignorCompanyId)
            ?: throw IllegalArgumentException("Opdrachtgever niet gevonden: ${consignorCompanyId.uuid}")

        val customerSnapshot = CustomerSnapshot(
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

        val allCatalogItems = catalogItems.findAll(null, null)
        
        // Create invoice lines from weight ticket lines (materials/waste streams)
        val materialInvoiceLines = weightTicket.lines.getLines().mapIndexed { index, weightTicketLine ->
            val catalogItem = allCatalogItems.find { it.id == weightTicketLine.catalogItemId }
                ?: throw IllegalArgumentException("Catalogus item niet gevonden: ${weightTicketLine.catalogItemId}")

            val vatRate = vatRates.getVatRateByCode(catalogItem.vatCode)
                ?: throw IllegalArgumentException("BTW code niet gevonden: ${catalogItem.vatCode}")

            val quantity = weightTicketLine.weight.value
            val unitPrice = catalogItem.defaultPrice ?: BigDecimal.ZERO
            val totalExclVat = quantity.multiply(unitPrice)
            val vatPercentage = BigDecimal(vatRate.percentage)

            InvoiceLine(
                id = invoices.nextLineId(),
                lineNumber = index + 1,
                date = LocalDate.now(),
                description = catalogItem.name,
                orderReference = "Weegbon ${weightTicket.id.number}",
                vatCode = catalogItem.vatCode,
                vatPercentage = vatPercentage,
                glAccountCode = catalogItem.salesAccountNumber,
                quantity = quantity,
                unitPrice = unitPrice,
                totalExclVat = totalExclVat,
                catalogItemId = catalogItem.id,
                catalogItemCode = catalogItem.code,
                catalogItemName = catalogItem.name,
                catalogItemType = catalogItem.type,
                unitOfMeasure = catalogItem.unitOfMeasure,
            )
        }

        // Create invoice lines from weight ticket product lines
        val startLineNumber = materialInvoiceLines.size + 1
        val productInvoiceLines = weightTicket.productLines.getLines().mapIndexed { index, productLine ->
            val catalogItem = allCatalogItems.find { it.id == productLine.catalogItemId }
                ?: throw IllegalArgumentException("Catalogus item niet gevonden: ${productLine.catalogItemId}")

            val vatRate = vatRates.getVatRateByCode(catalogItem.vatCode)
                ?: throw IllegalArgumentException("BTW code niet gevonden: ${catalogItem.vatCode}")

            val quantity = productLine.quantity
            val unitPrice = catalogItem.defaultPrice ?: BigDecimal.ZERO
            val totalExclVat = quantity.multiply(unitPrice)
            val vatPercentage = BigDecimal(vatRate.percentage)

            InvoiceLine(
                id = invoices.nextLineId(),
                lineNumber = startLineNumber + index,
                date = LocalDate.now(),
                description = catalogItem.name,
                orderReference = "Weegbon ${weightTicket.id.number}",
                vatCode = catalogItem.vatCode,
                vatPercentage = vatPercentage,
                glAccountCode = catalogItem.salesAccountNumber,
                quantity = quantity,
                unitPrice = unitPrice,
                totalExclVat = totalExclVat,
                catalogItemId = catalogItem.id,
                catalogItemCode = catalogItem.code,
                catalogItemName = catalogItem.name,
                catalogItemType = catalogItem.type,
                unitOfMeasure = productLine.unit,
            )
        }

        val invoiceLines = (materialInvoiceLines + productInvoiceLines).toMutableList()

        val invoiceId = invoices.nextId()
        val invoice = Invoice(
            id = invoiceId,
            invoiceNumber = null,
            invoiceType = when(weightTicket.direction) {
              WeightTicketDirection.INBOUND -> InvoiceType.PURCHASE
              WeightTicketDirection.OUTBOUND -> InvoiceType.SALE
            },
            documentType = InvoiceDocumentType.INVOICE,
            status = InvoiceStatus.DRAFT,
            invoiceDate = LocalDate.now(),
            customerSnapshot = customerSnapshot,
            originalInvoiceId = null,
            sourceWeightTicketId = weightTicket.id,
            lines = invoiceLines,
            createdAt = Clock.System.now(),
            createdBy = null,
            updatedAt = null,
            updatedBy = null,
            finalizedAt = null,
            finalizedBy = null,
        )

        val savedInvoice = invoices.save(invoice)

        // Update weight ticket with linked invoice and status
        weightTicket.linkedInvoiceId = savedInvoice.id.value
        weightTicket.status = WeightTicketStatus.INVOICED
        weightTickets.save(weightTicket)

        return CreateInvoiceFromWeightTicketResult(
            invoiceId = savedInvoice.id.value,
            weightTicketId = weightTicket.id.number,
        )
    }
}
