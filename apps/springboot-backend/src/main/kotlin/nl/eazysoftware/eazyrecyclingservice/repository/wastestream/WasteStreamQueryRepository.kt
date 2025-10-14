package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.application.query.*
import nl.eazysoftware.eazyrecyclingservice.domain.waste.EffectiveStatusPolicy
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamStatus
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationType.COMPANY
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationType.DUTCH_ADDRESS
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationType.NO_PICKUP
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationType.PROXIMITY_DESC
import org.hibernate.Hibernate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
class WasteStreamQueryRepository(
  private val entityManager: EntityManager,
  private val jpaRepository: WasteStreamJpaRepository,
  private val companyRepository: CompanyRepository
) : GetAllWasteStreams, GetWasteStreamByNumber {

  override fun execute(): List<WasteStreamListView> {
    val query = """
            SELECT
                ws.number,
                ws.name,
                e.code,
                pm.code,
                c.name,
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
      val status = columns[14] as String
      val lastActivityAt = columns[15] as Instant
      WasteStreamListView(
        wasteStreamNumber = columns[0] as String,
        wasteName = columns[1] as String,
        euralCode = columns[2] as String,
        processingMethodCode = columns[3] as String,
        consignorPartyName = columns[4] as String,
        pickupLocation = formatPickupLocation(columns),
        deliveryLocation = "${columns[11]} ${columns[12]}, ${columns[13]}",
        lastActivityAt = lastActivityAt.toKotlinInstant(),
        status = EffectiveStatusPolicy.compute(WasteStreamStatus.valueOf(status), lastActivityAt.toKotlinInstant(), Clock.System.now()).toString(),
      )
    }
  }

  private fun formatPickupLocation(columns: Array<*>): String {
    return when (val locationType: String = columns[5] as String) {
      DUTCH_ADDRESS -> "${columns[6]} ${columns[7]}, ${columns[9]}"
      PROXIMITY_DESC -> "${columns[8]}, ${columns[9]}"
      COMPANY -> {
        val companyId = columns[10] as UUID
        val company = companyRepository.findByIdOrNull(companyId) ?: throw IllegalArgumentException("Geen bedrijf gevonden met verwerkersnummer: $companyId")
        "${company.name}, ${company.address.city}"
      }
      NO_PICKUP -> "Geen herkomstlocatie"
      else -> throw IllegalStateException("Unexpected pickup location type: $locationType")
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
      pickupLocation = mapPickupLocation(wasteStream.pickupLocation),
      deliveryLocation = DeliveryLocationView(
        processorPartyId = wasteStream.processorParty.processorId!!,
        processor = mapCompany(wasteStream.processorParty)
      ),
      consignorParty = ConsignorView.CompanyConsignorView(mapCompany(wasteStream.consignorParty)),
      pickupParty = mapCompany(wasteStream.pickupParty),
      dealerParty = wasteStream.dealerParty?.let { mapCompany(it) },
      collectorParty = wasteStream.collectorParty?.let { mapCompany(it) },
      brokerParty = wasteStream.brokerParty?.let { mapCompany(it) },
      status = EffectiveStatusPolicy.compute(WasteStreamStatus.valueOf(wasteStream.status), wasteStream.lastActivityAt.toKotlinInstant(), Clock.System.now()).toString(),
      lastActivityAt = wasteStream.lastActivityAt.toKotlinInstant()
    )
  }

  private fun mapPickupLocation(location: PickupLocationDto): PickupLocationView? {
    return when (val dto = Hibernate.unproxy(location)) {
      is PickupLocationDto.DutchAddressDto ->
        PickupLocationView.DutchAddressView(
          streetName = dto.streetName,
          postalCode = dto.postalCode,
          buildingNumber = dto.buildingNumber,
          buildingNumberAddition = dto.buildingNumberAddition,
          city = dto.city,
          country = dto.country
        )

      is PickupLocationDto.ProximityDescriptionDto ->
        PickupLocationView.ProximityDescriptionView(
          postalCodeDigits = dto.postalCode,
          city = dto.city,
          description = dto.description,
          country = dto.country
        )

      is PickupLocationDto.PickupCompanyDto -> {
        val company = entityManager.find(CompanyDto::class.java, dto.companyId)
        company?.let {
          PickupLocationView.PickupCompanyView(
            company = mapCompany(it)
          )
        }
      }

      else -> null
    }
  }

  private fun mapCompany(company: CompanyDto): CompanyView {
    return CompanyView(
      id = company.id!!,
      name = company.name,
      chamberOfCommerceId = company.chamberOfCommerceId,
      vihbId = company.vihbId,
      processorId = company.processorId,
      address = AddressView(
        street = company.address.streetName ?: "",
        houseNumber = company.address.buildingNumber,
        houseNumberAddition = null,
        postalCode = company.address.postalCode,
        city = company.address.city ?: "",
        country = company.address.country ?: ""
      )
    )
  }
}
