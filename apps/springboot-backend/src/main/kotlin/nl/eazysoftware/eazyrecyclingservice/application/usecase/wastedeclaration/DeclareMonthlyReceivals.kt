package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration

import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MaandelijkseOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.AmiceSessions
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.time.Clock

interface DeclareMonthlyReceivals {
  fun declare(monthlyReceivalDeclarations: List<MonthlyReceivalDeclaration>)
}

data class MonthlyReceivalDeclaration(
  val id: String,
  val wasteStreamNumber: WasteStreamNumber,
  val transporters: List<String>,
  val totalWeight: Int,
  val totalShipments: Short,
  val yearMonth: YearMonth,
  val weightTicketIds: List<UUID>,
)

/**
 * Adapter for monthly receival declarations using the Amice Melding service.
 */
@Component
class DeclareMonthlyReceivalsService(
  private val amiceSessions: AmiceSessions,
  private val lmaDeclarations: LmaDeclarations,
  private val weightTickets: WeightTickets
) : DeclareMonthlyReceivals {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Transactional
  override fun declare(monthlyReceivalDeclarations: List<MonthlyReceivalDeclaration>) {
    logger.info("Declaring monthly receival for waste streams: ${monthlyReceivalDeclarations.joinToString(", ") { it.wasteStreamNumber.number }}")

    val message = monthlyReceivalDeclarations.map { mapToSoapMessage(it) }

    lmaDeclarations.saveAllPendingMonthlyReceivals(message)
    amiceSessions.declareMonthlyReceivals(message)

    // Mark weight ticket lines as declared - use the exact weight ticket IDs from each declaration
    val declaredAt = Clock.System.now()
    var totalUpdatedCount = 0
    monthlyReceivalDeclarations.forEach { declaration ->
      val updatedCount = weightTickets.markLinesAsDeclared(
        wasteStreamNumber = declaration.wasteStreamNumber,
        weightTicketIds = declaration.weightTicketIds,
        declaredAt = declaredAt
      )
      totalUpdatedCount += updatedCount
    }
    logger.info("Marked {} weight ticket lines as declared for monthly receivals", totalUpdatedCount)
  }

  private fun mapToSoapMessage(monthlyReceivalDeclaration: MonthlyReceivalDeclaration): MaandelijkseOntvangstMeldingDetails {
    val yearMonth = monthlyReceivalDeclaration.yearMonth
    val message = MaandelijkseOntvangstMeldingDetails().apply {
      meldingsNummerMelder = monthlyReceivalDeclaration.id
      afvalstroomNummer = monthlyReceivalDeclaration.wasteStreamNumber.number
      periodeMelding = "${yearMonth.month.number.toString().padStart(2, '0')}${yearMonth.year}"
      vervoerders = monthlyReceivalDeclaration.transporters.joinToString(",")
      totaalGewicht = monthlyReceivalDeclaration.totalWeight
      aantalVrachten = monthlyReceivalDeclaration.totalShipments
    }
    return message
  }
}
