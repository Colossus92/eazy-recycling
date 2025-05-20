package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.domain.service.PlanningService
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/planning")
class PlanningController(
    val planningService: PlanningService
) {

    @GetMapping("/{pickupDate}")
    fun getPlanningByDate(
        @PathVariable pickupDate: LocalDate,
        @RequestParam(required = false) truckId: String? = null,
        @RequestParam(required = false) driverId: UUID? = null,
        @RequestParam(required = false) status: String? = null
    ): PlanningView {
        return planningService.getPlanningByDate(pickupDate, truckId, driverId, status)
    }
}