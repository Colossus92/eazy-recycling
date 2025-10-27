package nl.eazysoftware.eazyrecyclingservice.application.query

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ContainerTransports
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProfileRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TruckRepository
import nl.eazysoftware.eazyrecyclingservice.repository.WasteContainerRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface GetTransportById {
  fun execute(transportId: UUID): TransportDetailView
}

@Service
@Transactional(readOnly = true)
class GetTransportByIdService(
  private val containerTransports: ContainerTransports,
  private val companyRepository: CompanyRepository,
  private val profileRepository: ProfileRepository,
  private val wasteContainerRepository: WasteContainerRepository,
  private val truckRepository: TruckRepository
) : GetTransportById {

  override fun execute(transportId: UUID): TransportDetailView {
    val id = TransportId(transportId)

    val containerTransport = containerTransports.findById(id)
      ?: throw EntityNotFoundException("Transport met id $transportId niet gevonden")

    return TransportDetailView(
      id = containerTransport.transportId.uuid,
      displayNumber = containerTransport.displayNumber?.value,
      consignorParty = mapCompany(containerTransport.consignorParty.uuid),
      carrierParty = mapCompany(containerTransport.carrierParty.uuid),
      pickupLocation = mapLocation(containerTransport.pickupLocation),
      pickupDateTime = containerTransport.pickupDateTime,
      deliveryLocation = mapLocation(containerTransport.deliveryLocation),
      deliveryDateTime = containerTransport.deliveryDateTime,
      transportType = containerTransport.transportType.name,
      status = containerTransport.getStatus(),
      truck = containerTransport.truck?.let { mapTruck(it) },
      driver = containerTransport.driver?.let { mapDriver(it.uuid) },
      note = containerTransport.note.description,
      transportHours = containerTransport.transportHours?.inWholeSeconds?.toDouble()?.div(3600),
      sequenceNumber = containerTransport.sequenceNumber,
      updatedAt = containerTransport.updatedAt,
      wasteContainer = containerTransport.wasteContainer?.let { mapWasteContainer(it.uuid) }
    )
  }

  private fun mapLocation(location: Location): PickupLocationView {
    return when (location) {
      is Location.DutchAddress -> PickupLocationView.DutchAddressView(
        streetName = location.streetName(),
        postalCode = location.postalCode().value,
        buildingNumber = location.buildingNumber(),
        buildingNumberAddition = location.buildingNumberAddition(),
        city = location.city(),
        country = location.country()
      )

      is Location.ProximityDescription -> PickupLocationView.ProximityDescriptionView(
        postalCodeDigits = location.postalCodeDigits,
        city = location.city,
        description = location.description,
        country = location.country
      )

      is Location.Company -> PickupLocationView.PickupCompanyView(
        company = mapCompany(location.companyId.uuid)
      )

      is Location.ProjectLocation -> PickupLocationView.ProjectLocationView(
        company = mapCompany(location.companyId.uuid),
        streetName = location.streetName(),
        postalCode = location.postalCode().value,
        buildingNumber = location.buildingNumber(),
        buildingNumberAddition = location.buildingNumberAddition(),
        city = location.city(),
        country = location.country()
      )

      is Location.NoLocation -> PickupLocationView.NoPickupView()
    }
  }

  private fun mapCompany(companyId: UUID): CompanyView {
    val company = companyRepository.findByIdOrNull(companyId)
      ?: throw EntityNotFoundException("Bedrijf met id $companyId niet gevonden")

    return CompanyView(
      id = company.id!!,
      name = company.name,
      chamberOfCommerceId = company.chamberOfCommerceId,
      vihbId = company.vihbId,
      processorId = company.processorId,
      address = AddressView(
        street = company.address.streetName ?: "Niet bekend",
        houseNumber = company.address.buildingNumber,
        houseNumberAddition = company.address.buildingName,
        postalCode = company.address.postalCode,
        city = company.address.city ?: "",
        country = company.address.country ?: "Nederland"
      )
    )
  }

  private fun mapDriver(driverId: UUID): DriverView {
    val driver = profileRepository.findByIdOrNull(driverId)
      ?: throw EntityNotFoundException("Chauffeur met id $driverId niet gevonden")

    return DriverView(
      id = driver.id,
      name = driver.firstName + " " + driver.lastName,
    )
  }

  private fun mapTruck(licensePlate: LicensePlate): TruckView {
    val truck = truckRepository.findByIdOrNull(licensePlate.value)
      ?: throw EntityNotFoundException("Truck met id $licensePlate niet gevonden")

    return TruckView(
      licensePlate = licensePlate.value,
      brand = truck.brand ?: "",
      model = truck.model ?: "",
    )
  }

  private fun mapWasteContainer(containerId: UUID): WasteContainerView {
    val container = wasteContainerRepository.findByIdOrNull(containerId)
      ?: throw EntityNotFoundException("Container met id $containerId niet gevonden")

    return WasteContainerView(
      uuid = container.uuid!!,
      containerNumber = container.id,
    )
  }
}
