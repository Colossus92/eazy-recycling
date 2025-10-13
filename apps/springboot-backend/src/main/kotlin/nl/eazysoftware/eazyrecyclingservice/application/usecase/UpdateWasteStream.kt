package nl.eazysoftware.eazyrecyclingservice.application.usecase

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UpdateWasteStream {
    fun handle(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand)
}

@Service
class UpdateWasteStreamService(
    private val wasteStreamRepo: WasteStreams,
) : UpdateWasteStream {

    @Transactional
    override fun handle(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand) {
        check(wasteStreamRepo.existsById(wasteStreamNumber)) {
            "Afvalstroom met nummer ${wasteStreamNumber.number} bestaat niet"
        }

        val wasteStream = WasteStream(
            wasteStreamNumber = wasteStreamNumber,
            wasteType = cmd.wasteType,
            collectionType = cmd.collectionType,
            pickupLocation = cmd.pickupLocation,
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
