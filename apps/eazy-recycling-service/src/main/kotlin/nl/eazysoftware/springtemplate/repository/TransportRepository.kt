package nl.eazysoftware.springtemplate.repository

import nl.eazysoftware.springtemplate.repository.entity.transport.TransportDto
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface TransportRepository : JpaRepository<TransportDto, UUID> {

    fun findByWaybill_PickupDateTimeBetween(start: LocalDateTime, end: LocalDateTime): List<TransportDto>
}