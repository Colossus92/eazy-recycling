package nl.eazysoftware.eazyrecyclingservice.application.query

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.application.query.mappers.CompanyViewMapper
import nl.eazysoftware.eazyrecyclingservice.application.query.mappers.PickupLocationViewMapper
import nl.eazysoftware.eazyrecyclingservice.application.query.mappers.WasteContainerViewMapper
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayString
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ContainerTransports
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import nl.eazysoftware.eazyrecyclingservice.domain.service.WasteContainerService
import nl.eazysoftware.eazyrecyclingservice.repository.ProfileRepository
import nl.eazysoftware.eazyrecyclingservice.repository.truck.TruckJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.time.toJavaInstant

interface GetTransportById {
  fun execute(transportId: UUID): TransportDetailView
}

@Service
@Transactional(readOnly = true)
class GetTransportByIdService(
  private val containerTransports: ContainerTransports,
  private val wasteTransports: WasteTransports,
  private val wasteStreams: WasteStreams,
  private val companies: Companies,
  private val profileRepository: ProfileRepository,
  private val truckJpaRepository: TruckJpaRepository,
  private val wasteContainerService: WasteContainerService,
  private val pickupLocationViewMapper: PickupLocationViewMapper,
  private val wasteContainerViewMapper: WasteContainerViewMapper,
  private val companyViewMapper: CompanyViewMapper,
) : GetTransportById {

  override fun execute(transportId: UUID): TransportDetailView {
    val id = TransportId(transportId)

    return wasteTransports.findById(id)
      ?.let { toView(it) }
      ?: containerTransports.findById(id)
        ?.let { toView(it) }
      ?: throw EntityNotFoundException("Transport met id $transportId niet gevonden")
  }

  private fun mapCompany(companyId: CompanyId): CompanyView {
    val company = companies.findById(companyId)
      ?: throw EntityNotFoundException("Bedrijf met id $companyId niet gevonden")

    return companyViewMapper.map(company)
  }

  private fun mapCompany(processorPartyId: ProcessorPartyId): CompanyView {
    val company = companies.findByProcessorId(processorPartyId.number)
      ?: throw EntityNotFoundException("Bedrijf met verwerkersnummer $processorPartyId niet gevonden")

    return companyViewMapper.map(company)
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
    val truck = truckJpaRepository.findByIdOrNull(licensePlate.value)
      ?: throw EntityNotFoundException("Truck met id $licensePlate niet gevonden")

    return TruckView(
      licensePlate = licensePlate.value,
      brand = truck.brand ?: "",
      description = truck.description ?: "",
      displayName = truck.getDisplayName()
    )
  }

  private fun mapWasteContainer(containerId: String) =
    wasteContainerViewMapper.map(wasteContainerService.getContainerById(containerId))

  private fun mapGoodsItem(goodsItem: GoodsItem): GoodsItemView {
    val wasteStream = wasteStreams.findByNumber(goodsItem.wasteStreamNumber)
      ?: throw IllegalStateException("Geen afvalstroomnummer ${goodsItem.wasteStreamNumber} gevonden")
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
    consignorParty = mapCompany(containerTransport.consignorParty),
    carrierParty = mapCompany(containerTransport.carrierParty),
    pickupLocation = pickupLocationViewMapper.mapLocation(containerTransport.pickupLocation),
    pickupDateTime = containerTransport.pickupDateTime.toDisplayString(),
    deliveryLocation = pickupLocationViewMapper.mapLocation(containerTransport.deliveryLocation),
    deliveryDateTime = containerTransport.deliveryDateTime?.toDisplayString(),
    transportType = containerTransport.transportType.name,
    status = containerTransport.getStatus(),
    truck = containerTransport.truck?.let { mapTruck(it) },
    driver = containerTransport.driver?.let { mapDriver(it.uuid) },
    note = containerTransport.note?.description,
    transportHours = containerTransport.transportHours?.inWholeSeconds?.toDouble()?.div(3600),
    driverNote = containerTransport.driverNote?.description,
    sequenceNumber = containerTransport.sequenceNumber,
    createdAt = containerTransport.createdAt?.toJavaInstant(),
    createdByName = containerTransport.createdBy,
    updatedAt = containerTransport.updatedAt?.toJavaInstant(),
    updatedByName = containerTransport.updatedBy,
    wasteContainer = containerTransport.wasteContainer?.let { mapWasteContainer(it.id) },
    containerOperation = containerTransport.containerOperation,
  )

  private fun toView(wasteTransport: WasteTransport): TransportDetailView {
    val wasteStream = wasteTransport.goods
      .first()
      .let {
        wasteStreams.findByNumber(it.wasteStreamNumber)
          ?: throw IllegalStateException("Geen afvalstroomnummer ${wasteTransport.goods.first().wasteStreamNumber} gevonden")
      }
    return TransportDetailView(
      id = wasteTransport.transportId.uuid,
      displayNumber = wasteTransport.displayNumber?.value,
      consignorParty = when (val consignor = wasteStream.consignorParty) {
        is Consignor.Company -> mapCompany(consignor.id)
        else -> throw IllegalArgumentException("Op dit moment worden alleen bedrijven als opdrachtgever ondersteund")
      },
      carrierParty = mapCompany(wasteTransport.carrierParty),
      pickupLocation = pickupLocationViewMapper.mapLocation(wasteStream.pickupLocation),
      pickupDateTime = wasteTransport.pickupDateTime.toDisplayString(),
      deliveryLocation = mapWasteDeliveryLocation(wasteStream.deliveryLocation),
      deliveryDateTime = wasteTransport.deliveryDateTime?.toDisplayString(),
      transportType = wasteTransport.transportType.name,
      status = wasteTransport.getStatus(),
      truck = wasteTransport.truck?.let { mapTruck(it) },
      driver = wasteTransport.driver?.let { mapDriver(it.uuid) },
      note = wasteTransport.note?.description,
      transportHours = wasteTransport.transportHours?.inWholeSeconds?.toDouble()?.div(3600),
      driverNote = wasteTransport.driverNote?.description,
      sequenceNumber = wasteTransport.sequenceNumber,
      createdAt = wasteTransport.createdAt?.toJavaInstant(),
      createdByName = wasteTransport.createdBy,
      updatedAt = wasteTransport.updatedAt?.toJavaInstant(),
      updatedByName = wasteTransport.updatedBy,
      wasteContainer = wasteTransport.wasteContainer?.let { mapWasteContainer(it.id) },
      containerOperation = wasteTransport.containerOperation,
      goodsItem = wasteTransport.goods.map { mapGoodsItem(it) },
      consigneeParty = mapCompany(wasteStream.deliveryLocation.processorPartyId),
      pickupParty = mapCompany(wasteStream.pickupParty),
      weightTicketId = wasteTransport.weightTicketId?.number,
    )
  }

  private fun mapWasteDeliveryLocation(deliveryLocation: WasteDeliveryLocation) = PickupLocationView.PickupCompanyView(
    company = mapCompany(deliveryLocation.processorPartyId)
  )

}
