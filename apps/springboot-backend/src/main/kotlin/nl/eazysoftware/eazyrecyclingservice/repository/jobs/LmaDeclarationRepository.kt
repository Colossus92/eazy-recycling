package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.EntityManager
import jakarta.persistence.SqlResultSetMapping
import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MaandelijkseOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclaration
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.toJavaInstant

interface LmaDeclarationJpaRepository : JpaRepository<LmaDeclarationDto, String>

@Repository
class LmaDeclarationRepository(
  private val jpaRepository: LmaDeclarationJpaRepository,
  private val entityManager: EntityManager,
  private val pickupLocationMapper: PickupLocationMapper,
) : LmaDeclarations {

  override fun save(declaration: LmaDeclarationDto): LmaDeclarationDto {
    return jpaRepository.save(declaration)
  }

  override fun saveAllPendingFirstReceivals(firstReceivals: List<EersteOntvangstMeldingDetails>) {
    val declarations = LmaDeclarationMapper.mapFirstReceivals(firstReceivals, LmaDeclarationDto.Status.PENDING)
    jpaRepository.saveAll(declarations)
    touchWasteStreams(declarations.map { it.wasteStreamNumber })
  }

  override fun saveAllPendingMonthlyReceivals(monthlyReceivals: List<MaandelijkseOntvangstMeldingDetails>) {
    val declarations = LmaDeclarationMapper.mapMonthlyReceivals(monthlyReceivals, LmaDeclarationDto.Status.PENDING)
    jpaRepository.saveAll(declarations)
    touchWasteStreams(declarations.map { it.wasteStreamNumber })
  }

  /**
   * Updates the last_modified_at timestamp for waste streams to track activity.
   * This is required for [nl.eazysoftware.eazyrecyclingservice.domain.model.waste.EffectiveStatusPolicy]
   * to correctly determine if a waste stream is expired.
   */
  private fun touchWasteStreams(wasteStreamNumbers: List<String>) {
    if (wasteStreamNumbers.isEmpty()) return
    entityManager.createNativeQuery(
      "UPDATE waste_streams SET last_modified_at = :now WHERE number IN :numbers"
    )
      .setParameter("now", Clock.System.now().toJavaInstant())
      .setParameter("numbers", wasteStreamNumbers)
      .executeUpdate()
  }

  override fun findByIds(ids: List<String>): List<LmaDeclarationDto> {
    return jpaRepository.findAllById(ids)
  }

  override fun findById(id: String): LmaDeclarationDto? {
    return jpaRepository.findById(id).orElse(null)
  }

  override fun saveAll(declarations: List<LmaDeclarationDto>): List<LmaDeclarationDto> {
    return jpaRepository.saveAll(declarations)
  }

  override fun saveCorrectiveDeclaration(declaration: LmaDeclarationDto): LmaDeclarationDto {
    val saved = jpaRepository.save(declaration)
    touchWasteStreams(listOf(saved.wasteStreamNumber))
    return saved
  }

  override fun hasExistingDeclaration(wasteStreamNumber: String): Boolean {
    val query = entityManager.createNativeQuery(
      "SELECT COUNT(*) FROM lma_declarations WHERE waste_stream_number = :wasteStreamNumber"
    )
      .setParameter("wasteStreamNumber", wasteStreamNumber)
    return (query.singleResult as Number).toLong() > 0
  }

  override fun findAll(pageable: Pageable): Page<LmaDeclaration> {
    // Count query for pagination
    val countQuery = entityManager.createNativeQuery(
      "SELECT COUNT(*) FROM lma_declarations"
    )
    val total = (countQuery.singleResult as Number).toLong()

    // Main query with joins to get waste stream name and pickup location
    val query = """
      SELECT
        d.id,
        d.waste_stream_number,
        d.period,
        d.total_weight,
        d.total_shipments,
        d.status,
        ws.name as waste_name,
        ws.pickup_location_id,
        d.errors,
        d.transporters
      FROM lma_declarations d
      LEFT JOIN waste_streams ws ON d.waste_stream_number = ws.number
      ORDER BY d.created_at DESC
      LIMIT :limit OFFSET :offset
    """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val results = entityManager.createNativeQuery(
      query,
      LmaDeclarationQueryResult::class.java
    )
      .setParameter("limit", pageable.pageSize)
      .setParameter("offset", pageable.offset)
      .resultList as List<LmaDeclarationQueryResult>

    val declarations = results.map { result ->
      // Parse period from MMYYYY format to YearMonth
      val yearMonth = parsePeriod(result.period)

      // Fetch pickup location if available
      val pickupLocation = if (result.pickupLocationId != null) {
        val pickupLocationDto = entityManager.find(
          nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto::class.java,
          result.pickupLocationId
        )
        pickupLocationDto?.let { pickupLocationMapper.toDomain(it) }
          ?: nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.NoLocation
      } else {
        nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.NoLocation
      }

      LmaDeclaration(
        id = result.id,
        wasteStreamNumber = WasteStreamNumber(result.wasteStreamNumber),
        pickupLocation = pickupLocation,
        wasteName = result.wasteName ?: "Unknown",
        totalWeight = result.totalWeight.toInt(),
        totalTransports = result.totalShipments.toInt(),
        period = yearMonth,
        status = result.status,
        errors = result.errors,
        transporters = result.transporters?.toList() ?: emptyList(),
      )
    }

    return PageImpl(declarations, pageable, total)
  }

  private fun parsePeriod(period: String): YearMonth {
    // Period format is MMYYYY (e.g., "112025" for November 2025)
    val month = period.take(2).toInt()
    val year = period.substring(2).toInt()
    return YearMonth(year, month)
  }
}


/**
 * Result type for LMA declaration queries.
 * Maps native SQL query results to typed properties.
 */
@SqlResultSetMapping(
  name = "LmaDeclarationQueryResultMapping",
  classes = [
    ConstructorResult(
      targetClass = LmaDeclarationQueryResult::class,
      columns = [
        ColumnResult(name = "id", type = String::class),
        ColumnResult(name = "waste_stream_number", type = String::class),
        ColumnResult(name = "period", type = String::class),
        ColumnResult(name = "total_weight", type = Long::class),
        ColumnResult(name = "total_shipments", type = Long::class),
        ColumnResult(name = "status", type = String::class),
        ColumnResult(name = "waste_name", type = String::class),
        ColumnResult(name = "pickup_location_id", type = String::class),
        ColumnResult(name = "errors", type = Array<String>::class),
        ColumnResult(name = "transporters", type = Array<String>::class)
      ]
    )
  ]
)
data class LmaDeclarationQueryResult(
  val id: String,
  val wasteStreamNumber: String,
  val period: String,
  val totalWeight: Long,
  val totalShipments: Long,
  val status: String,
  val wasteName: String?,
  val pickupLocationId: String?,
  val errors: Array<String>?,
  val transporters: Array<String>?,
)

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
