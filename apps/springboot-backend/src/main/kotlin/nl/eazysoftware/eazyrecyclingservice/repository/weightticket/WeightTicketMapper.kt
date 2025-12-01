package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyMapper
import org.springframework.stereotype.Component
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Component
class WeightTicketMapper(
  private val companies: Companies,
  private val pickupLocationMapper: PickupLocationMapper,
  private val companyMapper: CompanyMapper,
) {

  fun toDomain(dto: WeightTicketDto): WeightTicket {
    return WeightTicket(
      id = WeightTicketId(dto.id),
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
      lines = dto.lines
        .map { it.toDomain() }
        .toMutableList()
        .let { WeightTicketLines(it) },
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

    return WeightTicketDto(
      id = domain.id.number,
      consignorParty = companyMapper.toDto(consignorParty),
      lines = toDto(domain.lines),
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
    )
  }

  private fun toDto(domain: Weight.WeightUnit?): WeightUnitDto? {
    return when (domain) {
      Weight.WeightUnit.KILOGRAM -> WeightUnitDto.kg
      null -> null
    }
  }

  private fun toDto(domain: WeightTicketLines): List<WeightTicketLineDto> {
    return domain.getLines()
      .map {
        WeightTicketLineDto(
          wasteStreamNumber = it.waste.number,
          weightValue = it.weight.value,
          weightUnit = when (it.weight.unit) {
            Weight.WeightUnit.KILOGRAM -> WeightUnitDto.kg
          },
        )
      }
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
