package nl.eazysoftware.springtemplate.controller

import nl.eazysoftware.springtemplate.repository.entity.goods.GoodsItemDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.CompanyDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.LocationDto
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