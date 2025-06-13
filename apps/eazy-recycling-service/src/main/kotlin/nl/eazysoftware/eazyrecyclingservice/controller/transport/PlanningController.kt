package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.PlanningService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/planning")
class PlanningController(
    val planningService: PlanningService
) {

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @GetMapping("/{pickupDate}")
    fun getPlanningByDate(
        @PathVariable pickupDate: LocalDate,
        @RequestParam(required = false) truckId: String? = null,
        @RequestParam(required = false) driverId: UUID? = null,
        @RequestParam(required = false) status: String? = null
    ): PlanningView {
        return planningService.getPlanningByDate(pickupDate, truckId, driverId, status)
    }

    @PutMapping("/reorder")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    fun reorderTransports(@RequestBody reorderRequest: TransportReorderRequest): PlanningView {
        return planningService.reorderTransports(
            reorderRequest.date,
            reorderRequest.licensePlate,
            reorderRequest.transportIds
        )
    }

    // TODO: authorize per user
    @PreAuthorize(HAS_ANY_ROLE)
    @GetMapping("/driver/{driverId}")
    fun getPlanningByDriver(@PathVariable driverId: UUID, @RequestParam(required = true) startDate: LocalDate, @RequestParam(required = true) endDate: LocalDate): DriverPlanning {
        return planningService.getPlanningByDriver(driverId, startDate, endDate)
    }
}

data class TransportReorderRequest(
    val date: LocalDate,
    val licensePlate: String,
    val transportIds: List<UUID>
)