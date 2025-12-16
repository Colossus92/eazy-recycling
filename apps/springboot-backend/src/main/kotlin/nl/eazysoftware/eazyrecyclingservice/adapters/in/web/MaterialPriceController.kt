package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Materials
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialQueryResult
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@RestController
@RequestMapping("/material-prices")
@PreAuthorize(HAS_ANY_ROLE)
class MaterialPriceController(
    private val materials: Materials
) {

    @GetMapping
    fun getAllMaterialsWithPrices(): List<MaterialPriceResponse> {
        return materials.getAllMaterialsWithGroupDetails().map { it.toPriceResponse() }
    }

    @GetMapping("/{id}")
    fun getMaterialPrice(@PathVariable id: Long): MaterialPriceResponse {
        val material = materials.getMaterialWithGroupDetailsById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material with id $id not found")
        return material.toPriceResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PostMapping("/{id}")
    @Transactional
    fun createMaterialPrice(
        @PathVariable id: Long,
        @Valid @RequestBody request: MaterialPriceRequest
    ): MaterialPriceResponse {
        val material = materials.getMaterialWithGroupDetailsById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material with id $id not found")

        if (material.getDefaultPrice() != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Material with id $id already has a price. Use PUT to update.")
        }

        val updated = materials.updateMaterialPrice(id, request.price)
        if (!updated) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create price for material")
        }

        return materials.getMaterialWithGroupDetailsById(id)?.toPriceResponse()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve updated material")
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PutMapping("/{id}")
    @Transactional
    fun updateMaterialPrice(
        @PathVariable id: Long,
        @Valid @RequestBody request: MaterialPriceRequest
    ): MaterialPriceResponse {
        materials.getMaterialWithGroupDetailsById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material with id $id not found")

        val updated = materials.updateMaterialPrice(id, request.price)
        if (!updated) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update price for material")
        }

        return materials.getMaterialWithGroupDetailsById(id)?.toPriceResponse()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve updated material")
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteMaterialPrice(@PathVariable id: Long) {
        materials.getMaterialWithGroupDetailsById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material with id $id not found")

        val updated = materials.updateMaterialPrice(id, null)
        if (!updated) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete price for material")
        }
    }
}

data class MaterialPriceRequest(
    @field:Positive(message = "Price must be a positive number")
    val price: BigDecimal
)

data class MaterialPriceResponse(
    val id: Long,
    val code: String,
    val name: String,
    val materialGroupId: Long?,
    val materialGroupName: String?,
    val unitOfMeasure: String,
    val defaultPrice: BigDecimal?,
)

fun MaterialQueryResult.toPriceResponse(): MaterialPriceResponse {
    return MaterialPriceResponse(
        id = getId(),
        code = getCode(),
        name = getName(),
        materialGroupId = getMaterialGroupId(),
        materialGroupName = getMaterialGroupName(),
        unitOfMeasure = getUnitOfMeasure(),
        defaultPrice = getDefaultPrice(),
    )
}
