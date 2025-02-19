package nl.eazysoftware.springtemplate.repository

import nl.eazysoftware.springtemplate.repository.entity.transport.TransportDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface TransportRepository : JpaRepository<TransportDto, UUID> {

    fun findByTruckIsNotNullAndPickupDateTimeBetween(start: LocalDateTime, end: LocalDateTime): List<TransportDto>
    fun findByGoods_Uuid(waybillId: UUID): TransportDto?

    @Query(
        """
        SELECT t FROM TransportDto t 
        WHERE t.goods IS NOT NULL
        AND t.truck IS NULL
        AND t.pickupDateTime BETWEEN :start AND :end
    """
    )
    fun findUnassignedWaybills(@Param("start") startDateTime: LocalDateTime,
                               @Param("end") endDateTime: LocalDateTime
    ): List<TransportDto>

    fun findAllByGoodsNotNull(): List<TransportDto>
}