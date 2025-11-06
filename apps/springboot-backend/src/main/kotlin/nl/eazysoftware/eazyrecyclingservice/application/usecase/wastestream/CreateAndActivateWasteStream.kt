package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamValidationResult
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalStateException

interface CreateAndActivateWasteStream {
  fun handle(cmd: WasteStreamCommand): WasteStreamValidationResult
}

@Service
class CreateAndActivateWasteStreamService(
  private val createDraftWasteStream: CreateDraftWasteStream,
  private val validateWasteStream: ValidateWasteStream,
  private val wasteStreams: WasteStreams,
) : CreateAndActivateWasteStream {

  @Transactional
  override fun handle(cmd: WasteStreamCommand): WasteStreamValidationResult {
    val wasteStreamResult = createDraftWasteStream.handle(cmd)
    val validationResult = validateWasteStream.handle(ValidateWasteStreamCommand(wasteStreamResult.wasteStreamNumber))

    if (validationResult.isValid) {
      val wasteStream = wasteStreams.findByNumber(wasteStreamResult.wasteStreamNumber) ?: throw IllegalStateException("Fout bij ophalen van afvalstroomnummer ${wasteStreamResult.wasteStreamNumber.number}")
      wasteStream.activate()
      wasteStreams.save(wasteStream)
    }


    return validationResult
  }
}
