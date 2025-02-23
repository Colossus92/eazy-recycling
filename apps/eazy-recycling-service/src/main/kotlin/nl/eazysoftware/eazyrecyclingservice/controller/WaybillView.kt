package nl.eazysoftware.eazyrecyclingservice.controller

import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import java.time.LocalDateTime
import java.util.*

data class WaybillView(
    val id: String,
    val uuid: UUID,
    val note: String,
    val consigneeParty: CompanyDto,
    val consignorParty: CompanyDto,
    val carrierParty: CompanyDto,
    val pickupParty: CompanyDto,
    val goodsItem: GoodsItemDto,
    val deliveryLocation: LocationDto,
    val deliveryDateTime: LocalDateTime,
    val pickupLocation: LocationDto,
    val pickupDateTime: LocalDateTime,
    val licensePlate: String,
)