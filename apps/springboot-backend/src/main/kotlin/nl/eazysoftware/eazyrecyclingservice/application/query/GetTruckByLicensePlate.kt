package nl.eazysoftware.eazyrecyclingservice.application.query

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Trucks
import org.springframework.stereotype.Service

interface GetTruckByLicensePlate {
  fun handle(licensePlate: LicensePlate): TruckView
}

@Service
class GetTruckByLicensePlateQuery(
  private val trucks: Trucks,
  private val companies: Companies
) : GetTruckByLicensePlate {

  override fun handle(licensePlate: LicensePlate): TruckView {
    val truck = trucks.findByLicensePlate(licensePlate)
      ?: throw EntityNotFoundException("Vrachtwagen met kenteken ${licensePlate.value} niet gevonden")

    // Optionally fetch company name if carrierPartyId is present
    val companyName = truck.carrierPartyId?.let { companyId ->
      companies.findById(companyId)?.name
    }

    return TruckView(
      licensePlate = truck.licensePlate.value,
      brand = truck.brand,
      model = truck.description,
      carrierCompanyId = truck.carrierPartyId?.uuid?.toString(),
      carrierCompanyName = companyName,
      updatedAt = truck.updatedAt?.toLocalDateTime() ?: java.time.LocalDateTime.now()
    )
  }
}
