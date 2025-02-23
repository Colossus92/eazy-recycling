package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import org.springframework.data.jpa.repository.JpaRepository

interface LocationRepository : JpaRepository<LocationDto , String>{
    fun findByAddress_PostalCodeAndAddress_BuildingNumber(streetName: String, buildingNumber: String): LocationDto?

}