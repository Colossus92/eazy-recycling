package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.application.query.*
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayTime
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.EffectiveStatusPolicy
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamStatus
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.COMPANY
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.DUTCH_ADDRESS
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.NO_PICKUP
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.PROXIMITY_DESC
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyViewMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.nio.ByteBuffer
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

@Repository
class WasteStreamQueryRepository(
  private val entityManager: EntityManager,
  private val jpaRepository: WasteStreamJpaRepository,
  private val companyRepository: CompanyRepository,
  private val pickupLocationMapper: PickupLocationMapper
) : GetAllWasteStreams, GetWasteStreamByNumber {

  override fun execute(): List<WasteStreamListView> {
    val query = """
            SELECT
                ws.number,
                ws.name,
                e.code,
                pm.code,
                c.name,
                c.id,
                pl.location_type,
                pl.street_name,
                pl.building_number,
                pl.proximity_description,
                pl.city,
                pl.company_id,
                dc.street_name,
                dc.building_number,
                dc.city,
                ws.status,
                ws.last_activity_at
            FROM waste_streams ws
            JOIN eural e ON ws.eural_code = e.code
            JOIN processing_methods pm ON ws.processing_method_code = pm.code
            JOIN companies c ON ws.consignor_party_id = c.id
            LEFT JOIN pickup_locations pl ON ws.pickup_location_id = pl.id
            LEFT JOIN companies dc ON ws.processor_party_id = dc.processor_id
            ORDER BY ws.number
        """.trimIndent()

    val results = entityManager.createNativeQuery(query).resultList

    return results.map { row ->
      val columns = row as Array<*>
      val status = columns[15] as String
      val lastActivityAtRaw = columns[16]
      val lastActivityAt = when (lastActivityAtRaw) {
        is OffsetDateTime -> lastActivityAtRaw.toInstant() // Support for H2 Database
        is Instant -> lastActivityAtRaw
        else -> throw IllegalStateException("Unexpected type for last_activity_at: ${lastActivityAtRaw?.javaClass}")
      }
      WasteStreamListView(
        wasteStreamNumber = columns[0] as String,
        wasteName = columns[1] as String,
        euralCode = columns[2] as String,
        processingMethodCode = columns[3] as String,
        consignorPartyName = columns[4] as String,
        consignorPartyId = toUuid(columns[5]),
        pickupLocation = formatPickupLocation(columns),
        deliveryLocation = "${columns[12]} ${columns[13]}, ${columns[14]}",
        lastActivityAt = lastActivityAt.toKotlinInstant().toDisplayTime(),
        status = EffectiveStatusPolicy.compute(
          WasteStreamStatus.valueOf(status),
          lastActivityAt.toKotlinInstant(),
          Clock.System.now()
        ).toString(),
      )
    }
  }

  private fun formatPickupLocation(columns: Array<*>): String {
    return when (val locationType: String = columns[6] as String) {
      DUTCH_ADDRESS -> "${columns[7]} ${columns[8]}, ${columns[10]}"
      PROXIMITY_DESC -> "${columns[9]}, ${columns[10]}"
      COMPANY -> {
        val companyId = toUuid(columns[11])
        val company = companyRepository.findByIdOrNull(companyId)
          ?: throw IllegalArgumentException("Geen bedrijf gevonden met verwerkersnummer: $companyId")
        "${company.name}, ${company.address.city}"
      }

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
      status = EffectiveStatusPolicy.compute(
        WasteStreamStatus.valueOf(wasteStream.status),
        wasteStream.lastActivityAt.toKotlinInstant(),
        Clock.System.now()
      ).toString(),
      lastActivityAt = wasteStream.lastActivityAt.toKotlinInstant().toDisplayTime()
    )
  }
}
