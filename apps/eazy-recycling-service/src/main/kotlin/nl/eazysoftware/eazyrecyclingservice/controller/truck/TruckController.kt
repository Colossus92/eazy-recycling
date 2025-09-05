package nl.eazysoftware.eazyrecyclingservice.controller.truck

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.TruckService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/trucks")
class TruckController(
    private val truckService: TruckService,
) {

    @PostMapping
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @ResponseStatus(HttpStatus.CREATED)
    fun createTruck(@RequestBody truck: Truck) {
        truckService.createTruck(truck)
    }

    @GetMapping
    @PreAuthorize(HAS_ANY_ROLE)
    fun getAllTrucks(): List<Truck> {
        return truckService.getAllTrucks()
    }

    @GetMapping("/{licensePlate}")
    @PreAuthorize(HAS_ANY_ROLE)
    fun getTruckByLicensePlate(@PathVariable licensePlate: String): Truck {
        return truckService.getTruckByLicensePlate(licensePlate)
    }

    @DeleteMapping("/{licensePlate}")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTruck(@PathVariable licensePlate: String) {
        truckService.deleteTruck(licensePlate)
    }

    @PutMapping("/{licensePlate}")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @ResponseStatus(HttpStatus.OK)
    fun updateTruck(@PathVariable licensePlate: String, @RequestBody truck: Truck): Truck {
        return truckService.updateTruck(licensePlate, truck)
    }
}