package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import nl.eazysoftware.eazyrecyclingservice.application.query.GetMaterialPrices
import nl.eazysoftware.eazyrecyclingservice.application.usecase.material.*
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@RestController
@RequestMapping("/material-prices")
@PreAuthorize(HAS_ANY_ROLE)
class MaterialPriceController(
    private val getMaterialPrices: GetMaterialPrices,
    private val createMaterialPrice: CreateMaterialPrice,
    private val updateMaterialPrice: UpdateMaterialPrice,
    private val deleteMaterialPrice: DeleteMaterialPrice
) {

    @GetMapping
    fun getAllActivePrices(): List<MaterialPriceResponse> {
        return getMaterialPrices.getAllActive().map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun getPriceById(@PathVariable id: Long): MaterialPriceResponse {
        val price = getMaterialPrices.getById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material price with id $id not found")
        return price.toResponse()
    }

    @GetMapping("/material/{materialId}")
    fun getActivePricesByMaterialId(@PathVariable materialId: Long): List<MaterialPriceResponse> {
        return getMaterialPrices.getActiveByMaterialId(materialId).map { it.toResponse() }
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPrice(@Valid @RequestBody request: MaterialPriceRequest): MaterialPriceResponse {
        val command = request.toCommand()
        val result = createMaterialPrice.handle(command)
        return result.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PutMapping("/{id}")
    fun updatePrice(
        @PathVariable id: Long,
        @Valid @RequestBody request: MaterialPriceRequest
    ): MaterialPriceResponse {
        // Check if price exists
        getMaterialPrices.getById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material price with id $id not found")

        val command = request.toCommand()
        val result = updateMaterialPrice.handle(id, command)
        return result.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePrice(@PathVariable id: Long) {
        // Check if price exists
        getMaterialPrices.getById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material price with id $id not found")

        deleteMaterialPrice.handle(id)
    }
}

data class MaterialPriceRequest(
    @field:Positive(message = "Material ID moet een positief getal zijn")
    val materialId: Long,

    @field:Positive(message = "Prijs moet een positief getal zijn")
    val price: BigDecimal,

    @field:NotBlank(message = "Valuta is verplicht")
    val currency: String
) {
    fun toCommand(): MaterialPriceCommand {
        return MaterialPriceCommand(
            materialId = materialId,
            price = price,
            currency = currency
        )
    }
}

data class MaterialPriceResponse(
    val id: Long,
    val materialId: Long,
    val price: BigDecimal,
    val currency: String,
    val validFrom: String,
    val validTo: String?
)

fun MaterialPriceResult.toResponse(): MaterialPriceResponse {
    return MaterialPriceResponse(
        id = id,
        materialId = materialId,
        price = price,
        currency = currency,
        validFrom = validFrom,
        validTo = validTo
    )
}
