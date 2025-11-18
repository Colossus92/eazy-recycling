package nl.eazysoftware.eazyrecyclingservice.controller.truck

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllTrucks
import nl.eazysoftware.eazyrecyclingservice.application.query.GetTruckByLicensePlate
import nl.eazysoftware.eazyrecyclingservice.application.query.TruckView
import nl.eazysoftware.eazyrecyclingservice.application.usecase.truck.*
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/trucks")
class TruckController(
    private val createTruckUseCase: CreateTruck,
    private val updateTruckUseCase: UpdateTruck,
    private val deleteTruckUseCase: DeleteTruck,
    private val getAllTrucksQuery: GetAllTrucks,
    private val getTruckByLicensePlateQuery: GetTruckByLicensePlate
) {

    @PostMapping
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @ResponseStatus(HttpStatus.CREATED)
    fun createTruck(@Valid @RequestBody request: TruckRequest): TruckResult {
        val command = TruckCommand(
            licensePlate = LicensePlate(request.licensePlate),
            brand = request.brand,
            description = request.description,
            carrierPartyId = request.carrierPartyId?.let { CompanyId(UUID.fromString(it)) }
        )
        return createTruckUseCase.handle(command)
    }

    @GetMapping
    @PreAuthorize(HAS_ANY_ROLE)
    fun getAllTrucks(): List<TruckView> {
        return getAllTrucksQuery.handle()
    }

    @GetMapping("/{licensePlate}")
    @PreAuthorize(HAS_ANY_ROLE)
    fun getTruckByLicensePlate(@PathVariable licensePlate: String): TruckView {
        return getTruckByLicensePlateQuery.handle(LicensePlate(licensePlate))
    }

    @DeleteMapping("/{licensePlate}")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTruck(@PathVariable licensePlate: String) {
        deleteTruckUseCase.handle(LicensePlate(licensePlate))
    }

    @PutMapping("/{licensePlate}")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @ResponseStatus(HttpStatus.OK)
    fun updateTruck(@PathVariable licensePlate: String, @Valid @RequestBody request: TruckRequest): TruckResult {
        if (!licensePlate.equals(request.licensePlate, ignoreCase = true)) {
            throw IllegalArgumentException("Vrachtwagen komt niet overeen met kenteken $licensePlate")
        }

        val command = TruckCommand(
            licensePlate = LicensePlate(request.licensePlate),
            brand = request.brand,
            description = request.description,
            carrierPartyId = request.carrierPartyId?.let { CompanyId(UUID.fromString(it)) }
        )
        return updateTruckUseCase.handle(command)
    }

    data class TruckRequest(
        @field:NotBlank(message = "Kenteken is verplicht")
        val licensePlate: String,

        val brand: String?,

        val description: String?,

        val carrierPartyId: String? = null,
    )
}
