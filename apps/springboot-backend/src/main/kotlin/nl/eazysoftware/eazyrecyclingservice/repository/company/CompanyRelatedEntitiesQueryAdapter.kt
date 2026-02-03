package nl.eazysoftware.eazyrecyclingservice.repository.company

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.EffectiveStatusPolicy
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamStatus
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.time.Clock
import kotlin.time.toKotlinInstant

@Repository
class WasteStreamsByCompanyQueryAdapter(
    private val entityManager: EntityManager,
    private val companyRepository: CompanyJpaRepository
) : GetWasteStreamsByCompany {

    override fun execute(companyId: UUID): List<WasteStreamByCompanyView> {
        val query = """
            SELECT
                ws.number as wasteStreamNumber,
                ws.name as wasteName,
                pl.location_type as pickupLocationType,
                pl.street_name as pickupLocationStreetName,
                pl.building_number as pickupLocationBuildingNumber,
                pl.proximity_description as pickupLocationProximityDescription,
                pl.city as pickupLocationCity,
                pl.company_id as pickupLocationCompanyId,
                ws.status,
                ws.last_modified_at as lastActivityAt
            FROM waste_streams ws
            LEFT JOIN pickup_locations pl ON ws.pickup_location_id = pl.id
            WHERE ws.consignor_party_id = :companyId
            ORDER BY ws.number
        """.trimIndent()

        val results = entityManager.createNativeQuery(query)
            .setParameter("companyId", companyId)
            .resultList

        return results.map { row ->
            val columns = row as Array<*>
            val lastActivityAt = when (val activity = columns[9]) {
                is OffsetDateTime -> activity.toInstant()
                is Instant -> activity
                else -> null
            }
            val effectiveStatus = if (lastActivityAt != null) {
                EffectiveStatusPolicy.compute(
                    WasteStreamStatus.valueOf(columns[8] as String),
                    lastActivityAt.toKotlinInstant(),
                    Clock.System.now()
                ).toString()
            } else {
                columns[8] as String
            }

            WasteStreamByCompanyView(
                wasteStreamNumber = columns[0] as String,
                wasteName = columns[1] as String,
                pickupLocation = formatPickupLocation(
                    locationType = columns[2] as String?,
                    streetName = columns[3] as String?,
                    buildingNumber = columns[4] as String?,
                    proximityDescription = columns[5] as String?,
                    city = columns[6] as String?,
                    companyId = columns[7]
                ),
                status = effectiveStatus,
            )
        }
    }

    private fun formatPickupLocation(
        locationType: String?,
        streetName: String?,
        buildingNumber: String?,
        proximityDescription: String?,
        city: String?,
        companyId: Any?
    ): String {
        return when (locationType) {
            PickupLocationType.DUTCH_ADDRESS -> "$streetName $buildingNumber, $city"
            PickupLocationType.PROXIMITY_DESC -> "$proximityDescription, $city"
            PickupLocationType.COMPANY -> {
                val uuid = toUuid(companyId)
                val company = companyRepository.findByIdOrNull(uuid)
                "${company?.name ?: "Onbekend"}, ${company?.address?.city ?: ""}"
            }
            PickupLocationType.PROJECT_LOCATION -> "$streetName $buildingNumber, $city"
            PickupLocationType.NO_PICKUP -> "Geen herkomstlocatie"
            else -> "Onbekend"
        }
    }

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

@Repository
class WeightTicketsByCompanyQueryAdapter(
    private val entityManager: EntityManager,
    private val companyRepository: CompanyJpaRepository
) : GetWeightTicketsByCompany {

    override fun execute(companyId: UUID): List<WeightTicketByCompanyView> {
        val query = """
            SELECT
                wt.number,
                (SELECT SUM(wtl.weight_value)
                 FROM weight_ticket_lines wtl
                 WHERE wtl.weight_ticket_id = wt.id) as total_weight,
                wt.weighted_at,
                pl.location_type as pickupLocationType,
                pl.street_name as pickupLocationStreetName,
                pl.building_number as pickupLocationBuildingNumber,
                pl.proximity_description as pickupLocationProximityDescription,
                pl.city as pickupLocationCity,
                pl.company_id as pickupLocationCompanyId,
                wt.status
            FROM weight_tickets wt
            LEFT JOIN pickup_locations pl ON wt.pickup_location_id = pl.id
            WHERE wt.consignor_party_id = :companyId
            ORDER BY wt.number DESC
        """.trimIndent()

        val results = entityManager.createNativeQuery(query)
            .setParameter("companyId", companyId)
            .resultList

        return results.map { row ->
            val columns = row as Array<*>
            WeightTicketByCompanyView(
                id = columns[0] as Long,
                totalWeight = (columns[1] as? Number)?.toDouble(),
                weighingDate = columns[2] as? Instant,
                pickupLocation = formatPickupLocation(
                    locationType = columns[3] as String?,
                    streetName = columns[4] as String?,
                    buildingNumber = columns[5] as String?,
                    proximityDescription = columns[6] as String?,
                    city = columns[7] as String?,
                    companyId = columns[8]
                ),
                status = columns[9] as String,
            )
        }
    }

    private fun formatPickupLocation(
        locationType: String?,
        streetName: String?,
        buildingNumber: String?,
        proximityDescription: String?,
        city: String?,
        companyId: Any?
    ): String? {
        if (locationType == null) return null
        return when (locationType) {
            PickupLocationType.DUTCH_ADDRESS -> "$streetName $buildingNumber, $city"
            PickupLocationType.PROXIMITY_DESC -> "$proximityDescription, $city"
            PickupLocationType.COMPANY -> {
                val uuid = toUuid(companyId)
                val company = companyRepository.findByIdOrNull(uuid)
                "${company?.name ?: "Onbekend"}, ${company?.address?.city ?: ""}"
            }
            PickupLocationType.PROJECT_LOCATION -> "$streetName $buildingNumber, $city"
            PickupLocationType.NO_PICKUP -> "Geen herkomstlocatie"
            else -> "Onbekend"
        }
    }

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

@Repository
class TransportsByCompanyQueryAdapter(
    private val entityManager: EntityManager,
    private val companyRepository: CompanyJpaRepository
) : GetTransportsByCompany {

    override fun execute(companyId: UUID): List<TransportByCompanyView> {
        val query = """
            SELECT
                t.id,
                t.display_number,
                t.pickup_date,
                pl.location_type as pickupLocationType,
                pl.street_name as pickupLocationStreetName,
                pl.building_number as pickupLocationBuildingNumber,
                pl.proximity_description as pickupLocationProximityDescription,
                pl.city as pickupLocationCity,
                pl.company_id as pickupLocationCompanyId,
                t.driver_id,
                t.truck_id,
                t.transport_hours
            FROM transports t
            LEFT JOIN pickup_locations pl ON t.pickup_location_id = pl.id
            WHERE t.consignor_party_id = :companyId
            ORDER BY t.pickup_date DESC NULLS LAST, t.display_number DESC
        """.trimIndent()

        val results = entityManager.createNativeQuery(query)
            .setParameter("companyId", companyId)
            .resultList

        return results.map { row ->
            val columns = row as Array<*>
            val driverId = columns[9]
            val truckId = columns[10]
            val transportHours = columns[11]

            val status = when {
                driverId == null || truckId == null -> "UNPLANNED"
                transportHours != null -> "FINISHED"
                else -> "PLANNED"
            }

            TransportByCompanyView(
                id = toUuid(columns[0]),
                displayNumber = columns[1] as String?,
                date = columns[2] as? LocalDate,
                pickupLocation = formatPickupLocation(
                    locationType = columns[3] as String?,
                    streetName = columns[4] as String?,
                    buildingNumber = columns[5] as String?,
                    proximityDescription = columns[6] as String?,
                    city = columns[7] as String?,
                    companyId = columns[8]
                ),
                status = status,
            )
        }
    }

    private fun formatPickupLocation(
        locationType: String?,
        streetName: String?,
        buildingNumber: String?,
        proximityDescription: String?,
        city: String?,
        companyId: Any?
    ): String? {
        if (locationType == null) return null
        return when (locationType) {
            PickupLocationType.DUTCH_ADDRESS -> "$streetName $buildingNumber, $city"
            PickupLocationType.PROXIMITY_DESC -> "$proximityDescription, $city"
            PickupLocationType.COMPANY -> {
                val uuid = toUuid(companyId)
                val company = companyRepository.findByIdOrNull(uuid)
                "${company?.name ?: "Onbekend"}, ${company?.address?.city ?: ""}"
            }
            PickupLocationType.PROJECT_LOCATION -> "$streetName $buildingNumber, $city"
            PickupLocationType.NO_PICKUP -> "Geen herkomstlocatie"
            else -> "Onbekend"
        }
    }

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

@Repository
class InvoicesByCompanyQueryAdapter(
    private val entityManager: EntityManager
) : GetInvoicesByCompany {

    override fun execute(companyId: UUID): List<InvoiceByCompanyView> {
        val query = """
            SELECT
                i.id,
                i.invoice_number,
                i.invoice_type,
                i.status,
                COALESCE((SELECT SUM(il.quantity * il.unit_price * (1 + il.vat_percentage / 100))
                          FROM invoice_lines il
                          WHERE il.invoice_id = i.id), 0) as total_incl_vat
            FROM invoices i
            WHERE i.customer_company_id = :companyId
            ORDER BY i.invoice_number DESC NULLS LAST
        """.trimIndent()

        val results = entityManager.createNativeQuery(query)
            .setParameter("companyId", companyId)
            .resultList

        return results.map { row ->
            val columns = row as Array<*>
            InvoiceByCompanyView(
                id = toUuid(columns[0]),
                invoiceNumber = columns[1] as String?,
                invoiceType = columns[2] as String,
                status = columns[3] as String,
                totalInclVat = (columns[4] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO,
            )
        }
    }

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
