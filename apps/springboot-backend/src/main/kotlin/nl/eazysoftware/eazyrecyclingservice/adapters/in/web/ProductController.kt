package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Products
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductQueryResult
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.util.*

@RestController
@RequestMapping("/products")
@PreAuthorize(HAS_ANY_ROLE)
class ProductController(
    private val products: Products
) {

    @GetMapping
    fun getAllProducts(): List<ProductResponse> {
        return products.getAllProductsWithDetails().map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun getProductById(@PathVariable id: UUID): ProductResponse {
        val product = products.getProductWithDetailsById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product met id $id niet gevonden")
        return product.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(@Valid @RequestBody request: ProductRequest): ProductResponse {
        val product = request.toDomain()
        val created = products.createProduct(product)
        return products.getProductWithDetailsById(created.id!!)
            ?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Het ophalen van het product is mislukt")
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ProductRequest
    ): ProductResponse {
        products.getProductById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product met id $id niet gevonden")

        val product = request.toDomain()
        products.updateProduct(id, product)
        return products.getProductWithDetailsById(id)
            ?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Het ophalen van het aangepaste product is mislukt")
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProduct(@PathVariable id: UUID) {
        products.getProductById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product met id $id niet gevonden")

        products.deleteProduct(id)
    }
}

data class ProductRequest(
    @field:NotBlank(message = "Code is verplicht")
    val code: String,

    @field:NotBlank(message = "Naam is verplicht")
    val name: String,

    val categoryId: UUID?,

    @field:NotBlank(message = "Eenheid is verplicht")
    val unitOfMeasure: String,

    val vatRateId: UUID,

    val salesAccountNumber: String?,

    val purchaseAccountNumber: String?,

    @field:NotBlank(message = "Status is verplicht")
    val status: String,

    val defaultPrice: BigDecimal?,
) {
    fun toDomain(): Product {
        return Product(
            id = null,
            code = code,
            name = name,
            categoryId = categoryId,
            categoryName = null,
            unitOfMeasure = unitOfMeasure,
            vatRateId = vatRateId,
            salesAccountNumber = salesAccountNumber,
            purchaseAccountNumber = purchaseAccountNumber,
            status = status,
            defaultPrice = defaultPrice,
            createdAt = null,
            updatedAt = null
        )
    }
}

data class ProductResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val categoryId: UUID?,
    val unitOfMeasure: String,
    val vatCode: String,
    val vatRateId: UUID,
    val salesAccountNumber: String?,
    val purchaseAccountNumber: String?,
    val status: String,
    val defaultPrice: BigDecimal?,
    val createdAt: String?,
    val createdByName: String?,
    val updatedAt: String?,
    val updatedByName: String?,
)

fun ProductQueryResult.toResponse(): ProductResponse {
    return ProductResponse(
        id = getId(),
        code = getCode(),
        name = getName(),
        categoryId = getCategoryId(),
        unitOfMeasure = getUnitOfMeasure(),
        vatCode = getVatCode(),
        vatRateId = getVatRateId(),
        salesAccountNumber = getSalesAccountNumber(),
        purchaseAccountNumber = getPurchaseAccountNumber(),
        status = getStatus(),
        defaultPrice = getDefaultPrice(),
        createdAt = getCreatedAt()?.toString(),
        createdByName = getCreatedBy(),
        updatedAt = getUpdatedAt()?.toString(),
        updatedByName = getUpdatedBy(),
    )
}
