package nl.eazysoftware.eazyrecyclingservice.application.query

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayString
import nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer.toView
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ContainerTransports
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import nl.eazysoftware.eazyrecyclingservice.domain.service.WasteContainerService
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProfileRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TruckRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface GetTransportById {
  fun execute(transportId: UUID): TransportDetailView
}

@Service
@Transactional(readOnly = true)
class GetTransportByIdService(
  private val containerTransports: ContainerTransports,
  private val wasteTransports: WasteTransports,
  private val wasteStreams: WasteStreams,
  private val companyRepository: CompanyRepository,
  private val profileRepository: ProfileRepository,
  private val truckRepository: TruckRepository,
  private val wasteContainerService: WasteContainerService,
) : GetTransportById {

  override fun execute(transportId: UUID): TransportDetailView {
    val id = TransportId(transportId)

    return wasteTransports.findById(id)
      ?.let { toView(it) }
      ?: containerTransports.findById(id)
        ?.let { toView(it) }
      ?: throw EntityNotFoundException("Transport met id $transportId niet gevonden")
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

      is Location.Company -> {
        val id = location.companyId.uuid
        val company = companyRepository.findByIdOrNull(id)
          ?: throw EntityNotFoundException("Bedrijf met id $id niet gevonden")

        return PickupLocationView.PickupCompanyView(
          company = CompanyView(
            id = id,
            name = location.name,
            chamberOfCommerceId = company.chamberOfCommerceId,
            vihbId = company.vihbId,
            processorId = company.processorId,
            address = AddressView(
              street = location.address.streetName,
              houseNumber = location.address.buildingNumber,
              houseNumberAddition = location.address.buildingNumberAddition,
              postalCode = location.address.postalCode.value,
              city = location.address.city,
              country = location.address.country
            )
          )
        )
      }

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

    return toCompanyView(company)
  }

  private fun mapCompany(processorPartyId: ProcessorPartyId): CompanyView {
    val company = companyRepository.findByProcessorId(processorPartyId.number)
      ?: throw EntityNotFoundException("Bedrijf met verwerkersnummer $processorPartyId niet gevonden")

    return toCompanyView(company)
  }

  private fun toCompanyView(company: CompanyDto): CompanyView {
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

  private fun mapWasteContainer(containerId: String) =
    wasteContainerService.getContainerById(containerId).toView()

  private fun mapGoodsItem(goodsItem: GoodsItem, wasteStream: WasteStream): GoodsItemView {
    return GoodsItemView(
      wasteStreamNumber = wasteStream.wasteStreamNumber.number,
      name = wasteStream.wasteType.name,
      netNetWeight = goodsItem.netNetWeight,
      unit = goodsItem.unit,
      quantity = goodsItem.quantity,
      euralCode = wasteStream.wasteType.euralCode.code,
      processingMethodCode = wasteStream.wasteType.processingMethod.code,
      consignorClassification = 1,
    )
  }

  private fun toView(containerTransport: ContainerTransport) = TransportDetailView(
    id = containerTransport.transportId.uuid,
    displayNumber = containerTransport.displayNumber?.value,
    consignorParty = mapCompany(containerTransport.consignorParty.uuid),
    carrierParty = mapCompany(containerTransport.carrierParty.uuid),
    pickupLocation = mapLocation(containerTransport.pickupLocation),
    pickupDateTime = containerTransport.pickupDateTime.toDisplayString(),
    deliveryLocation = mapLocation(containerTransport.deliveryLocation),
    deliveryDateTime = containerTransport.deliveryDateTime?.toDisplayString(),
    transportType = containerTransport.transportType.name,
    status = containerTransport.getStatus(),
    truck = containerTransport.truck?.let { mapTruck(it) },
    driver = containerTransport.driver?.let { mapDriver(it.uuid) },
    note = containerTransport.note.description,
    transportHours = containerTransport.transportHours?.inWholeSeconds?.toDouble()?.div(3600),
    sequenceNumber = containerTransport.sequenceNumber,
    updatedAt = containerTransport.updatedAt,
    wasteContainer = containerTransport.wasteContainer?.let { mapWasteContainer(it.id) },
    containerOperation = containerTransport.containerOperation,
  )

  private fun toView(wasteTransport: WasteTransport): TransportDetailView {
    val wasteStream = wasteStreams.findByNumber(wasteTransport.goodsItem.wasteStreamNumber)
      ?: throw IllegalStateException("Geen afvalstroomnummer ${wasteTransport.goodsItem.wasteStreamNumber} gevonden")
    return TransportDetailView(
      id = wasteTransport.transportId.uuid,
      displayNumber = wasteTransport.displayNumber?.value,
      consignorParty = when(val consignor = wasteStream.consignorParty) {
        is Consignor.Company -> mapCompany(consignor.id.uuid)
        else -> throw IllegalArgumentException("Op dit moment worden alleen bedrijven als opdrachtgever ondersteund")
      },
      carrierParty = mapCompany(wasteTransport.carrierParty.uuid),
      pickupLocation = mapLocation(wasteStream.pickupLocation),
      pickupDateTime = wasteTransport.pickupDateTime.toDisplayString(),
      deliveryLocation = mapWasteDeliveryLocation(wasteStream.deliveryLocation),
      deliveryDateTime = wasteTransport.deliveryDateTime?.toDisplayString(),
      transportType = wasteTransport.transportType.name,
      status = wasteTransport.getStatus(),
      truck = wasteTransport.truck?.let { mapTruck(it) },
      driver = wasteTransport.driver?.let { mapDriver(it.uuid) },
      note = wasteTransport.note.description,
      transportHours = wasteTransport.transportHours?.inWholeSeconds?.toDouble()?.div(3600),
      sequenceNumber = wasteTransport.sequenceNumber,
      updatedAt = wasteTransport.updatedAt,
      wasteContainer = wasteTransport.wasteContainer?.let { mapWasteContainer(it.id) },
      containerOperation = wasteTransport.containerOperation,
      goodsItem = mapGoodsItem(wasteTransport.goodsItem, wasteStream),
      consigneeParty = mapCompany(wasteStream.deliveryLocation.processorPartyId),
      pickupParty = mapCompany(wasteStream.pickupParty.uuid),
    )
  }

  private fun mapWasteDeliveryLocation(deliveryLocation: WasteDeliveryLocation) = PickupLocationView.PickupCompanyView(
      company = mapCompany(deliveryLocation.processorPartyId)
    )

}
