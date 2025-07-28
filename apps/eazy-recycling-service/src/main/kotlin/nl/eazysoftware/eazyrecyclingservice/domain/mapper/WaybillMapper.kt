package nl.eazysoftware.eazyrecyclingservice.domain.mapper

import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.*
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.*
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ID
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.NetNetWeightMeasure
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.UUID
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_2.QuantityType
import oasis.names.specification.ubl.schema.xsd.waybill_2.Waybill
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import java.time.LocalDateTime

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
)
abstract class WaybillMapper {

    @Mapping(target = "note", expression = "java(toNote(waybill))")
    @Mapping(source = "ID", target = "goods.id")
    @Mapping(source = "UUID", target = "goods.uuid")
    @Mapping(target = "goods.consigneeParty", expression = "java(toConsigneeDto(waybill))")
    @Mapping(target = "goods.pickupParty", expression = "java(toPickupPartyDto(waybill))")
    @Mapping(target = "consignorParty", expression = "java(toConsignorPartyDto(waybill))")
    @Mapping(target = "carrierParty", expression = "java(toCarrierPartyDto(waybill))")
    @Mapping(target = "goods.goodsItem", expression = "java(toGoodsItemDto(waybill))")
    @Mapping(target = "deliveryLocation", expression = "java(toDeliveryLocationDto(waybill))")
    @Mapping(target = "deliveryCompany", ignore = true)
    @Mapping(target = "deliveryDateTime", expression = "java(toDeliveryDateTime(waybill))")
    @Mapping(target = "pickupLocation", expression = "java(toPickupLocationDto(waybill))")
    @Mapping(target = "pickupCompany", ignore = true)
    @Mapping(target = "pickupDateTime", expression = "java(toPickupDateTime(waybill))")
    @Mapping(target = "truck", expression = "java(toTruck(waybill))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transportType", expression = "java(waste())")
    @Mapping(target = "containerOperation", ignore = true)
    @Mapping(target = "displayNumber", ignore = true)
    @Mapping(target = "wasteContainer", ignore = true) // TODO resolve container
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "sequenceNumber", constant = "9999")
    @Mapping(target = "transportHours", ignore = true)
    abstract fun toDto(waybill: Waybill): TransportDto

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "ID", target = "wasteStreamNumber")
    @Mapping(source = "netNetWeightMeasure", target = "netNetWeight")
    @Mapping(target = "unit", expression = "java(toUnit(source))")
    @Mapping(target = "name", expression = "java(toName(source))")
    @Mapping(target = "euralCode", expression = "java(toEuralCode(source))")
    @Mapping(target = "processingMethodCode", expression = "java(toProcessingMethod(source))")
    abstract fun toDto(source: GoodsItemType): GoodsItemDto

    @Mapping(target = "id", expression = "java(toLocationId(source))")
    @Mapping(source = "locationTypeCode.value", target = "locationTypeCode")
    @Mapping(target = "description", expression = "java(toDescription(source))")
    abstract fun toDto(source: LocationType): LocationDto

    @Mapping(source = "streetName.value", target = "streetName")
    @Mapping(source = "buildingName.value", target = "buildingName")
    @Mapping(source = "buildingNumber.value", target = "buildingNumber")
    @Mapping(source = "cityName.value", target = "city")
    @Mapping(source = "postalZone.value", target = "postalCode")
    @Mapping(source = "country.name.value", target = "country")
    abstract fun toDto(postalAddress: AddressType): AddressDto

    fun waste(): TransportType {
        return TransportType.WASTE
    }

    fun toLocationId(source: LocationType): String {
        return source.id
            ?.value
            ?: (source.address.postalZone.value + source.address.buildingNumber.value)

    }

    fun toDto(source: ID): String {
        return source.value
    }

    fun toDto(source: UUID): String {
        return source.value
    }

    fun toConsigneeDto(waybill: Waybill): CompanyDto? {
        return toDto(waybill.shipment.consignments.first().consigneeParty)
    }

    fun toPickupPartyDto(waybill: Waybill): CompanyDto? {
        return toDto(waybill.shipment.goodsItems.first().pickup.pickupParty)
    }

    fun toConsignorPartyDto(waybill: Waybill): CompanyDto? {
        return toDto(waybill.shipment.consignments.first().consignorParty)
    }

    fun toCarrierPartyDto(waybill: Waybill): CompanyDto? {
        return toDto(waybill.shipment.consignments.first().carrierParty)
    }

    fun toGoodsItemDto(waybill: Waybill): GoodsItemDto {
        return toDto(waybill.shipment.goodsItems.first())
    }

    fun toDto(source: QuantityType): Int {
        return source.value?.toInt() ?: 0
    }

    fun toDto(source: NetNetWeightMeasure): Int {
        return source.value?.toInt() ?: 0
    }

    fun toUnit(source: GoodsItemType): String {
        return source.netNetWeightMeasure.unitCode
    }

    fun toName(source: GoodsItemType): String {
        return source.items.first().name.value
    }

    fun toEuralCode(source: GoodsItemType): String {
        return source.items.first().commodityClassifications.first().itemClassificationCode.value
    }

    fun toProcessingMethod(source: GoodsItemType): String? {
        return source.items.first()
            .additionalItemProperties.find { it.name.value == "vewerkingsmethode" }
            ?.value
            ?.value
    }

    fun toContainerNumber(source: GoodsItemType): String {
        return source.goodsItemContainers.first().id.value
    }

    fun toDeliveryLocationDto(source: Waybill): LocationDto {
        return toDto(source.shipment.goodsItems.first().delivery.deliveryLocation)
    }

    fun toPickupLocationDto(source: Waybill): LocationDto {
        return toDto(source.shipment.goodsItems.first().pickup.pickupLocation)
    }

    fun toDeliveryDateTime(source: Waybill): LocalDateTime {
        val estimatedDeliveryPeriod = source.shipment.goodsItems.first().delivery.estimatedDeliveryPeriod
        val deliveryDate = estimatedDeliveryPeriod.endDate.value
        val deliveryTime = estimatedDeliveryPeriod.endTime.value

        return LocalDateTime.of(deliveryDate, deliveryTime)
    }

    fun toPickupDateTime(source: Waybill): LocalDateTime {
        val pickup = source.shipment.goodsItems.first().pickup
        val deliveryDate = pickup.actualPickupDate.value
        val deliveryTime = pickup.actualPickupTime.value

        return LocalDateTime.of(deliveryDate, deliveryTime)
    }

    fun toTruck(source: Waybill): Truck {
        val licensePlate = source.shipment.shipmentStages.first().transportMeans.roadTransport.licensePlateID.value
        return Truck(licensePlate = licensePlate)
    }

    fun toNote(source: Waybill): String {
        return source.notes.joinToString { it.value }
    }

    fun toDescription(source: LocationType): String {
        return source.descriptions.joinToString { it.value }
    }

    private fun toDto(source: PartyType?): CompanyDto? {
        return source
            ?.let {
                val chamberOfCommerceId: String? = getChamberOfCommerceId(it)
                val vihbId: String? = getVihbId(it)


                return CompanyDto(
                    chamberOfCommerceId = chamberOfCommerceId,
                    vihbId = vihbId,
                    name = mapNameType(it.partyNames),
                    address = toDto(it.postalAddress)
                )
            }
    }

    private fun getChamberOfCommerceId(source: PartyType): String? {
        return source.partyIdentifications
            .firstOrNull { it.id.schemeAgencyName == "KvK" }
            ?.id
            ?.value
    }

    private fun getVihbId(source: PartyType): String? {
        return source.partyIdentifications
            .firstOrNull { it.id.schemeAgencyName == "VIHB" }
            ?.id
            ?.value
    }

    private fun mapNameType(source: List<PartyName>): String {
        return source.joinToString { it.name.value }
    }

}