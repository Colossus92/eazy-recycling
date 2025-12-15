package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Products
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@RestController
@RequestMapping("/products")
@PreAuthorize(HAS_ANY_ROLE)
class ProductController(
    private val products: Products
) {

    @GetMapping
    fun getAllProducts(): List<ProductResponse> {
        return products.getAllProducts().map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun getProductById(@PathVariable id: Long): ProductResponse {
        val product = products.getProductById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product with id $id not found")
        return product.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(@Valid @RequestBody request: ProductRequest): ProductResponse {
        val product = request.toDomain()
        val created = products.createProduct(product)
        return created.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductRequest
    ): ProductResponse {
        products.getProductById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product with id $id not found")

        val product = request.toDomain()
        val updated = products.updateProduct(id, product)
        return updated.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProduct(@PathVariable id: Long) {
        products.getProductById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product with id $id not found")

        products.deleteProduct(id)
    }
}

data class ProductRequest(
    @field:NotBlank(message = "Code is verplicht")
    val code: String,

    @field:NotBlank(message = "Naam is verplicht")
    val name: String,

    val categoryId: Long?,

    @field:NotBlank(message = "Eenheid is verplicht")
    val unitOfMeasure: String,

    @field:NotBlank(message = "VAT code is verplicht")
    val vatCode: String,

    val glAccountCode: String?,

    @field:NotBlank(message = "Status is verplicht")
    val status: String,

    val defaultPrice: BigDecimal?,

    val description: String?,
) {
    fun toDomain(): Product {
        return Product(
            id = null,
            code = code,
            name = name,
            categoryId = categoryId,
            categoryName = null,
            unitOfMeasure = unitOfMeasure,
            vatCode = vatCode,
            glAccountCode = glAccountCode,
            status = status,
            defaultPrice = defaultPrice,
            description = description,
            createdAt = null,
            updatedAt = null
        )
    }
}

data class ProductResponse(
    val id: Long,
    val code: String,
    val name: String,
    val categoryId: Long?,
    val unitOfMeasure: String,
    val vatCode: String,
    val glAccountCode: String?,
    val status: String,
    val defaultPrice: BigDecimal?,
    val description: String?,
    val createdAt: String?,
    val createdByName: String?,
    val updatedAt: String?,
    val updatedByName: String?,
)

fun Product.toResponse(): ProductResponse {
    return ProductResponse(
        id = id!!,
        code = code,
        name = name,
        categoryId = categoryId,
        unitOfMeasure = unitOfMeasure,
        vatCode = vatCode,
        glAccountCode = glAccountCode,
        status = status,
        defaultPrice = defaultPrice,
        description = description,
        createdAt = createdAt?.toString(),
        createdByName = createdBy,
        updatedAt = updatedAt?.toString(),
        updatedByName = updatedBy,
    )
}
