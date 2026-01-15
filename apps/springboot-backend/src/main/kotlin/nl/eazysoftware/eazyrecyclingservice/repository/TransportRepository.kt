package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface TransportRepository : JpaRepository<TransportDto, UUID> {

    fun findByPickupDateTimeIsBetween(start: Instant, end: Instant): List<TransportDto>

    fun findByDriverIdAndPickupDateTimeIsBetween(driverId: UUID, startDate: Instant, enDate: Instant): List<TransportDto>
    
    fun findByWeightTicketNumber(weightTicketNumber: Long): List<TransportDto>

    /**
     * Optimized query for planning view that eagerly fetches all required associations
     * to avoid N+1 query problems. Uses LEFT JOIN FETCH for optional relationships.
     */
    @Query("""
        SELECT DISTINCT t FROM TransportDto t
        LEFT JOIN FETCH t.truck truck
        LEFT JOIN FETCH truck.carrierParty
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.pickupLocation
        LEFT JOIN FETCH t.deliveryLocation
        LEFT JOIN FETCH t.wasteContainer
        WHERE t.pickupDateTime BETWEEN :start AND :end
        AND (:truckId IS NULL OR truck.licensePlate = :truckId)
        AND (:driverId IS NULL OR t.driver.id = :driverId)
        ORDER BY t.pickupDateTime, t.sequenceNumber
    """)
    fun findForPlanningView(
        @Param("start") start: Instant,
        @Param("end") end: Instant,
        @Param("truckId") truckId: String?,
        @Param("driverId") driverId: UUID?
    ): List<TransportDto>
}
