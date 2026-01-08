package nl.eazysoftware.eazyrecyclingservice.adapters.out.pricingapp

import com.fasterxml.jackson.annotation.JsonProperty
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.CreateProductRequest
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ExternalProduct
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.PricingAppSync
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.UpdateProductRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal

/**
 * Exception thrown when pricing app API calls fail.
 */
class PricingAppException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Adapter for communicating with the external pricing app API.
 */
@Component
@EnableConfigurationProperties(PricingAppProperties::class)
class PricingAppAdapter(
    private val restTemplate: RestTemplate,
    private val properties: PricingAppProperties
) : PricingAppSync {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun fetchExternalProducts(): List<ExternalProduct> {
        logger.info("Fetching products from pricing app")

        try {
            val response = restTemplate.exchange(
                "${properties.baseUrl}/products",
                HttpMethod.GET,
                HttpEntity<Any>(createHeaders()),
                PricingAppProductsResponse::class.java
            )

            if (response.body?.success != true) {
                throw PricingAppException("Ophalen van producten van app is mislukt: ${response.body?.message}")
            }

            return response.body?.data?.products?.map { it.toExternalProduct() } ?: emptyList()
        } catch (e: Exception) {
            logger.error("Error fetching products from pricing app", e)
            throw PricingAppException("Ophalen van producten van app is misluk", e)
        }
    }

    override fun createProduct(request: CreateProductRequest): Int {
        logger.info("Creating product in pricing app: ${request.name}")

        try {
            val apiRequest = PricingAppCreateRequest(
                name = request.name,
                price = request.price,
                priceStatus = request.priceStatus
            )

            val response = restTemplate.exchange(
                "${properties.baseUrl}/products/create",
                HttpMethod.POST,
                HttpEntity(apiRequest, createHeaders()),
                PricingAppProductResponse::class.java
            )

            if (response.body?.success != true) {
                throw PricingAppException("Failed to create product: ${response.body?.message}")
            }

            val productId = response.body?.data?.product?.id
                ?: throw PricingAppException("No product ID returned from create")

            logger.info("Created product in pricing app with ID: $productId")
            return productId
        } catch (e: PricingAppException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error creating product in pricing app", e)
            throw PricingAppException("Failed to create product in pricing app", e)
        }
    }

    override fun updateProduct(externalId: Int, request: UpdateProductRequest) {
        logger.info("Updating product in pricing app: $externalId")

        try {
            val apiRequest = PricingAppUpdateRequest(
                name = request.name,
                price = request.price,
                priceStatus = request.priceStatus
            )

            val response = restTemplate.exchange(
                "${properties.baseUrl}/products/$externalId/update",
                HttpMethod.POST,
                HttpEntity(apiRequest, createHeaders()),
                PricingAppBaseResponse::class.java
            )

            if (response.body?.success != true) {
                throw PricingAppException("Failed to update product: ${response.body?.message}")
            }

            logger.info("Updated product in pricing app: $externalId")
        } catch (e: PricingAppException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error updating product in pricing app", e)
            throw PricingAppException("Failed to update product in pricing app", e)
        }
    }

    override fun deleteProduct(externalId: Int) {
        logger.info("Deleting product from pricing app: $externalId")

        try {
            val response = restTemplate.exchange(
                "${properties.baseUrl}/products/$externalId/delete",
                HttpMethod.DELETE,
                HttpEntity<Any>(createHeaders()),
                PricingAppBaseResponse::class.java
            )

            if (response.body?.success != true) {
                throw PricingAppException("Failed to delete product: ${response.body?.message}")
            }

            logger.info("Deleted product from pricing app: $externalId")
        } catch (e: PricingAppException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error deleting product from pricing app", e)
            throw PricingAppException("Failed to delete product from pricing app", e)
        }
    }

    private fun createHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(properties.bearerToken)
        }
    }
}

// API Request DTOs

data class PricingAppCreateRequest(
    val name: String,
    val price: BigDecimal,
    @JsonProperty("price_status")
    val priceStatus: Int
)

data class PricingAppUpdateRequest(
    val name: String,
    val price: BigDecimal,
    @JsonProperty("price_status")
    val priceStatus: Int
)

// API Response DTOs

/**
 * Base response for simple operations (delete).
 * Delete returns: { "success": true, "message": "...", "data": [], "errors": [] }
 */
data class PricingAppBaseResponse(
    val success: Boolean,
    val message: String?,
    val errors: List<String>?
)

/**
 * Response for listing products.
 */
data class PricingAppProductsResponse(
    val success: Boolean,
    val message: String?,
    val data: PricingAppProductsData?,
    val errors: List<String>?
)

data class PricingAppProductsData(
    @JsonProperty("latest_update")
    val latestUpdate: String?,
    val products: List<PricingAppProduct>
)

/**
 * Product as returned in the list endpoint.
 */
data class PricingAppProduct(
    val id: Int,
    val name: String,
    val price: String, // API returns price as string with comma decimal separator in list
    @JsonProperty("price_status")
    val priceStatus: String,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("updated_at")
    val updatedAt: String
) {
    fun toExternalProduct(): ExternalProduct {
        // Parse price from Dutch format (comma as decimal separator)
        val parsedPrice = price.replace(",", ".").toBigDecimalOrNull() ?: BigDecimal.ZERO

        return ExternalProduct(
            id = id,
            name = name,
            price = parsedPrice,
            priceStatus = priceStatus.toIntOrNull() ?: 0,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

/**
 * Response for create/update operations.
 * Returns: { "success": true, "message": "...", "data": { "product": {...} }, "errors": [] }
 */
data class PricingAppProductResponse(
    val success: Boolean,
    val message: String?,
    val data: PricingAppProductData?,
    val errors: List<String>?
)

data class PricingAppProductData(
    val product: PricingAppProductDetail
)

/**
 * Product detail as returned in create/update responses.
 * Note: price is returned as a number (not string) in create/update responses.
 */
data class PricingAppProductDetail(
    val id: Int,
    val name: String,
    val price: BigDecimal,
    @JsonProperty("price_status")
    val priceStatus: Int,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("updated_at")
    val updatedAt: String
)
