package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.Truck

interface Trucks {
  fun save(truck: Truck): Truck
  fun findAll(): List<Truck>
  fun findByLicensePlate(licensePlate: LicensePlate): Truck?
  fun deleteByLicensePlate(licensePlate: LicensePlate)
  fun existsByLicensePlate(licensePlate: LicensePlate): Boolean
}
