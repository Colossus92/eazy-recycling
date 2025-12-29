package nl.eazysoftware.eazyrecyclingservice.repository.product

import nl.eazysoftware.eazyrecyclingservice.domain.model.product.Product
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Products
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

interface ProductJpaRepository : JpaRepository<CatalogItemDto, UUID> {

  @Query(
    value = """
            SELECT
                ci.id as id,
                ci.code as code,
                ci.name as name,
                cic.id as categoryId,
                cic.code as categoryCode,
                cic.name as categoryName,
                ci.unit_of_measure as unitOfMeasure,
                ci.vat_code as vatCode,
                ci.sales_account_number as salesAccountNumber,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.default_price as defaultPrice,
                ci.status as status,
                ci.created_at as createdAt,
                ci.created_by as createdBy,
                ci.last_modified_at as updatedAt,
                ci.last_modified_by as updatedBy
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            WHERE ci.type = 'PRODUCT'
        """,
    nativeQuery = true
  )
  fun findAllProducts(): List<ProductQueryResult>

  @Query(
    value = """
            SELECT
                ci.id as id,
                ci.code as code,
                ci.name as name,
                cic.id as categoryId,
                cic.code as categoryCode,
                cic.name as categoryName,
                ci.unit_of_measure as unitOfMeasure,
                ci.vat_code as vatCode,
                ci.sales_account_number as salesAccountNumber,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.default_price as defaultPrice,
                ci.status as status,
                ci.created_at as createdAt,
                ci.created_by as createdBy,
                ci.last_modified_at as updatedAt,
                ci.last_modified_by as updatedBy
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            WHERE ci.id = :id AND ci.type = 'PRODUCT'
        """,
    nativeQuery = true
  )
  fun findProductById(id: UUID): ProductQueryResult?

  @Query(
    value = """
            SELECT
                ci.id as id,
                ci.code as code,
                ci.name as name,
                cic.id as categoryId,
                cic.code as categoryCode,
                cic.name as categoryName,
                ci.unit_of_measure as unitOfMeasure,
                ci.vat_code as vatCode,
                ci.sales_account_number as salesAccountNumber,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.default_price as defaultPrice,
                ci.status as status,
                ci.created_at as createdAt,
                ci.created_by as createdBy,
                ci.last_modified_at as updatedAt,
                ci.last_modified_by as updatedBy
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            WHERE ci.type = 'PRODUCT' AND ci.status = :status
        """,
    nativeQuery = true
  )
  fun findProductsByStatus(status: String): List<ProductQueryResult>

  @Query(
    value = """
            SELECT
                ci.id as id,
                ci.code as code,
                ci.name as name,
                cic.id as categoryId,
                cic.code as categoryCode,
                cic.name as categoryName,
                ci.unit_of_measure as unitOfMeasure,
                ci.vat_code as vatCode,
                ci.sales_account_number as salesAccountNumber,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.default_price as defaultPrice,
                ci.status as status,
                ci.created_at as createdAt,
                ci.created_by as createdBy,
                ci.last_modified_at as updatedAt,
                ci.last_modified_by as updatedBy
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            WHERE ci.type = 'PRODUCT'
              AND (LOWER(ci.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(ci.code) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY ci.name
        """,
    nativeQuery = true
  )
  fun searchProducts(query: String): List<ProductQueryResult>

  @Modifying(clearAutomatically = true)
  @Query(
    value = """
            UPDATE catalog_items
            SET code = :code,
                name = :name,
                category_id = :categoryId,
                unit_of_measure = :unitOfMeasure,
                vat_code = :vatCode,
                purchase_account_number = :purchaseAccountNumber,
                sales_account_number = :salesAccountNumber,
                default_price = :defaultPrice,
                status = :status,
                last_modified_at = NOW()
            WHERE id = :id AND type = 'PRODUCT'
        """,
    nativeQuery = true
  )
  fun updateProductFields(
    id: UUID,
    code: String,
    name: String,
    categoryId: UUID?,
    unitOfMeasure: String,
    vatCode: String,
    purchaseAccountNumber: String?,
    salesAccountNumber: String?,
    defaultPrice: BigDecimal?,
    status: String
  ): Int
}

@Repository
class ProductRepository(
  private val jpaRepository: ProductJpaRepository,
  private val mapper: ProductMapper
) : Products {

  override fun getAllProducts(): List<Product> {
    return jpaRepository.findAllProducts().map { it.toDomain() }
  }

  override fun getProductById(id: UUID): Product? {
    return jpaRepository.findProductById(id)?.toDomain()
  }

  override fun getActiveProducts(): List<Product> {
    return jpaRepository.findProductsByStatus("ACTIVE").map { it.toDomain() }
  }

  override fun searchProducts(query: String, limit: Int): List<Product> {
    return jpaRepository.searchProducts(query).take(limit).map { it.toDomain() }
  }

  override fun createProduct(product: Product): Product {
    val dto = mapper.toDto(product.copy(id = null))
    val saved = jpaRepository.save(dto)
    return mapper.toDomain(saved)
  }

  @Transactional
  override fun updateProduct(id: UUID, product: Product): Product {
    val rowsUpdated = jpaRepository.updateProductFields(
      id = id,
      code = product.code,
      name = product.name,
      categoryId = product.categoryId,
      unitOfMeasure = product.unitOfMeasure,
      vatCode = product.vatCode,
      purchaseAccountNumber = product.purchaseAccountNumber,
      salesAccountNumber = product.salesAccountNumber,
      defaultPrice = product.defaultPrice,
      status = product.status
    )
    if (rowsUpdated == 0) {
      throw IllegalArgumentException("Product met id $id is niet gevonden of is niet van type product")
    }
    return jpaRepository.findByIdOrNull(id)?.let { mapper.toDomain(it) }
      ?: throw IllegalStateException("Het ophalen van product met id $id is mislukt")
  }

  override fun deleteProduct(id: UUID) {
    jpaRepository.deleteById(id)
  }
}

private fun ProductQueryResult.toDomain(): Product {
  return Product(
    id = getId(),
    code = getCode(),
    name = getName(),
    categoryId = getCategoryId(),
    categoryName = getCategoryName(),
    unitOfMeasure = getUnitOfMeasure(),
    vatCode = getVatCode(),
    salesAccountNumber = getSalesAccountNumber(),
    purchaseAccountNumber = getPurchaseAccountNumber(),
    status = getStatus(),
    defaultPrice = getDefaultPrice(),
    createdAt = getCreatedAt()?.let { kotlin.time.Instant.fromEpochMilliseconds(it.toEpochMilli()) },
    createdBy = getCreatedBy(),
    updatedAt = getUpdatedAt()?.let { kotlin.time.Instant.fromEpochMilliseconds(it.toEpochMilli()) },
    updatedBy = getUpdatedBy(),
  )
}
