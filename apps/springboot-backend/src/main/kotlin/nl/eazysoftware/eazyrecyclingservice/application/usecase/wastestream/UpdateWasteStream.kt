package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.service.CompanyService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UpdateWasteStream {
    fun handle(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand)
}

@Service
class UpdateWasteStreamService(
  private val wasteStreamRepo: WasteStreams,
  private val projectLocations: ProjectLocations,
  private val companyService: CompanyService,
) : UpdateWasteStream {

  @Transactional
    override fun handle(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand) {
        val wasteStream = wasteStreamRepo.findByNumber(wasteStreamNumber)
            ?: throw EntityNotFoundException("Afvalstroom met nummer ${wasteStreamNumber.number} bestaat niet")

        // Convert command to domain Location using LocationFactory
        val pickupLocation = cmd.pickupLocation.toDomain(companyService, projectLocations)

        wasteStream.update(
            wasteType = cmd.wasteType,
            collectionType = cmd.collectionType,
            pickupLocation = pickupLocation,
            deliveryLocation = cmd.deliveryLocation,
            consignorParty = cmd.consignorParty,
            pickupParty = cmd.pickupParty,
            dealerParty = cmd.dealerParty,
            collectorParty = cmd.collectorParty,
            brokerParty = cmd.brokerParty
        )

        wasteStreamRepo.save(wasteStream)
    }
}
