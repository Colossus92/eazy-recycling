package nl.eazysoftware.eazyrecyclingservice.application.usecase.truck

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Trucks
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface DeleteTruck {
  fun handle(licensePlate: LicensePlate)
}

@Service
class DeleteTruckService(
  private val trucks: Trucks
) : DeleteTruck {

  @Transactional
  override fun handle(licensePlate: LicensePlate) {
    // Verify truck exists before deleting
    trucks.findByLicensePlate(licensePlate)
      ?: throw EntityNotFoundException("Vrachtwagen met kenteken ${licensePlate.value} niet gevonden")

    trucks.deleteByLicensePlate(licensePlate)
  }
}
