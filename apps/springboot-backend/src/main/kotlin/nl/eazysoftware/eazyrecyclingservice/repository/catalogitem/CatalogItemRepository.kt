package nl.eazysoftware.eazyrecyclingservice.repository.catalogitem

import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.CatalogItems
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

interface CatalogItemJpaRepository : JpaRepository<CatalogItemDto, UUID> {

    @Query(
        value = """
            SELECT
                ci.id as id,
                ci.type as type,
                ci.code as code,
                ci.name as name,
                ci.unit_of_measure as unitOfMeasure,
                ci.vat_code as vatCode,
                cic.name as categoryName,
                ci.consignor_party_id as consignorPartyId,
                ci.default_price as defaultPrice,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.sales_account_number as salesAccountNumber,
                NULL as wasteStreamNumber
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            WHERE ci.status = 'ACTIVE'
              AND (ci.consignor_party_id IS NULL OR ci.consignor_party_id = :consignorPartyId)
              AND (:query IS NULL OR LOWER(ci.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(ci.code) LIKE LOWER(CONCAT('%', :query, '%')))

            UNION ALL

            SELECT
                ci.id as id,
                'WASTE_STREAM' as type,
                ci.code as code,
                ws.name as name,
                ci.unit_of_measure as unitOfMeasure,
                ci.vat_code as vatCode,
                cic.name as categoryName,
                ws.consignor_party_id as consignorPartyId,
                ci.default_price as defaultPrice,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.sales_account_number as salesAccountNumber,
                ws.number as wasteStreamNumber
            FROM waste_streams ws
            INNER JOIN catalog_items ci ON ws.catalog_item_id = ci.id
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            WHERE ws.status = 'ACTIVE'
              AND ws.consignor_party_id = :consignorPartyId
              AND (:query IS NULL OR LOWER(ws.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(ws.number) LIKE LOWER(CONCAT('%', :query, '%')))

            ORDER BY name
        """,
        nativeQuery = true
    )
    fun findAllForConsignor(
        @Param("consignorPartyId") consignorPartyId: UUID,
        @Param("query") query: String?
    ): List<CatalogItemQueryProjection>

    @Query(
        value = """
            SELECT
                ci.id as id,
                ci.type as type,
                ci.code as code,
                ci.name as name,
                ci.unit_of_measure as unitOfMeasure,
                ci.vat_code as vatCode,
                cic.name as categoryName,
                ci.consignor_party_id as consignorPartyId,
                ci.default_price as defaultPrice,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.sales_account_number as salesAccountNumber,
                NULL as wasteStreamNumber
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            WHERE ci.status = 'ACTIVE'
              AND ci.consignor_party_id IS NULL
              AND (:query IS NULL OR LOWER(ci.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(ci.code) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY ci.name
        """,
        nativeQuery = true
    )
    fun findAllGeneric(@Param("query") query: String?): List<CatalogItemQueryProjection>
}

interface CatalogItemQueryProjection {
    fun getId(): UUID
    fun getType(): String
    fun getCode(): String
    fun getName(): String
    fun getUnitOfMeasure(): String
    fun getVatCode(): String
    fun getCategoryName(): String?
    fun getConsignorPartyId(): UUID?
    fun getDefaultPrice(): java.math.BigDecimal?
    fun getPurchaseAccountNumber(): String?
    fun getSalesAccountNumber(): String?
    fun getWasteStreamNumber(): String?
}

@Repository
class CatalogItemRepository(
    private val jpaRepository: CatalogItemJpaRepository
) : CatalogItems {

    override fun findAll(consignorPartyId: UUID?, query: String?): List<CatalogItem> {
        val results = if (consignorPartyId != null) {
            jpaRepository.findAllForConsignor(consignorPartyId, query)
        } else {
            jpaRepository.findAllGeneric(query)
        }

        return results.map { projection ->
            CatalogItem(
                id = projection.getId(),
                type = CatalogItemType.valueOf(projection.getType()),
                code = projection.getCode(),
                name = projection.getName(),
                unitOfMeasure = projection.getUnitOfMeasure(),
                vatCode = projection.getVatCode(),
                categoryName = projection.getCategoryName(),
                consignorPartyId = projection.getConsignorPartyId()?.let { CompanyId(it) },
                defaultPrice = projection.getDefaultPrice(),
                purchaseAccountNumber = projection.getPurchaseAccountNumber(),
                salesAccountNumber = projection.getSalesAccountNumber(),
                wasteStreamNumber = projection.getWasteStreamNumber()?.let { WasteStreamNumber(it) },
                itemType = CatalogItemType.valueOf(projection.getType())
            )
        }
    }
}
