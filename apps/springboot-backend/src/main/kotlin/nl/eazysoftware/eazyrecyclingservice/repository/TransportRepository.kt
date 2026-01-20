package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.time.LocalDate
import java.util.*

interface TransportRepository : JpaRepository<TransportDto, UUID> {

    fun findByPickupTimingDateBetween(start: LocalDate, end: LocalDate): List<TransportDto>

    fun findByDriverIdAndPickupTimingDateBetween(driverId: UUID, startDate: LocalDate, endDate: LocalDate): List<TransportDto>
    
    fun findByWeightTicketNumber(weightTicketNumber: Long): List<TransportDto>

    /**
     * Optimized query for planning view that eagerly fetches all required associations
     * to avoid N+1 query problems. Uses LEFT JOIN FETCH for optional relationships.
     * 
     * Includes transports where:
     * - pickupTiming.date is within the date range, OR
     * - pickupTiming.date is null AND deliveryTiming.date is within the date range
     * 
     * Note: Sorting is done in-memory in PlanningService.createTransportsView() because
     * PostgreSQL's SELECT DISTINCT requires ORDER BY expressions to be in the select list.
     */
    @Query("""
        SELECT DISTINCT t FROM TransportDto t
        LEFT JOIN FETCH t.truck truck
        LEFT JOIN FETCH truck.carrierParty
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.pickupLocation
        LEFT JOIN FETCH t.deliveryLocation
        LEFT JOIN FETCH t.wasteContainer
        WHERE (
            (t.pickupTiming.date BETWEEN :startDate AND :endDate)
            OR (t.pickupTiming.date IS NULL AND t.deliveryTiming.date BETWEEN :startDate AND :endDate)
        )
        AND (:truckId IS NULL OR truck.licensePlate = :truckId)
        AND (:driverId IS NULL OR t.driver.id = :driverId)
    """)
    fun findForPlanningView(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("truckId") truckId: String?,
        @Param("driverId") driverId: UUID?
    ): List<TransportDto>
}
