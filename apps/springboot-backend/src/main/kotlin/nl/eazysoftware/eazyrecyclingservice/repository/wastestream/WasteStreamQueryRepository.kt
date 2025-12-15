package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.*
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayTime
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.EffectiveStatusPolicy
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamStatus
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.COMPANY
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.DUTCH_ADDRESS
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.NO_PICKUP
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.PROJECT_LOCATION
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.PROXIMITY_DESC
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyViewMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.nio.ByteBuffer
import java.time.OffsetDateTime
import java.util.*
import kotlin.time.Clock
import kotlin.time.toKotlinInstant
import java.time.Instant as JavaInstant

data class WasteStreamQueryResult(
  val wasteStreamNumber: String,
  val wasteName: String,
  val euralCode: String,
  val processingMethodCode: String,
  val consignorPartyName: String,
  val consignorPartyId: Any?,
  val pickupLocationType: String?,
  val pickupLocationStreetName: String?,
  val pickupLocationBuildingNumber: String?,
  val pickupLocationProximityDescription: String?,
  val pickupLocationCity: String?,
  val pickupLocationCompanyId: Any?,
  val deliveryLocationStreetName: String?,
  val deliveryLocationBuildingNumber: String?,
  val deliveryLocationCity: String?,
  val status: String,
  val lastActivityAt: Any?
)

@Repository
class WasteStreamQueryRepository(
  private val entityManager: EntityManager,
  private val jpaRepository: WasteStreamJpaRepository,
  private val companyRepository: CompanyJpaRepository,
  private val pickupLocationMapper: PickupLocationMapper
) : GetAllWasteStreams, GetWasteStreamByNumber {

  override fun execute(consignor: UUID?, status: String?): List<WasteStreamListView> {
    val baseQuery = """
            SELECT
                ws.number as wasteStreamNumber,
                ws.name as wasteName,
                e.code as euralCode,
                pm.code as processingMethodCode,
                c.name as consignorPartyName,
                c.id as consignorPartyId,
                pl.location_type as pickupLocationType,
                pl.street_name as pickupLocationStreetName,
                pl.building_number as pickupLocationBuildingNumber,
                pl.proximity_description as pickupLocationProximityDescription,
                pl.city as pickupLocationCity,
                pl.company_id as pickupLocationCompanyId,
                dc.street_name as deliveryLocationStreetName,
                dc.building_number as deliveryLocationBuildingNumber,
                dc.city as deliveryLocationCity,
                ws.status,
                ws.last_modified_at as lastActivityAt
            FROM waste_streams ws
            JOIN eural e ON ws.eural_code = e.code
            JOIN processing_methods pm ON ws.processing_method_code = pm.code
            JOIN companies c ON ws.consignor_party_id = c.id
            LEFT JOIN pickup_locations pl ON ws.pickup_location_id = pl.id
            LEFT JOIN companies dc ON ws.processor_party_id = dc.processor_id
        """.trimIndent()

    val whereClauses = mutableListOf<String>()
    if (consignor != null) {
      whereClauses.add("ws.consignor_party_id = :consignor")
    }
    if (status != null) {
      whereClauses.add("ws.status = :status")
    }

    val whereClause = if (whereClauses.isNotEmpty()) {
      " WHERE " + whereClauses.joinToString(" AND ")
    } else {
      ""
    }

    val query = "$baseQuery$whereClause ORDER BY ws.number"

    val nativeQuery = entityManager.createNativeQuery(query, WasteStreamQueryResult::class.java)
    if (consignor != null) {
      nativeQuery.setParameter("consignor", consignor)
    }
    if (status != null) {
      nativeQuery.setParameter("status", status)
    }

    @Suppress("UNCHECKED_CAST")
    val results = nativeQuery.resultList as List<WasteStreamQueryResult>

    return results.map { result ->
      val lastActivityAt = when (result.lastActivityAt) {
        is OffsetDateTime -> result.lastActivityAt.toInstant() // Support for H2 Database
        is JavaInstant -> result.lastActivityAt
        else -> throw IllegalStateException("Unexpected type for last_modified_at: ${result.lastActivityAt?.javaClass}")
      }
      val effectiveStatus = EffectiveStatusPolicy.compute(
        WasteStreamStatus.valueOf(result.status),
        lastActivityAt.toKotlinInstant(),
        Clock.System.now()
      ).toString()
      WasteStreamListView(
        wasteStreamNumber = result.wasteStreamNumber,
        wasteName = result.wasteName,
        euralCode = result.euralCode,
        processingMethodCode = result.processingMethodCode,
        consignorPartyName = result.consignorPartyName,
        consignorPartyId = toUuid(result.consignorPartyId),
        pickupLocation = formatPickupLocation(result),
        deliveryLocation = "${result.deliveryLocationStreetName} ${result.deliveryLocationBuildingNumber}, ${result.deliveryLocationCity}",
        lastActivityAt = lastActivityAt.toKotlinInstant().toDisplayTime(),
        status = effectiveStatus,
        isEditable = effectiveStatus == WasteStreamStatus.DRAFT.name,
      )
    }
  }

  private fun formatPickupLocation(result: WasteStreamQueryResult): String {
    return when (val locationType: String? = result.pickupLocationType) {
      DUTCH_ADDRESS -> "${result.pickupLocationStreetName} ${result.pickupLocationBuildingNumber}, ${result.pickupLocationCity}"
      PROXIMITY_DESC -> "${result.pickupLocationProximityDescription}, ${result.pickupLocationCity}"
      COMPANY -> {
        val companyId = toUuid(result.pickupLocationCompanyId)
        val company = companyRepository.findByIdOrNull(companyId)
          ?: throw IllegalArgumentException("Geen bedrijf gevonden met verwerkersnummer: $companyId")
        "${company.name}, ${company.address.city}"
      }
      PROJECT_LOCATION -> "${result.pickupLocationStreetName} ${result.pickupLocationBuildingNumber}, ${result.pickupLocationCity}"
      NO_PICKUP -> "Geen herkomstlocatie"
      else -> throw IllegalStateException("Unexpected pickup location type: $locationType")
    }
  }

  /**
   * Converts a UUID column value to java.util.UUID.
   * Handles both PostgreSQL (returns UUID directly) and H2 (returns byte array).
   */
  private fun toUuid(value: Any?): UUID {
    return when (value) {
      is UUID -> value
      is ByteArray -> {
        val buffer = ByteBuffer.wrap(value)
        UUID(buffer.long, buffer.long)
      }

      else -> throw IllegalStateException("Unexpected type for UUID column: ${value?.javaClass}")
    }
  }

  override fun execute(wasteStreamNumber: WasteStreamNumber): WasteStreamDetailView? {
    val wasteStream = jpaRepository.findByIdOrNull(wasteStreamNumber.number) ?: return null

    return WasteStreamDetailView(
      wasteStreamNumber = wasteStream.number,
      wasteType = WasteTypeView(
        name = wasteStream.name,
        euralCode = EuralCodeView(
          code = wasteStream.euralCode.code,
          description = wasteStream.euralCode.description
        ),
        processingMethod = ProcessingMethodView(
          code = wasteStream.processingMethodCode.code,
          description = wasteStream.processingMethodCode.description
        )
      ),
      collectionType = wasteStream.wasteCollectionType,
      pickupLocation = pickupLocationMapper.toView(wasteStream.pickupLocation),
      deliveryLocation = DeliveryLocationView(
        processorPartyId = wasteStream.processorParty.processorId!!,
        processor = CompanyViewMapper.map(wasteStream.processorParty)
      ),
      consignorParty = ConsignorView.CompanyConsignorView(CompanyViewMapper.map(wasteStream.consignorParty)),
      consignorClassification = wasteStream.consignorClassification,
      pickupParty = CompanyViewMapper.map(wasteStream.pickupParty),
      dealerParty = wasteStream.dealerParty?.let { CompanyViewMapper.map(it) },
      collectorParty = wasteStream.collectorParty?.let { CompanyViewMapper.map(it) },
      brokerParty = wasteStream.brokerParty?.let { CompanyViewMapper.map(it) },
      materialId = wasteStream.catalogItem?.id,
      status = EffectiveStatusPolicy.compute(
        WasteStreamStatus.valueOf(wasteStream.status),
        wasteStream.updatedAt?.toKotlinInstant(),
        Clock.System.now()
      ).toString(),
      createdAt = wasteStream.createdAt,
      createdBy = wasteStream.createdBy,
      updatedAt = wasteStream.updatedAt,
      updatedBy = wasteStream.updatedBy
    )
  }
}
