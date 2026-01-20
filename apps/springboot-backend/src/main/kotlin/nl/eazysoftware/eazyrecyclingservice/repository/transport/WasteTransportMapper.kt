package nl.eazysoftware.eazyrecyclingservice.repository.transport

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.TransportGoodsDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TimingConstraintDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.TruckDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastecontainer.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamRepository
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.WeightTicketDto
import org.springframework.stereotype.Component
import kotlin.time.toKotlinInstant

@Component
class WasteTransportMapper(
  private val wasteStreamRepository: WasteStreamRepository,
  private val locationMapper: PickupLocationMapper,
  private val companies: Companies,
  private val entityManager: EntityManager,
) {

  fun toDto(domain: WasteTransport): TransportDto {
    val firstWasteStreamNumber = domain.goods.first().wasteStreamNumber
    val wasteStream = wasteStreamRepository.findByNumber(firstWasteStreamNumber)
      ?: throw IllegalArgumentException("Afvalstroomnummer niet gevonden: ${firstWasteStreamNumber.number}")
    val deliveryLocation = companies.findByProcessorId(wasteStream.deliveryLocation.processorPartyId.number)
      ?.let {
        it.companyId.let { id ->
          Location.Company(
            id,
            name = it.name,
            address = it.address,
          )
        }
      }
      ?: throw IllegalArgumentException("Verwerker niet gevonden: ${wasteStream.deliveryLocation.processorPartyId.number}")

    val pickupTiming = domain.pickupTimingConstraint?.let { TimingConstraintDto.fromDomain(it) }
    val deliveryTiming = domain.deliveryTimingConstraint?.let { TimingConstraintDto.fromDomain(it) }

    return TransportDto(
      id = domain.transportId.uuid,
      displayNumber = domain.displayNumber?.value,
      pickupTiming = pickupTiming,
      deliveryTiming = deliveryTiming,
      transportType = domain.transportType,
      containerOperation = domain.containerOperation,
      wasteContainer = domain.wasteContainer?.let {
        entityManager.getReference(
          WasteContainerDto::class.java,
          it.id
        )
      },
      truck = domain.truck?.let { entityManager.getReference(TruckDto::class.java, it.value) },
      driver = domain.driver?.let { entityManager.getReference(ProfileDto::class.java, it.uuid) },
      note = domain.note?.description,
      goods = toDto(domain.goods),
      transportHours = domain.transportHours?.inWholeHours?.toDouble(),
      sequenceNumber = domain.sequenceNumber,
      carrierParty = domain.carrierParty.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
      consignorParty = when (val consignor = wasteStream.consignorParty) {
        is Consignor.Company -> entityManager.getReference(CompanyDto::class.java, consignor.id.uuid)
        is Consignor.Person -> throw IllegalArgumentException("Person consignor is not yet supported in persistence layer")
      },
      driverNote = domain.driverNote?.description,
      pickupLocation = locationMapper.toDto(wasteStream.pickupLocation),
      deliveryLocation = locationMapper.toDto(deliveryLocation),
      weightTicket = domain.weightTicketId?.let { entityManager.getReference(WeightTicketDto::class.java, it.id) },
    )
  }

  fun toDomain(dto: TransportDto): WasteTransport {

    return WasteTransport(
      transportId = TransportId(dto.id),
      displayNumber = TransportDisplayNumber(dto.displayNumber ?: ""),
      carrierParty = CompanyId(dto.carrierParty.id),
      pickupTimingConstraint = dto.pickupTiming?.toDomain(),
      deliveryTimingConstraint = dto.deliveryTiming?.toDomain(),
      transportType = dto.transportType,
      goods = dto.goods
        ?.let { toDomain(it) }
        ?: throw IllegalStateException("Afvaltransport met nummer ${dto.displayNumber} heeft geen afval"),
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
      weightTicketId = dto.weightTicket?.let { WeightTicketId(it.id, it.number) },
    )
  }

  private fun toDto(goodsItem: List<GoodsItem>) =
    goodsItem.map {
      TransportGoodsDto(
        wasteStreamNumber = it.wasteStreamNumber.number,
        netNetWeight = it.netNetWeight,
        unit = it.unit,
        quantity = it.quantity,
      )
    }

  private fun toDomain(transportGoodsDto: List<TransportGoodsDto>) =
    transportGoodsDto.map {
      GoodsItem(
        wasteStreamNumber = WasteStreamNumber(it.wasteStreamNumber),
        netNetWeight = it.netNetWeight,
        unit = it.unit,
        quantity = it.quantity
      )
    }
}
