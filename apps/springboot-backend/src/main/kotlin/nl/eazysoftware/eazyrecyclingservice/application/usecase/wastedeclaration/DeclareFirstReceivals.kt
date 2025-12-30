package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.AmiceSessions
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.time.Clock

interface DeclareFirstReceivals {

  fun declareFirstReceivals(firstReceivalDeclarations: List<FirstReceivalDeclaration>)
}

data class FirstReceivalDeclaration(
  val id: String,
  val wasteStream: WasteStream,
  val transporters: List<String>,
  val totalWeight: Int,
  val totalShipments: Short,
  val yearMonth: YearMonth,
  val weightTicketIds: List<UUID>,
)

/**
 * Adapter for first receival declarations using the Amice Melding service.
 */
@Component
class DeclareFirstReceivalsService(
  private val amiceSessions: AmiceSessions,
  private val lmaDeclarations: LmaDeclarations,
  private val weightTickets: WeightTickets,
  private val firstReceivalMessageMapper: FirstReceivalMessageMapper
): DeclareFirstReceivals {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Transactional
  override fun declareFirstReceivals(
    firstReceivalDeclarations: List<FirstReceivalDeclaration>
  ) {
    logger.info("Declaring first receival for waste streams: ${firstReceivalDeclarations.joinToString(", ") { it.wasteStream.wasteStreamNumber.number }}")

    val messages = firstReceivalDeclarations.map { declaration ->
      firstReceivalMessageMapper.mapToSoapMessage(
        declarationId = declaration.id,
        wasteStream = declaration.wasteStream,
        transporters = declaration.transporters,
        totalWeight = declaration.totalWeight,
        totalShipments = declaration.totalShipments,
        yearMonth = declaration.yearMonth
      )
    }

    lmaDeclarations.saveAllPendingFirstReceivals(messages)
    amiceSessions.declareFirstReceivals(messages)

    // Mark weight ticket lines as declared - use the exact weight ticket IDs from each declaration
    val declaredAt = Clock.System.now()
    var totalUpdatedCount = 0
    firstReceivalDeclarations.forEach { declaration ->
      val updatedCount = weightTickets.markLinesAsDeclared(
        wasteStreamNumber = declaration.wasteStream.wasteStreamNumber,
        weightTicketIds = declaration.weightTicketIds,
        declaredAt = declaredAt
      )
      totalUpdatedCount += updatedCount
    }
    logger.info("Marked {} weight ticket lines as declared for first receivals", totalUpdatedCount)
  }

}
