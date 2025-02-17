package nl.eazysoftware.springtemplate.repository

import nl.eazysoftware.springtemplate.repository.entity.truck.Truck
import org.springframework.data.jpa.repository.JpaRepository

interface TruckRepository: JpaRepository<Truck, String> {
    fun findByLicensePlate(truckId: String): Truck?
}