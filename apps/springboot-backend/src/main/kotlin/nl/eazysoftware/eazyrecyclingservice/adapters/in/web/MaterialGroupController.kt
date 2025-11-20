package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialGroup
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialGroups
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/material-groups")
@PreAuthorize(HAS_ANY_ROLE)
class MaterialGroupController(
    private val materialGroups: MaterialGroups
) {

    @GetMapping
    fun getAllMaterialGroups(): List<MaterialGroupResponse> {
        return materialGroups.getAllMaterialGroups().map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun getMaterialGroupById(@PathVariable id: Long): MaterialGroupResponse {
        val materialGroup = materialGroups.getMaterialGroupById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material group with id $id not found")
        return materialGroup.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createMaterialGroup(@Valid @RequestBody request: MaterialGroupRequest): MaterialGroupResponse {
        val materialGroup = request.toDomain()
        val created = materialGroups.createMaterialGroup(materialGroup)
        return created.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PutMapping("/{id}")
    fun updateMaterialGroup(
        @PathVariable id: Long,
        @Valid @RequestBody request: MaterialGroupRequest
    ): MaterialGroupResponse {
        // Check if material group exists
        materialGroups.getMaterialGroupById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material group with id $id not found")

        val materialGroup = request.toDomain()
        val updated = materialGroups.updateMaterialGroup(id, materialGroup)
        return updated.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMaterialGroup(@PathVariable id: Long) {
        // Check if material group exists
        materialGroups.getMaterialGroupById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Material group with id $id not found")

        materialGroups.deleteMaterialGroup(id)
    }
}

data class MaterialGroupRequest(
    @field:NotBlank(message = "Code is verplicht")
    val code: String,

    @field:NotBlank(message = "Naam is verplicht")
    val name: String,

    val description: String?,
) {
    fun toDomain(): MaterialGroup {
        return MaterialGroup(
            id = null,
            code = code,
            name = name,
            description = description,
            createdAt = null,
            updatedAt = null
        )
    }
}

data class MaterialGroupResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String?
)

fun MaterialGroup.toResponse(): MaterialGroupResponse {
    return MaterialGroupResponse(
        id = id!!,
        code = code,
        name = name,
        description = description,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt?.toString()
    )
}
