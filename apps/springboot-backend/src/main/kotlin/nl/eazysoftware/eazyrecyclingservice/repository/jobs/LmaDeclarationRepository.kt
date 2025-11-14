package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MaandelijkseOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.toJavaInstant

interface LmaDeclarationJpaRepository : JpaRepository<LmaDeclarationDto, String>
interface LmaDeclarationPageableRepository : PagingAndSortingRepository<LmaDeclarationDto, String>

@Repository
class LmaDeclarationRepository(
  private val jpaRepository: LmaDeclarationJpaRepository,
  private val pageableRepository: LmaDeclarationPageableRepository,
) : LmaDeclarations {
  override fun saveAllPendingFirstReceivals(firstReceivals: List<EersteOntvangstMeldingDetails>) {
    LmaDeclarationMapper.mapFirstReceivals(firstReceivals, LmaDeclarationDto.Status.PENDING).apply { jpaRepository.saveAll(this) }
  }

  override fun saveAllPendingMonthlyReceivals(monthlyReceivals: List<MaandelijkseOntvangstMeldingDetails>) {
    LmaDeclarationMapper.mapMonthlyReceivals(monthlyReceivals, LmaDeclarationDto.Status.PENDING).apply { jpaRepository.saveAll(this) }
  }

  override fun findByIds(ids: List<String>): List<LmaDeclarationDto> {
    return jpaRepository.findAllById(ids)
  }

  override fun saveAll(declarations: List<LmaDeclarationDto>): List<LmaDeclarationDto> {
    return jpaRepository.saveAll(declarations)
  }

  override fun findAll(pageable: Pageable): Page<LmaDeclarationDto?> {
    return pageableRepository.findAll(pageable)
  }
}


object LmaDeclarationMapper {

  fun mapFirstReceivals(
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

  fun mapMonthlyReceivals(
    monthlyReceivals: List<MaandelijkseOntvangstMeldingDetails>,
    status: LmaDeclarationDto.Status,
  ) = monthlyReceivals.map { monthlyReceival ->
    LmaDeclarationDto(
      id = monthlyReceival.meldingsNummerMelder,
      wasteStreamNumber = monthlyReceival.afvalstroomNummer,
      period = monthlyReceival.periodeMelding,
      transporters = monthlyReceival.vervoerders.split(",").map { it.trim() },
      totalWeight = monthlyReceival.totaalGewicht.toLong(),
      totalShipments = monthlyReceival.aantalVrachten.toLong(),
      createdAt = Clock.System.now().toJavaInstant(),
      status = status,
    )
  }
}
