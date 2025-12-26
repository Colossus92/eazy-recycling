package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration

import jakarta.transaction.Transactional
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import nl.eazysoftware.eazyrecyclingservice.application.util.DeclarationCutoffDateCalculator
import nl.eazysoftware.eazyrecyclingservice.config.clock.toYearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.model.declaration.UndeclaredWeightTicketLine
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import kotlin.time.Clock
import kotlin.time.toJavaInstant

/**
 * Use case for detecting and creating late declaration jobs.
 *
 * Late declarations are weight ticket lines that were not declared during the normal
 * monthly declaration cycle (on the 20th of the following month).
 */
@Service
class DetectLateDeclarations(
  private val weightTickets: WeightTickets,
  private val lmaDeclarations: LmaDeclarations,
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * Detects undeclared weight ticket lines and creates late first receival declarations.
   *
   * First receivals are for waste streams that have never been declared before.
   */
  @Transactional
  fun detectAndCreateForLateWeightTickets() {
    val cutoffDate = DeclarationCutoffDateCalculator.calculateDeclarationCutoffDate(Clock.System.now())
    logger.info("Detecting late first receivals with cutoff date: {}", cutoffDate)

    val undeclaredLines = weightTickets.findUndeclaredLines(cutoffDate)

    if (undeclaredLines.isEmpty()) {
      logger.info("No undeclared weight ticket lines found for late first receivals")
      return
    }

    logger.info("Found {} undeclared weight ticket line(s) for late first receivals", undeclaredLines.size)

    // Group by waste stream number and period, then filter for first receivals
    val groupedByWasteStreamAndPeriod = groupByWasteStreamAndPeriod(undeclaredLines)

    groupedByWasteStreamAndPeriod.forEach { (key, lines) ->
      val (wasteStreamNumber, period) = key

      // Check if this waste stream has any prior declarations (if not, it's a first receival)
      val hasExistingDeclaration = lmaDeclarations.hasExistingDeclaration(wasteStreamNumber)
      val totalWeight = lines.sumOf { it.weightValue.toLong() }

      val formattedPeriod = formatPeriod(period)
      
      // Delete any existing pending late declarations for this waste stream and period
      lmaDeclarations.deletePendingLateDeclarations(wasteStreamNumber, formattedPeriod)
      
      if (!hasExistingDeclaration) {
        // This is a first receival - create a CORRECTIVE declaration for manual approval
        val declaration = LmaDeclarationDto(
          id = "LATE-FIRST-${UUID.randomUUID()}",
          wasteStreamNumber = wasteStreamNumber,
          period = formattedPeriod,
          transporters = emptyList(), // Transporters are not relevant for late declarations, transporters can only be declared once for a period
          totalWeight = totalWeight,
          totalShipments = lines.size.toLong(),
          createdAt = Clock.System.now().toJavaInstant(),
          status = LmaDeclarationDto.Status.WAITING_APPROVAL,
        )

        lmaDeclarations.saveCorrectiveDeclaration(declaration)
        logger.info("Created late first receival declaration for waste stream {} period {} (overwriting any previous pending)", wasteStreamNumber, period)
      } else {
        val declaration = LmaDeclarationDto(
          id = "LATE-MONTHLY-${UUID.randomUUID()}",
          wasteStreamNumber = wasteStreamNumber,
          period = formattedPeriod,
          transporters = emptyList(), // Transporters are not relevant for late declarations, transporters can only be declared once for a period
          totalWeight = totalWeight,
          totalShipments = lines.size.toLong(),
          createdAt = Clock.System.now().toJavaInstant(),
          status = LmaDeclarationDto.Status.WAITING_APPROVAL,
        )

        lmaDeclarations.saveCorrectiveDeclaration(declaration)
        logger.info("Created late monthly receival declaration for waste stream {} period {} (overwriting any previous pending)", wasteStreamNumber, period)
      }
    }
  }

  private fun groupByWasteStreamAndPeriod(
    lines: List<UndeclaredWeightTicketLine>
  ): Map<Pair<String, YearMonth>, List<UndeclaredWeightTicketLine>> {
    return lines.groupBy { line ->
      val period = line.weightedAt.toYearMonth()
      Pair(line.wasteStreamNumber.number, period)
    }
  }

  private fun formatPeriod(yearMonth: YearMonth): String {
    return "${yearMonth.month.number.toString().padStart(2, '0')}${yearMonth.year}"
  }
}
