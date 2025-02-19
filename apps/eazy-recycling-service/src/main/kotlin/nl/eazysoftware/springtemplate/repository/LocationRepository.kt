package nl.eazysoftware.springtemplate.repository

import nl.eazysoftware.springtemplate.repository.entity.waybill.LocationDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.WaybillDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface LocationRepository : JpaRepository<LocationDto , String>{
    fun findByAddress_PostalCodeAndAddress_BuildingNumber(streetName: String, buildingNumber: String): LocationDto?

}