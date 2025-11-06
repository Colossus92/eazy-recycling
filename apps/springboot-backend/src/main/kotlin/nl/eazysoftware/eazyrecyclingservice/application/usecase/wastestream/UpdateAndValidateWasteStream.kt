package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamValidationResult
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalStateException

interface UpdateAndValidateWasteStream {
    fun handle(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand): WasteStreamValidationResult
}

@Service
class UpdateAndValidateWasteStreamService(
  private val updateWasteStream: UpdateWasteStream,
  private val validateWasteStream: ValidateWasteStream,
  private val wasteStreams: WasteStreams,
) : UpdateAndValidateWasteStream {

  @Transactional
    override fun handle(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand): WasteStreamValidationResult {
        updateWasteStream.handle(wasteStreamNumber, cmd)
        val validationResult = validateWasteStream.handle(ValidateWasteStreamCommand(wasteStreamNumber))

        if(validationResult.isValid) {
          val wasteStream = wasteStreams.findByNumber(wasteStreamNumber) ?: throw IllegalStateException("Fout bij ophalen van afvalstroomnummer ${wasteStreamNumber.number}")
          wasteStream.activate()
          wasteStreams.save(wasteStream)
        }

        return validationResult
    }
}
