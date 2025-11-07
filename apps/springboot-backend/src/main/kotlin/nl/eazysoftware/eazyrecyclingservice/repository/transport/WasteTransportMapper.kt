package nl.eazysoftware.eazyrecyclingservice.repository.transport

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.config.clock.toCetKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastecontainer.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamRepository
import org.springframework.stereotype.Component
import java.time.ZoneId
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Component
class WasteTransportMapper(
  private val wasteStreamRepository: WasteStreamRepository,
  private val locationMapper: PickupLocationMapper,
  private val companies: Companies,
  private val entityManager: EntityManager,
) {

  fun toDto(domain: WasteTransport): TransportDto {
    val wasteStream = wasteStreamRepository.findByNumber(domain.goodsItem.wasteStreamNumber)
      ?: throw IllegalArgumentException("Afvalstroomnummer niet gevonden: ${domain.goodsItem.wasteStreamNumber.number}")
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

    return TransportDto(
      id = domain.transportId.uuid,
      displayNumber = domain.displayNumber?.value,
      pickupDateTime = domain.pickupDateTime.toJavaInstant(),
      deliveryDateTime = domain.deliveryDateTime?.toJavaInstant(),
      transportType = domain.transportType,
      containerOperation = domain.containerOperation,
      wasteContainer = domain.wasteContainer?.let {
        entityManager.getReference(
          WasteContainerDto::class.java,
          it.id
        )
      },
      truck = domain.truck?.let { entityManager.getReference(Truck::class.java, it.value) },
      driver = domain.driver?.let { entityManager.getReference(ProfileDto::class.java, it.uuid) },
      note = domain.note.description,
      goodsItem = toDto(domain.goodsItem),
      transportHours = domain.transportHours?.inWholeHours?.toDouble(),
      updatedAt = domain.updatedAt?.toJavaInstant()?.atZone(ZoneId.of("Europe/Amsterdam"))?.toLocalDateTime(),
      sequenceNumber = domain.sequenceNumber,
      carrierParty = domain.carrierParty.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
      consignorParty = when (val consignor = wasteStream.consignorParty) {
        is Consignor.Company -> entityManager.getReference(CompanyDto::class.java, consignor.id.uuid)
        is Consignor.Person -> throw IllegalArgumentException("Person consignor is not yet supported in persistence layer")
      },
      pickupLocation = locationMapper.toDto(wasteStream.pickupLocation),
      deliveryLocation = locationMapper.toDto(deliveryLocation),
    )
  }

  fun toDomain(dto: TransportDto): WasteTransport {

    return WasteTransport(
      transportId = TransportId(dto.id),
      displayNumber = TransportDisplayNumber(dto.displayNumber ?: ""),
      carrierParty = CompanyId(dto.carrierParty.id),
      pickupDateTime = dto.pickupDateTime.toKotlinInstant(),
      deliveryDateTime = dto.deliveryDateTime?.toKotlinInstant(),
      transportType = dto.transportType,
      goodsItem = dto.goodsItem
        ?.let { toDomain(it) }
        ?: throw IllegalStateException("Afvaltransport met nummer ${dto.displayNumber} heeft geen afval"),
      wasteContainer = dto.wasteContainer?.let { WasteContainerId(it.id) },
      containerOperation = dto.containerOperation,
      truck = dto.truck?.let { LicensePlate(it.licensePlate) },
      driver = dto.driver?.let { UserId(it.id) },
      note = Note(dto.note),
      transportHours = dto.transportHours?.let { kotlin.time.Duration.parse("${it}h") },
      updatedAt = dto.updatedAt?.toCetKotlinInstant(),
      sequenceNumber = dto.sequenceNumber,
    )
  }

  private fun toDto(goodsItem: GoodsItem) = GoodsItemDto(
    wasteStreamNumber = goodsItem.wasteStreamNumber.number,
    netNetWeight = goodsItem.netNetWeight,
    unit = goodsItem.unit,
    quantity = goodsItem.quantity
  )

  private fun toDomain(goodsItemDto: GoodsItemDto) = GoodsItem(
    wasteStreamNumber = WasteStreamNumber(goodsItemDto.wasteStreamNumber),
    netNetWeight = goodsItemDto.netNetWeight,
    unit = goodsItemDto.unit,
    quantity = goodsItemDto.quantity
  )
}
