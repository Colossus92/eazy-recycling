package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyMapper
import org.springframework.stereotype.Component
import java.util.*
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Component
class WeightTicketMapper(
  private val companies: Companies,
  private val pickupLocationMapper: PickupLocationMapper,
  private val companyMapper: CompanyMapper,
  private val catalogItemRepository: CatalogItemJpaRepository,
) {

  fun toDomain(dto: WeightTicketDto): WeightTicket {
    return WeightTicket(
      id = WeightTicketId(dto.id, dto.number),
      carrierParty = dto.carrierParty?.id?.let { CompanyId(it) },
      consignorParty = Consignor.Company(CompanyId(dto.consignorParty.id)),
      direction = dto.direction,
      pickupLocation = dto.pickupLocation?.let { pickupLocationMapper.toDomain(it) },
      deliveryLocation = dto.deliveryLocation?.let { pickupLocationMapper.toDomain(it) },
      truckLicensePlate = dto.truckLicensePlate?.let { LicensePlate(it) },
      reclamation = dto.reclamation,
      note = dto.note?.let { Note(it) },
      status = toDomainStatus(dto.status),
      createdAt = dto.createdAt?.toKotlinInstant(),
      createdBy = dto.createdBy,
      updatedAt = dto.updatedAt?.toKotlinInstant(),
      updatedBy = dto.updatedBy,
      weightedAt = dto.weightedAt?.toKotlinInstant(),
      cancellationReason = dto.cancellationReason?.let { CancellationReason(it) },
      linkedInvoiceId = dto.linkedInvoiceId,
      pdfUrl = dto.pdfUrl,
      lines = dto.lines
        .map { lineDto ->
          lineDto.toDomain(lineDto.catalogItemId, lineDto.catalogItem.type)
        }
        .toMutableList()
        .let { WeightTicketLines(it) },
      productLines = dto.productLines
        .map { productLineDto ->
          WeightTicketProductLine(
            catalogItemId = productLineDto.catalogItemId,
            catalogItemType = productLineDto.catalogItem.type,
            quantity = productLineDto.quantity,
            unit = productLineDto.unit
          )
        }
        .let { WeightTicketProductLines(it) },
      secondWeighing = dto.secondWeighingValue?.let {
        Weight(
          it,
          when (dto.secondWeighingUnit) {
            WeightUnitDto.kg -> Weight.WeightUnit.KILOGRAM
            null -> throw IllegalStateException("Tweede weging eenheid is niet gevonden voor weegbon met nummer ${dto.id}")
          },
        )
      },
      tarraWeight = dto.tarraWeightValue?.let {
        Weight(
          it,
          when (dto.tarraWeightUnit) {
            WeightUnitDto.kg -> Weight.WeightUnit.KILOGRAM
            null -> throw IllegalStateException("Tarra gewicht eenheid is niet gevonden voor weegbon met nummer ${dto.id}")
          },
        )
      },
    )
  }

  fun toDto(domain: WeightTicket): WeightTicketDto {
    val carrierParty = domain.carrierParty
      ?.let { companies.findById(it) ?: throw IllegalStateException("Transporteur niet gevonden: ${it.uuid}") }
    val consignorParty = when (val consignor = domain.consignorParty) {
      is Consignor.Company -> companies.findById(consignor.id)
        ?: throw IllegalArgumentException("Opdrachtgever niet gevonden: ${consignor.id.uuid}")

      Consignor.Person -> throw IllegalArgumentException("Particuliere opdrachtgever wordt nog niet ondersteund.")
    }

    val weightTicketDto = WeightTicketDto(
      id = domain.id.id,
      number = domain.id.number,
      consignorParty = companyMapper.toDto(consignorParty),
      lines = mutableListOf(),
      productLines = mutableListOf(),
      secondWeighingValue = domain.secondWeighing?.value,
      secondWeighingUnit = toDto(domain.secondWeighing?.unit),
      tarraWeightValue = domain.tarraWeight?.value,
      tarraWeightUnit = toDto(domain.tarraWeight?.unit),
      carrierParty = carrierParty?.let { companyMapper.toDto(carrierParty) },
      truckLicensePlate = domain.truckLicensePlate?.value,
      reclamation = domain.reclamation,
      note = domain.note?.description,
      status = toDtoStatus(domain.status),
      direction = domain.direction,
      pickupLocation = domain.pickupLocation?.let { pickupLocationMapper.toDto(it) },
      deliveryLocation = domain.deliveryLocation?.let { pickupLocationMapper.toDto(it) },
      weightedAt = domain.weightedAt?.toJavaInstant(),
      cancellationReason = domain.cancellationReason?.value,
      linkedInvoiceId = domain.linkedInvoiceId,
      pdfUrl = domain.pdfUrl,
    )

    // Set lines after creating parent to establish bidirectional relationship
    weightTicketDto.lines.addAll(toDto(domain.lines, weightTicketDto))
    weightTicketDto.productLines.addAll(toProductLineDto(domain.productLines, weightTicketDto))

    return weightTicketDto
  }

  private fun toDto(domain: Weight.WeightUnit?): WeightUnitDto? {
    return when (domain) {
      Weight.WeightUnit.KILOGRAM -> WeightUnitDto.kg
      null -> null
    }
  }

  private fun toDto(domain: WeightTicketLines, weightTicket: WeightTicketDto): MutableList<WeightTicketLineDto> {
    return domain.getLines()
      .map {
        val catalogItem = catalogItemRepository.findById(it.catalogItemId)
          .orElseThrow { IllegalArgumentException("Item niet gevonden: ${it.catalogItemId}") }

        WeightTicketLineDto(
          id = UUID.randomUUID(),
          weightTicket = weightTicket,
          wasteStreamNumber = it.waste?.number,
          catalogItem = catalogItem,
          catalogItemId = it.catalogItemId,
          weightValue = it.weight.value,
          weightUnit = when (it.weight.unit) {
            Weight.WeightUnit.KILOGRAM -> WeightUnitDto.kg
          },
          declaredWeight = it.declarationState.declaredWeight,
          lastDeclaredAt = it.declarationState.lastDeclaredAt?.toJavaInstant(),
        )
      }.toMutableList()
  }

  private fun toProductLineDto(domain: WeightTicketProductLines, weightTicket: WeightTicketDto): MutableList<WeightTicketProductLineDto> {
    return domain.getLines()
      .map {
        val catalogItem = catalogItemRepository.findById(it.catalogItemId)
          .orElseThrow { IllegalArgumentException("Item niet gevonden: ${it.catalogItemId}") }

        WeightTicketProductLineDto(
          id = UUID.randomUUID(),
          weightTicket = weightTicket,
          catalogItem = catalogItem,
          catalogItemId = it.catalogItemId,
          quantity = it.quantity,
          unit = it.unit,
        )
      }.toMutableList()
  }

  private fun toDomainStatus(dto: WeightTicketStatusDto): WeightTicketStatus {
    return when (dto) {
      WeightTicketStatusDto.DRAFT -> WeightTicketStatus.DRAFT
      WeightTicketStatusDto.COMPLETED -> WeightTicketStatus.COMPLETED
      WeightTicketStatusDto.INVOICED -> WeightTicketStatus.INVOICED
      WeightTicketStatusDto.CANCELLED -> WeightTicketStatus.CANCELLED
    }
  }

  private fun toDtoStatus(domain: WeightTicketStatus): WeightTicketStatusDto {
    return when (domain) {
      WeightTicketStatus.DRAFT -> WeightTicketStatusDto.DRAFT
      WeightTicketStatus.COMPLETED -> WeightTicketStatusDto.COMPLETED
      WeightTicketStatus.INVOICED -> WeightTicketStatusDto.INVOICED
      WeightTicketStatus.CANCELLED -> WeightTicketStatusDto.CANCELLED
    }
  }
}
