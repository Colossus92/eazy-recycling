package nl.eazysoftware.springtemplate.repository

import nl.eazysoftware.springtemplate.repository.entity.waybill.LocationDto
import org.springframework.data.jpa.repository.JpaRepository

interface LocationRepository : JpaRepository<LocationDto , String>{
    fun findByAddress_PostalCodeAndAddress_BuildingNumber(streetName: String, buildingNumber: String): LocationDto?

}