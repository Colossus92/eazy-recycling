package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.product.ProductCategory
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProductCategories
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/product-categories")
@PreAuthorize(HAS_ANY_ROLE)
class ProductCategoryController(
    private val productCategories: ProductCategories
) {

    @GetMapping
    fun getAllCategories(): List<ProductCategoryResponse> {
        return productCategories.getAllCategories().map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun getCategoryById(@PathVariable id: Long): ProductCategoryResponse {
        val category = productCategories.getCategoryById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Productcategorie met id $id niet gevonden")
        return category.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(@Valid @RequestBody request: ProductCategoryRequest): ProductCategoryResponse {
        val category = request.toDomain()
        val created = productCategories.createCategory(category)
        return created.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductCategoryRequest
    ): ProductCategoryResponse {
        productCategories.getCategoryById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Productcategorie met id $id niet gevonden")

        val category = request.toDomain()
        val updated = productCategories.updateCategory(id, category)
        return updated.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategory(@PathVariable id: Long) {
        productCategories.getCategoryById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Productcategorie met id $id niet gevonden")

        productCategories.deleteCategory(id)
    }
}

data class ProductCategoryRequest(
    @field:NotBlank(message = "Code is verplicht")
    val code: String,

    @field:NotBlank(message = "Naam is verplicht")
    val name: String,

    val description: String?,
) {
    fun toDomain(): ProductCategory {
        return ProductCategory(
            id = null,
            code = code,
            name = name,
            description = description,
            createdAt = null,
            updatedAt = null
        )
    }
}

data class ProductCategoryResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val createdAt: String?,
    val createdByName: String?,
    val updatedAt: String?,
    val updatedByName: String?,
)

fun ProductCategory.toResponse(): ProductCategoryResponse {
    return ProductCategoryResponse(
        id = id!!,
        code = code,
        name = name,
        description = description,
        createdAt = createdAt?.toString(),
        createdByName = createdBy,
        updatedAt = updatedAt?.toString(),
        updatedByName = updatedBy,
    )
}
