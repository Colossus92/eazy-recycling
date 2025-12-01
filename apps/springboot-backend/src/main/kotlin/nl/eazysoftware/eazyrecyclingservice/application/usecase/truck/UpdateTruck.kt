package nl.eazysoftware.eazyrecyclingservice.application.usecase.truck

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.Truck
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Trucks
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UpdateTruck {
  fun handle(cmd: TruckCommand): TruckResult
}

@Service
class UpdateTruckService(
  private val trucks: Trucks
) : UpdateTruck {

  @Transactional
  override fun handle(cmd: TruckCommand): TruckResult {
    // Verify truck exists
    trucks.findByLicensePlate(cmd.licensePlate)
      ?: throw EntityNotFoundException("Vrachtwagen met kenteken ${cmd.licensePlate.value} niet gevonden")

    val truck = Truck(
      licensePlate = cmd.licensePlate,
      brand = cmd.brand,
      description = cmd.description,
      carrierPartyId = cmd.carrierPartyId,
      createdAt = null,
      createdBy = null,
      updatedAt = null,
      updatedBy = null
    )

    trucks.save(truck)

    return TruckResult(licensePlate = cmd.licensePlate.value)
  }
}
