package nl.eazysoftware.eazyrecyclingservice.repository.transport

import jakarta.persistence.EntityManager
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.LocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TruckRepository
import nl.eazysoftware.eazyrecyclingservice.repository.WasteContainerRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ContainerTransportMapper(
  private val companyRepository: CompanyRepository,
  private val locationRepository: LocationRepository,
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
      pickupLocation = mapLocationToDomain(dto.pickupLocation),
      pickupDateTime = dto.pickupDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toKotlinInstant(),
      deliveryLocation = mapLocationToDomain(dto.deliveryLocation),
      deliveryDateTime = dto.deliveryDateTime?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toKotlinInstant()
        ?: kotlinx.datetime.Clock.System.now(),
      transportType = dto.transportType,
      wasteContainer = dto.wasteContainer?.let { WasteContainerId(it.uuid!!) },
      truck = dto.truck?.let { LicensePlate(it.licensePlate) },
      driver = dto.driver?.let { UserId(it.id) },
      note = Note(dto.note),
      transportHours = dto.transportHours?.let { kotlin.time.Duration.parse("${it}h") },
      updatedAt = dto.updatedAt!!.atZone(java.time.ZoneId.systemDefault()).toInstant().toKotlinInstant(),
      sequenceNumber = dto.sequenceNumber
    )
  }

  fun toDto(domain: ContainerTransport): TransportDto {
    val consignorCompany = companyRepository.findByIdOrNull(domain.consignorParty.uuid)
      ?: throw IllegalArgumentException("Consignor company not found: ${domain.consignorParty.uuid}")
    val carrierCompany = companyRepository.findByIdOrNull(domain.carrierParty.uuid)
      ?: throw IllegalArgumentException("Carrier company not found: ${domain.carrierParty.uuid}")

    val pickupLocationDto = mapLocationToDto(domain.pickupLocation)
    val deliveryLocationDto = mapLocationToDto(domain.deliveryLocation)

    return TransportDto(
      id = domain.transportId?.uuid,
      displayNumber = domain.displayNumber?.value,
      consignorParty = consignorCompany,
      carrierParty = carrierCompany,
      pickupCompany = when (domain.pickupLocation) {
        is Location.Company -> entityManager.getReference(CompanyDto::class.java, domain.pickupLocation.companyId)
        else -> null
      },
      pickupCompanyBranch = null,
      pickupLocation = pickupLocationDto,
      pickupDateTime = domain.pickupDateTime.toJavaInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
      deliveryCompany = when (domain.deliveryLocation) {
        is Location.Company -> entityManager.getReference(CompanyDto::class.java, domain.deliveryLocation.companyId)
        else -> null
      },
      deliveryCompanyBranch = null,
      deliveryLocation = deliveryLocationDto,
      deliveryDateTime = domain.deliveryDateTime.toJavaInstant().atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime(),
      transportType = domain.transportType,
      containerOperation = ContainerOperation.DELIVERY,
      wasteContainer = domain.wasteContainer?.let { containerRepository.findByIdOrNull(it.uuid) },
      truck = domain.truck?.let { truckRepository.findByIdOrNull(it.value) },
      driver = domain.driver?.let { entityManager.getReference(ProfileDto::class.java, it.uuid) },
      note = domain.note.description,
      goods = null, // Container transport has no goods
      transportHours = domain.transportHours?.inWholeHours?.toDouble(),
      updatedAt = domain.updatedAt.toJavaInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
      sequenceNumber = domain.sequenceNumber
    )
  }

  private fun mapLocationToDomain(locationDto: LocationDto): Location {
    // Simple mapping - in real implementation you'd need to determine the type
    return Location.DutchAddress(
      address = Address(

        streetName = locationDto.address.streetName ?: "",
        postalCode = DutchPostalCode(locationDto.address.postalCode),
        buildingNumber = locationDto.address.buildingNumber,
        buildingNumberAddition = null,
        city = locationDto.address.city ?: "",
        country = locationDto.address.country ?: "",
      )
    )
  }

  //TODO allow for different types in database
  private fun mapLocationToDto(location: Location): LocationDto {
    return when (location) {
      is Location.DutchAddress -> {
        val existingLocation = locationRepository.findByAddress_PostalCodeAndAddress_BuildingNumber(
          location.postalCode().value,
          location.buildingNumber(),
        )

        existingLocation ?: locationRepository.save(
          LocationDto(
            id = java.util.UUID.randomUUID().toString(),
            address = AddressDto(
              streetName = location.streetName(),
              buildingNumber = location.buildingNumber(),
              postalCode = location.postalCode().value,
              city = location.city(),
              country = location.country()
            )
          )
        )
      }

      is Location.ProximityDescription -> {
        locationRepository.save(
          LocationDto(
            id = java.util.UUID.randomUUID().toString(),
            address = AddressDto(
              streetName = location.description,
              buildingNumber = "",
              postalCode = location.postalCodeDigits,
              city = location.city,
              country = location.country
            )
          )
        )
      }

      is Location.Company -> {
        val company = companyRepository.findByIdOrNull(location.companyId.uuid)
          ?: throw IllegalArgumentException("Company not found: ${location.companyId.uuid}")
        // Map company address to location
        locationRepository.save(
          LocationDto(
            id = java.util.UUID.randomUUID().toString(),
            address = AddressDto(
              streetName = company.address.streetName ?: "",
              buildingNumber = company.address.buildingNumber,
              postalCode = company.address.postalCode,
              city = company.address.city ?: "",
              country = company.address.country ?: "Nederland"
            )
          )
        )
      }

      is Location.ProjectLocation -> {
        val company = companyRepository.findByIdOrNull(location.companyId.uuid)
          ?: throw IllegalArgumentException("Company not found: ${location.companyId.uuid}")
        // Map company address to location
        locationRepository.save(
          LocationDto(
            id = java.util.UUID.randomUUID().toString(),
            address = AddressDto(
              streetName = company.address.streetName ?: "",
              buildingNumber = company.address.buildingNumber,
              postalCode = company.address.postalCode,
              city = company.address.city ?: "",
              country = company.address.country ?: "Nederland"
            )
          )
        )
      }

      is Location.NoLocation -> {
        locationRepository.save(
          LocationDto(
            id = java.util.UUID.randomUUID().toString(),
            address = AddressDto(
              streetName = "Geen locatie",
              buildingNumber = "",
              postalCode = "0000",
              city = "",
              country = "Nederland"
            )
          )
        )
      }
    }
  }
}
