package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.material.Material
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Materials
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/materials")
@PreAuthorize(HAS_ANY_ROLE)
class MaterialController(
    private val materials: Materials
) {

    @GetMapping
    fun getAllMaterials(): List<MaterialResponse> {
        return materials.getAllMaterials().map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun getMaterialById(@PathVariable id: Long): MaterialResponse {
        val material = materials.getMaterialById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material with id $id not found")
        return material.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createMaterial(@Valid @RequestBody request: MaterialRequest): MaterialResponse {
        val material = request.toDomain()
        val created = materials.createMaterial(material)
        return created.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PutMapping("/{id}")
    fun updateMaterial(
        @PathVariable id: Long,
        @Valid @RequestBody request: MaterialRequest
    ): MaterialResponse {
        // Check if material exists
        materials.getMaterialById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material with id $id not found")

        val material = request.toDomain()
        val updated = materials.updateMaterial(id, material)
        return updated.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMaterial(@PathVariable id: Long) {
        // Check if material exists
        materials.getMaterialById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material with id $id not found")

        materials.deleteMaterial(id)
    }
}

data class MaterialRequest(
    @field:NotBlank(message = "Code is verplicht")
    val code: String,

    @field:NotBlank(message = "Naam is verplicht")
    val name: String,

    @field:Positive(message = "Material group ID moet een positief getal zijn")
    val materialGroupId: Long,

    @field:NotBlank(message = "Eenheid is verplicht")
    val unitOfMeasure: String,

    @field:NotBlank(message = "VAT code is verplicht")
    val vatCode: String,

    @field:NotBlank(message = "Status is verplicht")
    val status: String
) {
    fun toDomain(): Material {
        return Material(
            id = null,
            code = code,
            name = name,
            materialGroupId = materialGroupId,
            unitOfMeasure = unitOfMeasure,
            vatCode = vatCode,
            status = status,
            createdAt = null,
            updatedAt = null
        )
    }
}

data class MaterialResponse(
    val id: Long,
    val code: String,
    val name: String,
    val materialGroupId: Long,
    val unitOfMeasure: String,
    val vatCode: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String?
)

fun Material.toResponse(): MaterialResponse {
    return MaterialResponse(
        id = id!!,
        code = code,
        name = name,
        materialGroupId = materialGroupId,
        unitOfMeasure = unitOfMeasure,
        vatCode = vatCode,
        status = status,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt?.toString()
    )
}
