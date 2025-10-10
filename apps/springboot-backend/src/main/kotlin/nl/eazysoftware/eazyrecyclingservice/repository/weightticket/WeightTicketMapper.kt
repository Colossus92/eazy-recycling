package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.model.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.model.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.model.WeightTicketStatus
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Goods
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamMapper
import org.springframework.stereotype.Component

@Component
class WeightTicketMapper(
  private val companyRepository: CompanyRepository,
  private val wasteStreamMapper: WasteStreamMapper,
) {

  fun toDomain(dto: WeightTicketDto): WeightTicket {
    return WeightTicket(
      id = WeightTicketId(dto.id),
      carrierParty = CompanyId(dto.carrierParty.id!!),
      consignorParty = CompanyId(dto.consignorParty.id!!),
      truck = dto.truckLicensePlate?.let { LicensePlate(it) },
      goods = dto.goods.map { weightTicketGoodsDto ->
        Goods(
          id = weightTicketGoodsDto.id!!,
          waste = wasteStreamMapper.toDomain(weightTicketGoodsDto.wasteStream),
          weight = Weight(
            weightTicketGoodsDto.weight,
            Weight.WeightUnit.valueOf(weightTicketGoodsDto.unit)
          )
        )
      },
      reclamation = dto.reclamation,
      note = dto.note?.let { Note(it) },
      status = toDomainStatus(dto.status),
      createdAt = dto.createdAt,
      updatedAt = dto.updatedAt,
      weightedAt = dto.weightedAt
    )
  }

  fun toDto(domain: WeightTicket): WeightTicketDto {
    val carrierParty = companyRepository.findById(domain.carrierParty.uuid)
      .orElseThrow { IllegalStateException("Transporteur niet gevonden: ${domain.carrierParty.uuid}") }

    val consignorParty = companyRepository.findById(domain.consignorParty.uuid)
      .orElseThrow { IllegalStateException("Opdrachtgever niet gevonden: ${domain.consignorParty.uuid}") }

    // Step 1: Create the parent DTO without goods
    val weightTicketDto = WeightTicketDto(
      id = domain.id.number,
      consignorParty = consignorParty,
      goods = mutableListOf(),
      carrierParty = carrierParty,
      truckLicensePlate = domain.truck?.value,
      reclamation = domain.reclamation,
      note = domain.note?.description,
      status = toDtoStatus(domain.status),
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
      weightedAt = domain.weightedAt
    )

    // Step 2: Create goods with reference to the parent and add them to the collection
    val goodsDtos = domain.goods.map { goods ->
      WeightTicketGoodsDto(
        id = goods.id,
        weightTicket = weightTicketDto,
        wasteStream = wasteStreamMapper.toDto(goods.waste),
        weight = goods.weight.value,
        unit = goods.weight.unit.name
      )
    }
    weightTicketDto.goods.addAll(goodsDtos)

    return weightTicketDto
  }

  private fun toDomainStatus(dto: WeightTicketStatusDto): WeightTicketStatus {
    return when (dto) {
      WeightTicketStatusDto.DRAFT -> WeightTicketStatus.DRAFT
      WeightTicketStatusDto.PROCESSED -> WeightTicketStatus.PROCESSED
      WeightTicketStatusDto.COMPLETED -> WeightTicketStatus.COMPLETED
      WeightTicketStatusDto.CANCELLED -> WeightTicketStatus.CANCELLED
    }
  }

  private fun toDtoStatus(domain: WeightTicketStatus): WeightTicketStatusDto {
    return when (domain) {
      WeightTicketStatus.DRAFT -> WeightTicketStatusDto.DRAFT
      WeightTicketStatus.PROCESSED -> WeightTicketStatusDto.PROCESSED
      WeightTicketStatus.COMPLETED -> WeightTicketStatusDto.COMPLETED
      WeightTicketStatus.CANCELLED -> WeightTicketStatusDto.CANCELLED
    }
  }
}
