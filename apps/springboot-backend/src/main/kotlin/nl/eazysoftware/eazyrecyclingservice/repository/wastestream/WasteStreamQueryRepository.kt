package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllWasteStreams
import nl.eazysoftware.eazyrecyclingservice.application.query.WasteStreamListView
import org.springframework.stereotype.Repository

@Repository
class WasteStreamQueryRepository(
    private val entityManager: EntityManager
) : GetAllWasteStreams {

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
                pl.building_number,
                dc.postal_code,
                dc.building_number
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
                pickupLocationNumber = columns[7] as String?,
                deliveryLocationPostalCode = columns[8] as String?,
                deliveryLocationNumber = columns[9] as String?
            )
        }
    }
}
