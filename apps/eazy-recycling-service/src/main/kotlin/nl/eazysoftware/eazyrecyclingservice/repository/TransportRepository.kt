package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface TransportRepository : JpaRepository<TransportDto, UUID> {

    fun findByGoods_Uuid(waybillId: UUID): TransportDto?

    fun findByPickupDateTimeIsBetween(start: LocalDateTime, end: LocalDateTime): List<TransportDto>

    fun findByDriverIdAndPickupDateTimeIsBetween(driverId: UUID, startDate: LocalDateTime, enDate: LocalDateTime): List<TransportDto>
}