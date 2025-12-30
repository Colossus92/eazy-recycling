package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.FirstReceivalDeclaration
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ReceivalDeclarationIdGenerator
import org.springframework.stereotype.Component
import java.util.*

@Component
class ReceivalDeclarationFactory(
  private val idGenerator: ReceivalDeclarationIdGenerator
) {

  fun create(
    wasteStream: WasteStream,
    transporters: List<String>,
    totalWeight: Int,
    totalShipments: Short,
    yearMonth: YearMonth,
    weightTicketIds: List<UUID>,
  ): FirstReceivalDeclaration {
    val id = idGenerator.nextId()
    return FirstReceivalDeclaration(
      id = id,
      wasteStream = wasteStream,
      transporters = transporters,
      totalWeight = totalWeight,
      totalShipments = totalShipments,
      yearMonth = yearMonth,
      weightTicketIds = weightTicketIds,
    )
  }
}
