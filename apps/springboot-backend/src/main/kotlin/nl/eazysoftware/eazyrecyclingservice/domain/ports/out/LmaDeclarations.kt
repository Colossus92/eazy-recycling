package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MaandelijkseOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface LmaDeclarations {
  fun saveAll(declarations: List<LmaDeclarationDto>): List<LmaDeclarationDto>
  fun findByIds(ids: List<String>): List<LmaDeclarationDto>
  fun saveAllPendingFirstReceivals(firstReceivals: List<EersteOntvangstMeldingDetails>)
  fun saveAllPendingMonthlyReceivals(monthlyReceivals: List<MaandelijkseOntvangstMeldingDetails>)
  fun findAll(pageable: Pageable): Page<LmaDeclaration>
}


data class LmaDeclaration(
  val wasteStreamNumber: WasteStreamNumber,
  val pickupLocation: Location,
  val wasteName: String,
  val totalWeight: Int,
  val totalTransports: Int,
  val period: YearMonth,
  val status: String,
  val errors: Array<String>?,
)
