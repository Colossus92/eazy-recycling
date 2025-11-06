package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UpdateWasteStream {
    fun handle(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand)
}

@Service
class UpdateWasteStreamService(
  private val wasteStreams: WasteStreams,
  private val wasteStreamFactory: WasteStreamFactory,
) : UpdateWasteStream {

  @Transactional
    override fun handle(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand) {
        val updatedWasteStream = wasteStreamFactory.updateExisting(wasteStreamNumber, cmd)

        wasteStreams.save(updatedWasteStream)
    }
}
