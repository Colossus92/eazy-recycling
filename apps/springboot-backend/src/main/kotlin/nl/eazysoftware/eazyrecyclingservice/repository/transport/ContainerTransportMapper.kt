package nl.eazysoftware.eazyrecyclingservice.repository.transport

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerTransport
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TimingConstraint
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportDisplayNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportId
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TimingConstraintDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.truck.TruckJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.wastecontainer.WasteContainerJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import kotlin.time.toKotlinInstant

@Component
class ContainerTransportMapper(
  private val companies: Companies,
  private val pickupLocationMapper: PickupLocationMapper,
  private val companyMapper: CompanyMapper,
  private val truckJpaRepository: TruckJpaRepository,
  private val containerRepository: WasteContainerJpaRepository,
  private val entityManager: EntityManager,
) {

  fun toDomain(dto: TransportDto): ContainerTransport {
    return ContainerTransport(
      transportId = TransportId(dto.id),
      displayNumber = TransportDisplayNumber(dto.displayNumber ?: ""),
      consignorParty = CompanyId(dto.consignorParty.id),
      carrierParty = CompanyId(dto.carrierParty.id),
      pickupLocation = pickupLocationMapper.toDomain(dto.pickupLocation),
      deliveryLocation = pickupLocationMapper.toDomain(dto.deliveryLocation),
      transportType = dto.transportType,
      wasteContainer = dto.wasteContainer?.let { WasteContainerId(it.id) },
      containerOperation = dto.containerOperation,
      truck = dto.truck?.let { LicensePlate(it.licensePlate) },
      driver = dto.driver?.let { UserId(it.id) },
      note = dto.note?.let { Note(it) },
      transportHours = dto.transportHours?.let { kotlin.time.Duration.parse("${it}h") },
      driverNote = dto.driverNote?.let { Note(it) },
      createdAt = dto.createdAt?.toKotlinInstant(),
      createdBy = dto.createdBy,
      updatedAt = dto.updatedAt?.toKotlinInstant(),
      updatedBy = dto.updatedBy,
      sequenceNumber = dto.sequenceNumber,
      pickupTimingConstraint = dto.pickupTiming?.toDomain(),
      deliveryTimingConstraint = dto.deliveryTiming?.toDomain()
    )
  }

  fun toDto(domain: ContainerTransport): TransportDto {
    val consignorCompany = companies.findById(domain.consignorParty)
      ?: throw IllegalArgumentException("Consignor company not found: ${domain.consignorParty.uuid}")
    val carrierCompany = companies.findById(domain.carrierParty)
      ?: throw IllegalArgumentException("Carrier company not found: ${domain.carrierParty.uuid}")

    val pickupTiming = domain.pickupTimingConstraint?.let { TimingConstraintDto.fromDomain(it) }
    val deliveryTiming = domain.deliveryTimingConstraint?.let { TimingConstraintDto.fromDomain(it) }

    return TransportDto(
      id = domain.transportId.uuid,
      displayNumber = domain.displayNumber?.value,
      consignorParty = companyMapper.toDto(consignorCompany),
      carrierParty = companyMapper.toDto(carrierCompany),
      pickupLocation = pickupLocationMapper.toDto(domain.pickupLocation),
      pickupTiming = pickupTiming,
      deliveryLocation = pickupLocationMapper.toDto(domain.deliveryLocation),
      deliveryTiming = deliveryTiming,
      transportType = domain.transportType,
      containerOperation = domain.containerOperation,
      wasteContainer = domain.wasteContainer?.let { containerRepository.findByIdOrNull(it.id) },
      truck = domain.truck?.let { truckJpaRepository.findByIdOrNull(it.value) },
      driver = domain.driver?.let { entityManager.getReference(ProfileDto::class.java, it.uuid) },
      note = domain.note?.description,
      transportHours = domain.transportHours?.inWholeHours?.toDouble(),
      driverNote = domain.driverNote?.description,
      sequenceNumber = domain.sequenceNumber
    )
  }
}
