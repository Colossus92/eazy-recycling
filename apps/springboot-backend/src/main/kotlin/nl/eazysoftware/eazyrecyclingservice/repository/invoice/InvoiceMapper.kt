package nl.eazysoftware.eazyrecyclingservice.repository.invoice

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.WeightTicketDto
import org.springframework.stereotype.Component
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Component
class InvoiceMapper(
    private val entityManager: EntityManager
) {

    fun toDomain(dto: InvoiceDto): Invoice {
        return Invoice(
            id = InvoiceId(dto.id),
            invoiceNumber = dto.invoiceNumber?.let { InvoiceNumber(it) },
            invoiceType = dto.invoiceType,
            documentType = dto.documentType,
            status = dto.status,
            invoiceDate = dto.invoiceDate,
            customerSnapshot = CustomerSnapshot(
                companyId = CompanyId(dto.customerCompanyId),
                customerNumber = dto.customerNumber,
                name = dto.customerName,
                address = AddressSnapshot(
                    streetName = dto.customerStreetName,
                    buildingNumber = dto.customerBuildingNumber,
                    buildingNumberAddition = dto.customerBuildingNumberAddition,
                    postalCode = dto.customerPostalCode,
                    city = dto.customerCity,
                    country = dto.customerCountry,
                ),
                vatNumber = dto.customerVatNumber,
            ),
            originalInvoiceId = dto.originalInvoiceId?.let { InvoiceId(it) },
            sourceWeightTicketId = dto.weightTicket?.let { WeightTicketId(it.id, it.number) },
            lines = dto.lines.map { toDomainLine(it) }.toMutableList(),
            createdAt = dto.createdAt?.toKotlinInstant() ?: kotlin.time.Clock.System.now(),
            createdBy = dto.createdBy,
            updatedAt = dto.updatedAt?.toKotlinInstant(),
            updatedBy = dto.updatedBy,
            finalizedAt = dto.finalizedAt?.toKotlinInstant(),
            finalizedBy = dto.finalizedBy,
            pdfUrl = dto.pdfUrl,
        )
    }

    fun toDto(domain: Invoice): InvoiceDto {
        val dto = InvoiceDto(
            id = domain.id.value,
            invoiceNumber = domain.invoiceNumber?.value,
            invoiceType = domain.invoiceType,
            documentType = domain.documentType,
            status = domain.status,
            invoiceDate = domain.invoiceDate,
            customerCompanyId = domain.customerSnapshot.companyId.uuid,
            customerNumber = domain.customerSnapshot.customerNumber,
            customerName = domain.customerSnapshot.name,
            customerStreetName = domain.customerSnapshot.address.streetName,
            customerBuildingNumber = domain.customerSnapshot.address.buildingNumber,
            customerBuildingNumberAddition = domain.customerSnapshot.address.buildingNumberAddition,
            customerPostalCode = domain.customerSnapshot.address.postalCode,
            customerCity = domain.customerSnapshot.address.city,
            customerCountry = domain.customerSnapshot.address.country,
            customerVatNumber = domain.customerSnapshot.vatNumber,
            originalInvoiceId = domain.originalInvoiceId?.value,
            weightTicket = domain.sourceWeightTicketId?.let { entityManager.getReference(WeightTicketDto::class.java, it.id) },
            finalizedAt = domain.finalizedAt?.toJavaInstant(),
            finalizedBy = domain.finalizedBy,
            pdfUrl = domain.pdfUrl,
        )

        dto.lines.clear()
        dto.lines.addAll(domain.lines.map { toDtoLine(it, dto) })

        return dto
    }

    private fun toDomainLine(dto: InvoiceLineDto): InvoiceLine {
        return InvoiceLine(
            id = InvoiceLineId(dto.id),
            lineNumber = dto.lineNumber,
            date = dto.lineDate,
            description = dto.description,
            orderReference = dto.orderReference,
            vatCode = dto.vatCode,
            vatPercentage = dto.vatPercentage,
            glAccountCode = dto.glAccountCode,
            quantity = dto.quantity,
            unitPrice = dto.unitPrice,
            totalExclVat = dto.totalExclVat,
            catalogItemId = dto.catalogItemId,
            catalogItemCode = dto.catalogItemCode,
            catalogItemName = dto.catalogItemName,
            catalogItemType = dto.catalogItemType,
            unitOfMeasure = dto.unitOfMeasure,
        )
    }

    private fun toDtoLine(domain: InvoiceLine, invoiceDto: InvoiceDto): InvoiceLineDto {
        return InvoiceLineDto(
            id = domain.id.value,
            invoice = invoiceDto,
            lineNumber = domain.lineNumber,
            lineDate = domain.date,
            description = domain.description,
            orderReference = domain.orderReference,
            vatCode = domain.vatCode,
            vatPercentage = domain.vatPercentage,
            glAccountCode = domain.glAccountCode,
            quantity = domain.quantity,
            unitPrice = domain.unitPrice,
            totalExclVat = domain.totalExclVat,
            unitOfMeasure = domain.unitOfMeasure,
            catalogItemId = domain.catalogItemId,
            catalogItemCode = domain.catalogItemCode,
            catalogItemName = domain.catalogItemName,
            catalogItemType = domain.catalogItemType,
        )
    }
}
