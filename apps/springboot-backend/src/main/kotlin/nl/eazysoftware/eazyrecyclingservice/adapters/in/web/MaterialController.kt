package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.material.Material
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Materials
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialQueryResult
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@RestController
@RequestMapping("/materials")
@PreAuthorize(HAS_ANY_ROLE)
class MaterialController(
    private val materials: Materials
) {

    @GetMapping
    fun getAllMaterials(): List<MaterialResponse> {
        return materials.getAllMaterialsWithGroupDetails().map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun getMaterialById(@PathVariable id: Long): MaterialResponse {
        val material = materials.getMaterialWithGroupDetailsById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Materiaal met id $id niet gevonden")
        return material.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createMaterial(@Valid @RequestBody request: MaterialRequest): MaterialResponse {
        val material = request.toDomain()
        val created = materials.createMaterial(material)
        // Fetch with group details for response
        return materials.getMaterialWithGroupDetailsById(created.id!!)
            ?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Het ophalen van het materiaal is mislukt")
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PutMapping("/{id}")
    fun updateMaterial(
        @PathVariable id: Long,
        @Valid @RequestBody request: MaterialRequest
    ): MaterialResponse {
        // Check if material exists
        materials.getMaterialById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Materiaal met id $id niet gevonden")

        val material = request.toDomain()
        materials.updateMaterial(id, material)
        // Fetch with group details for response
        return materials.getMaterialWithGroupDetailsById(id)
            ?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Het ophalen van het aangepaste materiaal is mislukt")
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMaterial(@PathVariable id: Long) {
        // Check if material exists
        materials.getMaterialById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Materiaal met id $id niet gevonden")

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

  val salesAccountNumber: String?,

  val purchaseAccountNumber: String?,

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
            salesAccountNumber = salesAccountNumber,
            purchaseAccountNumber = purchaseAccountNumber,
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
  val materialGroupId: Long?,
  val materialGroupCode: String?,
  val materialGroupName: String?,
  val unitOfMeasure: String,
  val vatCode: String,
  val salesAccountNumber: String?,
  val purchaseAccountNumber: String?,
  val defaultPrice: BigDecimal?,
  val status: String,
  val createdAt: String?,
  val createdByName: String?,
  val updatedAt: String?,
  val updatedByName: String?,
)

fun MaterialQueryResult.toResponse(): MaterialResponse {
    return MaterialResponse(
        id = getId(),
        code = getCode(),
        name = getName(),
        materialGroupId = getMaterialGroupId(),
        materialGroupCode = getMaterialGroupCode(),
        materialGroupName = getMaterialGroupName(),
        unitOfMeasure = getUnitOfMeasure(),
        vatCode = getVatCode(),
        salesAccountNumber = getSalesAccountNumber(),
        purchaseAccountNumber = getPurchaseAccountNumber(),
        defaultPrice = getDefaultPrice(),
        status = getStatus(),
        createdAt = getCreatedAt()?.toString(),
        createdByName = getCreatedBy(),
        updatedAt = getUpdatedAt()?.toString(),
        updatedByName = getUpdatedBy(),
    )
}
