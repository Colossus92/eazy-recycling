package nl.eazysoftware.eazyrecyclingservice.repository.truck

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.Truck
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Trucks
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.TruckDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

interface TruckJpaRepository: JpaRepository<TruckDto, String>

@Repository
class TruckRepositoryAdapter(
  private val jpaRepository: TruckJpaRepository,
  private val truckMapper: TruckMapper
) : Trucks {

  override fun save(truck: Truck): Truck {
    val dto = truckMapper.toDto(truck)
    val savedDto = jpaRepository.save(dto)
    return truckMapper.toDomain(savedDto)
  }

  override fun findAll(): List<Truck> {
    return jpaRepository.findAll()
      .map { truckMapper.toDomain(it) }
  }

  override fun findByLicensePlate(licensePlate: LicensePlate): Truck? {
    return jpaRepository.findByIdOrNull(licensePlate.value.uppercase())
      ?.let { truckMapper.toDomain(it) }
  }

  override fun deleteByLicensePlate(licensePlate: LicensePlate) {
    jpaRepository.deleteById(licensePlate.value.uppercase())
  }

  override fun existsByLicensePlate(licensePlate: LicensePlate): Boolean {
    return jpaRepository.existsById(licensePlate.value.uppercase())
  }
}
