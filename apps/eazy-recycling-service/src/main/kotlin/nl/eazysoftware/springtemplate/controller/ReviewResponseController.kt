package nl.eazysoftware.springtemplate.controller

import nl.eazysoftware.springtemplate.repository.Truck
import nl.eazysoftware.springtemplate.repository.TruckRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trucks")
@CrossOrigin(origins = ["*"])
class ReviewResponseController(
    private val repository: TruckRepository,
) {

    private val logger: Logger = LoggerFactory.getLogger(ReviewResponseController::class.java)

    @PostMapping
    fun createTruck(@RequestBody review: Truck): Truck {
        if (repository.existsById(review.licensePlate)) {
            throw IllegalArgumentException("A review with the ID '${review.licensePlate}' already exists.")
        }

        return repository.save(review)
    }

    @GetMapping
    fun getTrucks(): List<Truck> {
        return repository.findAll()
    }
}
