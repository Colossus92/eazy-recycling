package nl.eazysoftware.springtemplate.domain.mapper

import nl.eazysoftware.springtemplate.repository.WaybillRepository
import nl.eazysoftware.springtemplate.repository.entity.waybill.WaybillDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class WaybillService(
    val waybillRepository: WaybillRepository,
) {
    fun getUnassignedWaybillsByDate(pickupDate: LocalDate): List<WaybillDto> {
        val start = pickupDate.atStartOfDay()
        val end = pickupDate.atTime(23, 59, 59)
        return waybillRepository.findUnassignedWaybills(start, end)
    }

    fun findAll(): List<WaybillDto> {
        return waybillRepository.findAll()
    }
}