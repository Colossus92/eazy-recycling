package nl.eazysoftware.eazyrecyclingservice.repository.declaration

import jakarta.persistence.EntityManager
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import nl.eazysoftware.eazyrecyclingservice.domain.model.declaration.UndeclaredWeightTicketLine
import nl.eazysoftware.eazyrecyclingservice.domain.model.declaration.WeightTicketDeclarationSnapshot
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTicketDeclarationSnapshots
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

interface WeightTicketDeclarationSnapshotJpaRepository : JpaRepository<WeightTicketDeclarationSnapshotDto, Long> {

  @Query("""
    SELECT s FROM WeightTicketDeclarationSnapshotDto s
    WHERE s.weightTicketId = :weightTicketId
    AND s.weightTicketLineIndex = :lineIndex
    ORDER BY s.declaredAt DESC
    LIMIT 1
  """)
  fun findLatestByWeightTicketLine(weightTicketId: Long, lineIndex: Int): WeightTicketDeclarationSnapshotDto?
}

@Repository
class WeightTicketDeclarationSnapshotRepository(
  private val jpaRepository: WeightTicketDeclarationSnapshotJpaRepository,
  private val entityManager: EntityManager,
) : WeightTicketDeclarationSnapshots {

  override fun save(snapshot: WeightTicketDeclarationSnapshot) {
    jpaRepository.save(snapshot.toDto())
  }

  override fun saveAll(snapshots: List<WeightTicketDeclarationSnapshot>) {
    jpaRepository.saveAll(snapshots.map { it.toDto() })
  }

  override fun findLatestByWeightTicketLine(weightTicketId: Long, lineIndex: Int): WeightTicketDeclarationSnapshot? {
    return jpaRepository.findLatestByWeightTicketLine(weightTicketId, lineIndex)?.toDomain()
  }

  override fun findUndeclaredLines(cutoffDate: YearMonth): List<UndeclaredWeightTicketLine> {
    // Find weight ticket lines that have not been declared yet
    // Only includes tickets from months that have passed their declaration deadline
    val query = """
      SELECT
        wt.id as weight_ticket_id,
        row_number() OVER (PARTITION BY wt.id ORDER BY wtl.waste_stream_number) - 1 as line_index,
        wtl.waste_stream_number,
        wtl.weight_value,
        wt.weighted_at
      FROM weight_ticket_lines wtl
      JOIN weight_tickets wt ON wt.id = wtl.weight_ticket_id
      LEFT JOIN weight_ticket_declaration_snapshots snap
        ON snap.weight_ticket_id = wt.id
        AND snap.weight_ticket_line_index = (
          SELECT COUNT(*) - 1
          FROM weight_ticket_lines wtl2
          WHERE wtl2.weight_ticket_id = wt.id
          AND wtl2.waste_stream_number <= wtl.waste_stream_number
        )
        AND snap.waste_stream_number = wtl.waste_stream_number
      WHERE wt.status IN ('COMPLETED', 'INVOICED')
        AND wt.weighted_at < :cutoffDate
        AND snap.id IS NULL
      ORDER BY wt.weighted_at, wt.id
    """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val results = entityManager.createNativeQuery(query)
      .setParameter("cutoffDate", java.time.YearMonth.of(cutoffDate.year, cutoffDate.month.number).atDay(1).atStartOfDay())
      .resultList as List<Array<Any>>

    return results.map { row ->
      UndeclaredWeightTicketLine(
        weightTicketId = WeightTicketId((row[0] as Number).toLong()),
        weightTicketLineIndex = (row[1] as Number).toInt(),
        wasteStreamNumber = WasteStreamNumber(row[2] as String),
        weightValue = row[3] as BigDecimal,
        weightedAt = (row[4] as java.time.Instant).toKotlinInstant(),
      )
    }
  }

  private fun WeightTicketDeclarationSnapshot.toDto(): WeightTicketDeclarationSnapshotDto {
    val period = String.format("%02d%04d", declarationPeriod.month.number, declarationPeriod.year)
    return WeightTicketDeclarationSnapshotDto(
      id = if (id == 0L) 0 else id,
      weightTicketId = weightTicketId.number,
      weightTicketLineIndex = weightTicketLineIndex,
      wasteStreamNumber = wasteStreamNumber.number,
      declaredWeightValue = declaredWeightValue,
      declarationId = declarationId,
      declaredAt = declaredAt.toJavaInstant(),
      declarationPeriod = period,
    )
  }

  private fun WeightTicketDeclarationSnapshotDto.toDomain(): WeightTicketDeclarationSnapshot {
    val month = declarationPeriod.take(2).toInt()
    val year = declarationPeriod.substring(2).toInt()
    return WeightTicketDeclarationSnapshot(
      id = id,
      weightTicketId = WeightTicketId(weightTicketId),
      weightTicketLineIndex = weightTicketLineIndex,
      wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
      declaredWeightValue = declaredWeightValue,
      declarationId = declarationId,
      declaredAt = declaredAt.toKotlinInstant(),
      declarationPeriod = YearMonth(year, month),
    )
  }
}
