package nl.eazysoftware.eazyrecyclingservice.controller.truck

import jakarta.annotation.security.RolesAllowed
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles.ADMIN
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles.CHAUFFEUR
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles.PLANNER
import nl.eazysoftware.eazyrecyclingservice.domain.service.TruckService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/trucks")
class TruckController(
    private val truckService: TruckService,
) {


    @PostMapping
    @RolesAllowed(ADMIN, PLANNER)
    @ResponseStatus(HttpStatus.CREATED)
    fun createTruck(@RequestBody truck: Truck) {
        truckService.createTruck(truck)
    }

    @GetMapping
    @RolesAllowed(ADMIN, PLANNER, CHAUFFEUR)
    fun getAllTrucks(): List<Truck> {
        return truckService.getAllTrucks()
    }

    @GetMapping("/{licensePlate}")
    @RolesAllowed(ADMIN, PLANNER, CHAUFFEUR)
    fun getTruckByLicensePlate(@PathVariable licensePlate: String): Truck {
        return truckService.getTruckByLicensePlate(licensePlate)
    }

    @DeleteMapping("/{licensePlate}")
    @RolesAllowed(ADMIN, PLANNER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTruck(@PathVariable licensePlate: String) {
        truckService.deleteTruck(licensePlate)
    }

    @PutMapping("/{licensePlate}")
    @RolesAllowed(ADMIN, PLANNER)
    @ResponseStatus(HttpStatus.OK)
    fun updateTruck(@PathVariable licensePlate: String, @RequestBody truck: Truck): Truck {
        return truckService.updateTruck(licensePlate, truck)
    }
}