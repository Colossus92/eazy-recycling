package nl.eazysoftware.eazyrecyclingservice.application.usecase

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UpdateWasteStream {
    fun handle(cmd: WasteStreamCommand)
}

@Service
class UpdateWasteStreamService(
    private val wasteStreamRepo: WasteStreams,
    private val wasteStreamMapper: WasteStreamMapper
) : UpdateWasteStream {

    @Transactional
    override fun handle(cmd: WasteStreamCommand) {
        check(wasteStreamRepo.existsById(cmd.wasteStreamNumber)) {
            "Afvalstroom met nummer ${cmd.wasteStreamNumber.number} bestaat niet"
        }

        val wasteStream = WasteStream(
            wasteStreamNumber = cmd.wasteStreamNumber,
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
