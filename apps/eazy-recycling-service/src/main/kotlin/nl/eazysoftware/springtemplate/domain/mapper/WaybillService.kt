package nl.eazysoftware.springtemplate.domain.mapper

import nl.eazysoftware.springtemplate.controller.WaybillView
import nl.eazysoftware.springtemplate.repository.TransportRepository
import nl.eazysoftware.springtemplate.repository.entity.transport.TransportDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class WaybillService(
    val transportRepository: TransportRepository,
) {
    fun getUnassignedWaybillsByDate(pickupDate: LocalDate): List<WaybillView> {
        val start = pickupDate.atStartOfDay()
        val end = pickupDate.atTime(23, 59, 59)
        return transportRepository.findUnassignedWaybills(start, end)
            .map { map(it) }
    }

    fun findAll(): List<WaybillView> {
        return transportRepository.findAllByGoodsNotNull()
            .map { map(it) }
    }

    fun map(transportDto: TransportDto): WaybillView {
        val goods = transportDto.goods ?: throw IllegalArgumentException("Goods not found")
        return WaybillView(
            id = transportDto.id.toString(),
            uuid = goods.uuid,
            note = goods.note,
            consigneeParty = goods.consigneeParty,
            consignorParty = transportDto.consignorParty,
            carrierParty = transportDto.carrierParty,
            pickupParty = goods.pickupParty,
            goodsItem = goods.goodsItem,
            deliveryLocation = transportDto.deliveryLocation,
            deliveryDateTime = transportDto.deliveryDateTime,
            pickupLocation = transportDto.pickupLocation,
            pickupDateTime = transportDto.pickupDateTime,
            licensePlate = transportDto.truck?.licensePlate ?: ""
        )
    }
}