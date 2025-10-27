package nl.eazysoftware.eazyrecyclingservice.repository.transport

import jakarta.persistence.EntityManager
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TruckRepository
import nl.eazysoftware.eazyrecyclingservice.repository.WasteContainerRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ContainerTransportMapper(
  private val companyRepository: CompanyRepository,
  private val pickupLocationMapper: PickupLocationMapper,
  private val truckRepository: TruckRepository,
  private val containerRepository: WasteContainerRepository,
  private val entityManager: EntityManager,
) {

  fun toDomain(dto: TransportDto): ContainerTransport {
    return ContainerTransport(
      transportId = TransportId(dto.id!!),
      displayNumber = TransportDisplayNumber(dto.displayNumber ?: ""),
      consignorParty = CompanyId(dto.consignorParty.id!!),
      carrierParty = CompanyId(dto.carrierParty.id!!),
      pickupLocation = pickupLocationMapper.toDomain(dto.pickupLocation),
      pickupDateTime = dto.pickupDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toKotlinInstant(),
      deliveryLocation = pickupLocationMapper.toDomain(dto.deliveryLocation),
      deliveryDateTime = dto.deliveryDateTime?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toKotlinInstant()
        ?: kotlinx.datetime.Clock.System.now(),
      transportType = dto.transportType,
      wasteContainer = dto.wasteContainer?.let { WasteContainerId(it.uuid!!) },
      truck = dto.truck?.let { LicensePlate(it.licensePlate) },
      driver = dto.driver?.let { UserId(it.id) },
      note = Note(dto.note),
      transportHours = dto.transportHours?.let { kotlin.time.Duration.parse("${it}h") },
      updatedAt = dto.updatedAt?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toKotlinInstant(),
      sequenceNumber = dto.sequenceNumber
    )
  }

  fun toDto(domain: ContainerTransport): TransportDto {
    val consignorCompany = companyRepository.findByIdOrNull(domain.consignorParty.uuid)
      ?: throw IllegalArgumentException("Consignor company not found: ${domain.consignorParty.uuid}")
    val carrierCompany = companyRepository.findByIdOrNull(domain.carrierParty.uuid)
      ?: throw IllegalArgumentException("Carrier company not found: ${domain.carrierParty.uuid}")

    return TransportDto(
      id = domain.transportId?.uuid,
      displayNumber = domain.displayNumber?.value,
      consignorParty = consignorCompany,
      carrierParty = carrierCompany,
      pickupLocation = pickupLocationMapper.toDto(domain.pickupLocation),
      pickupDateTime = domain.pickupDateTime.toJavaInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
      deliveryLocation = pickupLocationMapper.toDto(domain.deliveryLocation),
      deliveryDateTime = domain.deliveryDateTime.toJavaInstant().atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime(),
      transportType = domain.transportType,
      containerOperation = ContainerOperation.DELIVERY,
      wasteContainer = domain.wasteContainer?.let { containerRepository.findByIdOrNull(it.uuid) },
      truck = domain.truck?.let { truckRepository.findByIdOrNull(it.value) },
      driver = domain.driver?.let { entityManager.getReference(ProfileDto::class.java, it.uuid) },
      note = domain.note.description,
      transportHours = domain.transportHours?.inWholeHours?.toDouble(),
      updatedAt = domain.updatedAt?.toJavaInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDateTime(),
      sequenceNumber = domain.sequenceNumber
    )
  }
}
