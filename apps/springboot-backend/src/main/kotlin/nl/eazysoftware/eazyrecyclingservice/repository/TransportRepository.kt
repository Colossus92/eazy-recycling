package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.*

interface TransportRepository : JpaRepository<TransportDto, UUID> {

    fun findByPickupDateTimeIsBetween(start: Instant, end: Instant): List<TransportDto>

    fun findByDriverIdAndPickupDateTimeIsBetween(driverId: UUID, startDate: Instant, enDate: Instant): List<TransportDto>
    
    fun findByWeightTicketNumber(weightTicketNumber: Long): List<TransportDto>
}
