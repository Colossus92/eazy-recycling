package nl.eazysoftware.springtemplate.controller

import nl.eazysoftware.springtemplate.domain.mapper.WaybillService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/waybill")
class WaybillController(
    val waybillService: WaybillService
) {

    @GetMapping("/unassigned/{pickupDate}")
    fun getUnassignedWaybillsByDate(@PathVariable("pickupDate") pickupDate: LocalDate): List<WaybillView> {
        return waybillService.getUnassignedWaybillsByDate(pickupDate)
    }

    @GetMapping
    fun findAll(): List<WaybillView> {
        return waybillService.findAll()
    }
}