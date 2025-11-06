package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamValidationResult
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CreateAndActivateWasteStream {
  fun handle(cmd: WasteStreamCommand): WasteStreamValidationResult
}

@Service
class CreateAndActivateWasteStreamService(
  private val wasteStreamFactory: WasteStreamFactory,
  private val validateWasteStream: ValidateWasteStream,
  private val wasteStreams: WasteStreams,
) : CreateAndActivateWasteStream {

  @Transactional
  override fun handle(cmd: WasteStreamCommand): WasteStreamValidationResult {
    val wasteStream = wasteStreamFactory.createDraft(cmd)
    val validationResult = validateWasteStream.handle(ValidateWasteStreamCommand(wasteStream))

    if (validationResult.isValid) {
      wasteStream.activate()
      wasteStreams.save(wasteStream)
    }


    return validationResult
  }
}
