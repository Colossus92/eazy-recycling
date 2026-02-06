package nl.eazysoftware.eazyrecyclingservice.repository.catalogitem

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItem
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.CatalogItems
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

interface CatalogItemJpaRepository : JpaRepository<CatalogItemDto, UUID> {

    fun findAllByTypeAndStatus(type: CatalogItemType, status: String): List<CatalogItemDto>

    @Query(
        value = """
            SELECT
                ci.id as id,
                ci.type as type,
                ci.code as code,
                ci.name as name,
                ci.unit_of_measure as unitOfMeasure,
                vr.vat_code as vatCode,
                CAST(vr.percentage AS DECIMAL) as vatPercentage,
                cic.name as categoryName,
                ci.consignor_party_id as consignorPartyId,
                ci.default_price as defaultPrice,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.sales_account_number as salesAccountNumber,
                NULL as wasteStreamNumber,
                NULL as pickupStreet,
                NULL as pickupBuildingNumber,
                NULL as pickupCity
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            LEFT JOIN vat_rates vr ON ci.vat_rate_id = vr.id
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
                vr.vat_code as vatCode,
                CAST(vr.percentage AS DECIMAL) as vatPercentage,
                cic.name as categoryName,
                ws.consignor_party_id as consignorPartyId,
                ci.default_price as defaultPrice,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.sales_account_number as salesAccountNumber,
                ws.number as wasteStreamNumber,
                pl.street_name as pickupStreet,
                pl.building_number as pickupBuildingNumber,
                pl.city as pickupCity
            FROM waste_streams ws
            INNER JOIN catalog_items ci ON ws.catalog_item_id = ci.id
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            LEFT JOIN vat_rates vr ON ci.vat_rate_id = vr.id
            LEFT JOIN pickup_locations pl ON ws.pickup_location_id = pl.id
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
                vr.vat_code as vatCode,
                CAST(vr.percentage AS DECIMAL) as vatPercentage,
                cic.name as categoryName,
                ci.consignor_party_id as consignorPartyId,
                ci.default_price as defaultPrice,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.sales_account_number as salesAccountNumber,
                NULL as wasteStreamNumber,
                NULL as pickupStreet,
                NULL as pickupBuildingNumber,
                NULL as pickupCity
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            LEFT JOIN vat_rates vr ON ci.vat_rate_id = vr.id
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
    fun getVatPercentage(): java.math.BigDecimal?
    fun getCategoryName(): String?
    fun getConsignorPartyId(): UUID?
    fun getDefaultPrice(): java.math.BigDecimal?
    fun getPurchaseAccountNumber(): String?
    fun getSalesAccountNumber(): String?
    fun getWasteStreamNumber(): String?
    fun getPickupStreet(): String?
    fun getPickupBuildingNumber(): String?
    fun getPickupCity(): String?
}

@Repository
class CatalogItemRepository(
    private val jpaRepository: CatalogItemJpaRepository,
    @PersistenceContext private val entityManager: EntityManager
) : CatalogItems {

    override fun findAllByType(type: CatalogItemType): List<CatalogItem> {
        return jpaRepository.findAllByTypeAndStatus(type, "ACTIVE")
            .map { dto ->
                CatalogItem(
                    id = dto.id,
                    type = dto.type,
                    code = dto.code,
                    name = dto.name,
                    unitOfMeasure = dto.unitOfMeasure,
                    vatCode = dto.vatRate.vatCode,
                    vatPercentage = dto.vatRate.percentage,
                    vatRateId = dto.vatRate.id,
                    categoryName = dto.category?.name,
                    consignorPartyId = dto.consignorParty?.id?.let { CompanyId(it) },
                    defaultPrice = dto.defaultPrice,
                    purchaseAccountNumber = dto.purchaseAccountNumber,
                    salesAccountNumber = dto.salesAccountNumber,
                    wasteStreamNumber = null,
                    itemType = dto.type
                )
            }
    }

    override fun create(catalogItem: CatalogItem): CatalogItem {
        // For LMA import, we create a simple material catalog item
        val dto = CatalogItemDto(
            id = catalogItem.id,
            type = catalogItem.type,
            code = catalogItem.code,
            name = catalogItem.name,
            unitOfMeasure = catalogItem.unitOfMeasure,
            vatRate = entityManager.getReference(VatRateDto::class.java, catalogItem.vatRateId),
            category = null,
            consignorParty = null,
            defaultPrice = catalogItem.defaultPrice,
            status = "ACTIVE",
            purchaseAccountNumber = catalogItem.purchaseAccountNumber,
            salesAccountNumber = catalogItem.salesAccountNumber
        )
        val saved = jpaRepository.save(dto)
        return catalogItem.copy(id = saved.id)
    }

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
                vatPercentage = projection.getVatPercentage(),
                categoryName = projection.getCategoryName(),
                consignorPartyId = projection.getConsignorPartyId()?.let { CompanyId(it) },
                defaultPrice = projection.getDefaultPrice(),
                purchaseAccountNumber = projection.getPurchaseAccountNumber(),
                salesAccountNumber = projection.getSalesAccountNumber(),
                wasteStreamNumber = projection.getWasteStreamNumber()?.let { WasteStreamNumber(it) },
                itemType = CatalogItemType.valueOf(projection.getType()),
                pickupStreet = projection.getPickupStreet(),
                pickupBuildingNumber = projection.getPickupBuildingNumber(),
                pickupCity = projection.getPickupCity(),
            )
        }
    }
}
