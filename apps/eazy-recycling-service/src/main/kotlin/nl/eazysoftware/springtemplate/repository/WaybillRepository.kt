package nl.eazysoftware.springtemplate.repository

import nl.eazysoftware.springtemplate.repository.entity.waybill.WaybillDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface WaybillRepository : JpaRepository<WaybillDto, UUID>{
    @Query("""
        SELECT w FROM WaybillDto w 
        LEFT JOIN TransportDto t ON w.uuid = t.goods.uuid
        WHERE t.id IS NULL
        AND w.pickupDateTime BETWEEN :start AND :end
    """)
    fun findUnassignedWaybills(@Param("start") startDateTime: LocalDateTime,
                               @Param("end") endDateTime: LocalDateTime
    ): List<WaybillDto>
}