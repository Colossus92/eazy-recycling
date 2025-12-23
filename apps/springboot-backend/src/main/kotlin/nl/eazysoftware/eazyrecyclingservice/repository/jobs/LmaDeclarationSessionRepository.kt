package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MeldingSessieResponseDetails
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarationSessions
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*
import kotlin.time.Clock
import kotlin.time.toJavaInstant

interface LmaDeclarationSessionJpaRepository : JpaRepository<LmaDeclarationSessionDto, UUID> {
  fun findByStatus(status: LmaDeclarationSessionDto.Status): List<LmaDeclarationSessionDto>
}

@Repository
class LmaDeclarationSessionRepository(
  private val lmaDeclarationSessionJpaRepository: LmaDeclarationSessionJpaRepository,
) : LmaDeclarationSessions {
  override fun saveFirstReceivalSession(meldingSessieResponseDetails: MeldingSessieResponseDetails,  ids: List<String>) {
    lmaDeclarationSessionJpaRepository.save(
      LmaDeclarationSessionDto(
        id = UUID.fromString(meldingSessieResponseDetails.meldingSessieUUID),
        type = LmaDeclarationSessionDto.Type.FIRST_RECEIVAL,
        declarationIds = ids.map { it },
        status = LmaDeclarationSessionDto.Status.PENDING,
        createdAt = Clock.System.now().toJavaInstant(),
      )
    )
  }

  override fun saveMonthlyReceivalSession(meldingSessieResponseDetails: MeldingSessieResponseDetails,  ids: List<String>) {
    lmaDeclarationSessionJpaRepository.save(
      LmaDeclarationSessionDto(
        id = UUID.fromString(meldingSessieResponseDetails.meldingSessieUUID),
        type = LmaDeclarationSessionDto.Type.MONTHLY_RECEIVAL,
        declarationIds = ids.map { it },
        status = LmaDeclarationSessionDto.Status.PENDING,
        createdAt = Clock.System.now().toJavaInstant(),
      )
    )
  }

  override fun findPending(): List<LmaDeclarationSessionDto> {
    return lmaDeclarationSessionJpaRepository.findByStatus(LmaDeclarationSessionDto.Status.PENDING)
  }

  override fun save(session: LmaDeclarationSessionDto): LmaDeclarationSessionDto {
    return lmaDeclarationSessionJpaRepository.save(session)
  }
}
