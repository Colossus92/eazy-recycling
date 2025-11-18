package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.GetCompatibleWasteStreams
import nl.eazysoftware.eazyrecyclingservice.application.query.WasteStreamListView
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayTime
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.EffectiveStatusPolicy
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamStatus
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.nio.ByteBuffer
import java.time.OffsetDateTime
import java.util.*
import kotlin.time.Clock
import kotlin.time.toKotlinInstant
import java.time.Instant as JavaInstant

@Repository
class GetCompatibleWasteStreamsAdapter(
  private val entityManager: EntityManager,
  private val jpaRepository: WasteStreamJpaRepository,
  private val companyRepository: CompanyJpaRepository
) : GetCompatibleWasteStreams {

  override fun execute(wasteStreamNumber: WasteStreamNumber): List<WasteStreamListView> {
    // First, get the reference waste stream to extract compatibility criteria
    val referenceStream = jpaRepository.findByIdOrNull(wasteStreamNumber.number)
      ?: throw jakarta.persistence.EntityNotFoundException("Afvalstroom met nummer ${wasteStreamNumber.number} niet gevonden")

    // Query all waste streams that match the compatibility criteria:
    // - Same pickup location
    // - Same processor party (delivery location)
    // - Same consignor party
    val query = """
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
                ws.last_activity_at as lastActivityAt
            FROM waste_streams ws
            JOIN eural e ON ws.eural_code = e.code
            JOIN processing_methods pm ON ws.processing_method_code = pm.code
            JOIN companies c ON ws.consignor_party_id = c.id
            LEFT JOIN pickup_locations pl ON ws.pickup_location_id = pl.id
            LEFT JOIN companies dc ON ws.processor_party_id = dc.processor_id
            WHERE ws.pickup_location_id = :pickupLocationId
              AND ws.processor_party_id = :processorPartyId
              AND ws.consignor_party_id = :consignorPartyId
            ORDER BY ws.number
        """.trimIndent()

    val nativeQuery = entityManager.createNativeQuery(query, WasteStreamQueryResult::class.java)
    nativeQuery.setParameter("pickupLocationId", referenceStream.pickupLocation.id)
    nativeQuery.setParameter("processorPartyId", referenceStream.processorParty.processorId)
    nativeQuery.setParameter("consignorPartyId", referenceStream.consignorParty.id)

    @Suppress("UNCHECKED_CAST")
    val results = nativeQuery.resultList as List<WasteStreamQueryResult>

    return results.map { result ->
      val lastActivityAt = when (result.lastActivityAt) {
        is OffsetDateTime -> result.lastActivityAt.toInstant() // Support for H2 Database
        is JavaInstant -> result.lastActivityAt
        else -> throw IllegalStateException("Unexpected type for last_activity_at: ${result.lastActivityAt?.javaClass}")
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
      PickupLocationType.DUTCH_ADDRESS -> "${result.pickupLocationStreetName} ${result.pickupLocationBuildingNumber}, ${result.pickupLocationCity}"
      PickupLocationType.PROXIMITY_DESC -> "${result.pickupLocationProximityDescription}, ${result.pickupLocationCity}"
      PickupLocationType.COMPANY -> {
        val companyId = toUuid(result.pickupLocationCompanyId)
        val company = companyRepository.findByIdOrNull(companyId)
          ?: throw IllegalArgumentException("Geen bedrijf gevonden met verwerkersnummer: $companyId")
        "${company.name}, ${company.address.city}"
      }
      PickupLocationType.PROJECT_LOCATION -> "${result.pickupLocationStreetName} ${result.pickupLocationBuildingNumber}, ${result.pickupLocationCity}"
      PickupLocationType.NO_PICKUP -> "Geen herkomstlocatie"
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
}
