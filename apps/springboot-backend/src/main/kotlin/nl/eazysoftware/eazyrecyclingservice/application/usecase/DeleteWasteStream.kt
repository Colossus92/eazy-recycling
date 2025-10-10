package nl.eazysoftware.eazyrecyclingservice.application.usecase

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface DeleteWasteStream {
    fun handle(cmd: DeleteWasteStreamCommand)
}

data class DeleteWasteStreamCommand(
    val wasteStreamNumber: WasteStreamNumber
)

@Service
class DeleteWasteStreamService(
    private val wasteStreamRepo: WasteStreams
) : DeleteWasteStream {

    @Transactional
    override fun handle(cmd: DeleteWasteStreamCommand) {
        check(wasteStreamRepo.existsById(cmd.wasteStreamNumber)) {
            "Afvalstroom met nummer ${cmd.wasteStreamNumber.number} bestaat niet"
        }

        wasteStreamRepo.deleteByNumber(cmd.wasteStreamNumber)
    }
}
