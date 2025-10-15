package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.config.clock.toJavaInstant
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketStatus
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class WeightTicketMapper(
  private val companyRepository: CompanyRepository,
  private val wasteStreamMapper: WasteStreamMapper,
) {

  fun toDomain(dto: WeightTicketDto): WeightTicket {
    return WeightTicket(
      id = WeightTicketId(dto.id),
      carrierParty = dto.carrierParty?.id?.let { CompanyId(it) },
      consignorParty = Consignor.Company(CompanyId(dto.consignorParty.id!!)),
      truckLicensePlate = dto.truckLicensePlate?.let { LicensePlate(it) },
      reclamation = dto.reclamation,
      note = dto.note?.let { Note(it) },
      status = toDomainStatus(dto.status),
      createdAt = dto.createdAt.toKotlinInstant(),
      updatedAt = dto.updatedAt?.toKotlinInstant(),
      weightedAt = dto.weightedAt?.toKotlinInstant()
    )
  }

  fun toDto(domain: WeightTicket): WeightTicketDto {
    val carrierParty = domain.carrierParty?.uuid
      ?.let { companyRepository.findByIdOrNull(it) ?: throw IllegalStateException("Transporteur niet gevonden: ${it}") }

    val consignorParty = when (domain.consignorParty) {
      is Consignor.Company -> companyRepository.findByIdOrNull(domain.consignorParty.id.uuid)
        ?: throw IllegalArgumentException("Opdrachtgever niet gevonden: ${domain.consignorParty.id.uuid}")

      Consignor.Person -> throw IllegalArgumentException("Particuliere opdrachtgever wordt nog niet ondersteund.")
    }

    // Step 1: Create the parent DTO without goods
    val weightTicketDto = WeightTicketDto(
      id = domain.id.number,
      consignorParty = consignorParty,
      carrierParty = carrierParty,
      truckLicensePlate = domain.truckLicensePlate?.value,
      reclamation = domain.reclamation,
      note = domain.note?.description,
      status = toDtoStatus(domain.status),
      createdAt = domain.createdAt.toJavaInstant(),
      updatedAt = domain.updatedAt?.toJavaInstant(),
      weightedAt = domain.weightedAt?.toJavaInstant()
    )

    return weightTicketDto
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
