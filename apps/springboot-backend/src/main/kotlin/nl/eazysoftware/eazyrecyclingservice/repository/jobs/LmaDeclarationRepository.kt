package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.toJavaInstant

interface LmaDeclarationJpaRepository : JpaRepository<LmaDeclarationDto, String>


@Repository
class LmaDeclarationRepository(
  private val jpaRepository: LmaDeclarationJpaRepository
) : LmaDeclarations {
  override fun saveAllPending(firstReceivals: List<EersteOntvangstMeldingDetails>) {
    LmaDeclarationMapper.map(firstReceivals, LmaDeclarationDto.Status.PENDING).apply { jpaRepository.saveAll(this) }
  }

  override fun findByIds(ids: List<String>): List<LmaDeclarationDto> {
    return jpaRepository.findAllById(ids)
  }

  override fun saveAll(declarations: List<LmaDeclarationDto>): List<LmaDeclarationDto> {
    return jpaRepository.saveAll(declarations)
  }
}


object LmaDeclarationMapper {

  fun map(
    firstReceivals: List<EersteOntvangstMeldingDetails>,
    status: LmaDeclarationDto.Status
  ) = firstReceivals.map { firstReceival ->
    LmaDeclarationDto(
      id = firstReceival.meldingsNummerMelder,
      wasteStreamNumber = firstReceival.afvalstroomNummer,
      period = firstReceival.periodeMelding,
      transporters = firstReceival.vervoerders.split(",").map { it.trim() },
      totalWeight = firstReceival.totaalGewicht.toLong(),
      totalShipments = firstReceival.aantalVrachten.toLong(),
      createdAt = Clock.System.now().toJavaInstant(),
      status = status,
    )
  }
}
