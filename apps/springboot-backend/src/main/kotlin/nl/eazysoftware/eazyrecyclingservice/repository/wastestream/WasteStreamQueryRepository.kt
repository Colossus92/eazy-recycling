package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.*
import nl.eazysoftware.eazyrecyclingservice.application.usecase.DeleteWasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class WasteStreamQueryRepository(
  private val entityManager: EntityManager,
  private val jpaRepository: WasteStreamJpaRepository,
  private val deleteWasteStream: DeleteWasteStream
) : GetAllWasteStreams, GetWasteStreamByNumber {

  override fun execute(): List<WasteStreamListView> {
    val query = """
            SELECT
                ws.number,
                ws.name,
                e.code,
                pm.code,
                c.chamber_of_commerce_id,
                c.name,
                pl.postal_code,
                pl.street_name,
                pl.building_number,
                pl.city,
                dc.postal_code,
                dc.street_name,
                dc.building_number,
                dc.city
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
      WasteStreamListView(
        wasteStreamNumber = columns[0] as String,
        wasteName = columns[1] as String,
        euralCode = columns[2] as String,
        processingMethodCode = columns[3] as String,
        consignorPartyChamberOfCommerceId = columns[4] as String?,
        consignorPartyName = columns[5] as String,
        pickupLocationPostalCode = columns[6] as String?,
        pickupLocationStreetName = columns[7] as String?,
        pickupLocationNumber = columns[8] as String?,
        pickupLocationCity = columns[9] as String,
        deliveryLocationPostalCode = columns[10] as String?,
        deliveryLocationStreetName = columns[11] as String,
        deliveryLocationNumber = columns[12] as String?,
        deliveryLocationCity = columns[13] as String,
      )
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
      brokerParty = wasteStream.brokerParty?.let { mapCompany(it) }
    )
  }

  private fun mapPickupLocation(location: PickupLocationDto): PickupLocationView? {
    return when (location) {
      is PickupLocationDto.DutchAddressDto ->
        PickupLocationView.DutchAddressView(
          streetName = location.streetName,
          postalCode = location.postalCode,
          buildingNumber = location.buildingNumber,
          buildingNumberAddition = location.buildingNumberAddition,
          city = location.city,
          country = location.country
        )

      is PickupLocationDto.ProximityDescriptionDto ->
        PickupLocationView.ProximityDescriptionView(
          postalCodeDigits = location.postalCode,
          city = location.city,
          description = location.description,
          country = location.country
        )

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
