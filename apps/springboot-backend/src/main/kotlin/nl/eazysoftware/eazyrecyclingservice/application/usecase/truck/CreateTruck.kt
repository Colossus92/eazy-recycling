package nl.eazysoftware.eazyrecyclingservice.application.usecase.truck

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.Truck
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Trucks
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CreateTruck {
  fun handle(cmd: TruckCommand): TruckResult
}

@Service
class CreateTruckService(
  private val trucks: Trucks
) : CreateTruck {

  @Transactional
  override fun handle(cmd: TruckCommand): TruckResult {
    // Check for duplicates
    if (trucks.existsByLicensePlate(cmd.licensePlate)) {
      throw DuplicateKeyException("Vrachtwagen met kenteken ${cmd.licensePlate.value} bestaat al")
    }

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

    val savedTruck = trucks.save(truck)

    return TruckResult(licensePlate = savedTruck.licensePlate.value)
  }
}
