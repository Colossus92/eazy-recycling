package nl.eazysoftware.eazyrecyclingservice.controller

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
    @ResponseStatus(HttpStatus.CREATED)
    fun createTruck(@RequestBody truck: Truck) {
        truckService.createTruck(truck)
    }

    @GetMapping
    fun getAllTrucks(): List<Truck> {
        return truckService.getAllTrucks()
    }

    @GetMapping("/{licensePlate}")
    fun getTruckByLicensePlate(@PathVariable licensePlate: String): Truck {
        return truckService.getTruckByLicensePlate(licensePlate)
    }
}