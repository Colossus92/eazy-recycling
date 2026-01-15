package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Trucks
import org.springframework.stereotype.Service

interface GetAllTrucks {
  fun handle(): List<TruckView>
}

@Service
class GetAllTrucksQuery(
  private val trucks: Trucks
) : GetAllTrucks {

  override fun handle(): List<TruckView> {
    return trucks.findAll().map { truck ->
      TruckView(
        licensePlate = truck.licensePlate.value,
        brand = truck.brand,
        description = truck.description,
        displayName = truck.displayName,
        carrierPartyId = truck.carrierPartyId?.uuid?.toString(),
        carrierCompanyName = null, // Will be populated by join if needed
        createdAt = truck.createdAt,
        createdByName = truck.createdBy,
        updatedAt = truck.updatedAt,
        updatedByName = truck.updatedBy
      )
    }
  }
}
