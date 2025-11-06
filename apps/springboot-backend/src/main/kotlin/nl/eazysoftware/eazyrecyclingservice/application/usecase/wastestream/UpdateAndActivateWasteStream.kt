package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamValidationResult
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UpdateAndActivateWasteStream {
    fun handle(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand): WasteStreamValidationResult
}

@Service
class UpdateAndActivateWasteStreamService(
  private val validateWasteStream: ValidateWasteStream,
  private val wasteStreams: WasteStreams,
  private val wasteStreamFactory: WasteStreamFactory,
) : UpdateAndActivateWasteStream {

  @Transactional
    override fun handle(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand): WasteStreamValidationResult {
        val updatedWasteStream = wasteStreamFactory.updateExisting(wasteStreamNumber, cmd)
        val validationResult = validateWasteStream.handle(ValidateWasteStreamCommand(updatedWasteStream))

        if(validationResult.isValid) {
          updatedWasteStream.activate()
          wasteStreams.save(updatedWasteStream)
        }

        return validationResult
    }
}
