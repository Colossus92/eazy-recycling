package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.FirstReceivalDeclaration
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.MonthlyReceivalDeclaration
import nl.eazysoftware.eazyrecyclingservice.config.clock.toLmaPeriod
import nl.eazysoftware.eazyrecyclingservice.config.clock.toYearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclaration
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.toJavaInstant

interface LmaDeclarationJpaRepository : JpaRepository<LmaDeclarationDto, String> {
  fun deleteByIdIn(ids: List<String>)
}

@Repository
class LmaDeclarationRepository(
  private val jpaRepository: LmaDeclarationJpaRepository,
  private val entityManager: EntityManager,
  private val pickupLocationMapper: PickupLocationMapper,
) : LmaDeclarations {

  override fun save(declaration: LmaDeclarationDto): LmaDeclarationDto {
    return jpaRepository.save(declaration)
  }

  override fun saveAllPendingFirstReceivals(firstReceivals: List<FirstReceivalDeclaration>) {
    val declarations = LmaDeclarationMapper.mapFirstReceivals(firstReceivals, LmaDeclarationDto.Status.PENDING)
    jpaRepository.saveAll(declarations)
    touchWasteStreams(declarations.map { it.wasteStreamNumber })
  }

  override fun saveAllPendingMonthlyReceivals(monthlyReceivals: List<MonthlyReceivalDeclaration>) {
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
      "SELECT COUNT(*) FROM lma_declarations WHERE waste_stream_number = :wasteStreamNumber AND status = 'COMPLETED'"
    )
      .setParameter("wasteStreamNumber", wasteStreamNumber)
    return (query.singleResult as Number).toLong() > 0
  }

  override fun deletePendingLateDeclarations(wasteStreamNumber: String, period: String) {
    val query = entityManager.createNativeQuery(
      """
        SELECT id FROM lma_declarations
        WHERE waste_stream_number = :wasteStreamNumber
        AND period = :period
        AND status = '${LmaDeclarationDto.Status.WAITING_APPROVAL.name}'
      """.trimIndent()
    )
      .setParameter("wasteStreamNumber", wasteStreamNumber)
      .setParameter("period", period)

    @Suppress("UNCHECKED_CAST")
    val ids = query.resultList as List<String>

    if (ids.isNotEmpty()) {
      jpaRepository.deleteByIdIn(ids)
    }
  }

  override fun hasDeclarationsWithStatus(status: String): Boolean {
    val query = entityManager.createNativeQuery(
      "SELECT COUNT(*) FROM lma_declarations WHERE status = :status"
    )
      .setParameter("status", status)
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
        d.type,
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
      val yearMonth = result.period.toYearMonth()

      // Fetch pickup location if available
      val pickupLocation = if (result.pickupLocationId != null) {
        val pickupLocationDto = entityManager.find(
          PickupLocationDto::class.java,
          result.pickupLocationId
        )
        pickupLocationDto?.let { pickupLocationMapper.toDomain(it) }
          ?: Location.NoLocation
      } else {
        Location.NoLocation
      }

      LmaDeclaration(
        id = result.id,
        wasteStreamNumber = WasteStreamNumber(result.wasteStreamNumber),
        pickupLocation = pickupLocation,
        wasteName = result.wasteName ?: "Unknown",
        totalWeight = result.totalWeight.toInt(),
        totalTransports = result.totalShipments.toInt(),
        period = yearMonth,
        type = LmaDeclaration.Type.valueOf(result.type),
        status = result.status,
        errors = result.errors,
        transporters = result.transporters?.toList() ?: emptyList(),
      )
    }

    return PageImpl(declarations, pageable, total)
  }
}


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
  val type: String,
  val transporters: Array<String>?,
)

object LmaDeclarationMapper {

  fun mapFirstReceivals(
    firstReceivals: List<FirstReceivalDeclaration>,
    status: LmaDeclarationDto.Status
  ) = firstReceivals.map { declaration ->
    LmaDeclarationDto(
      id = declaration.id,
      wasteStreamNumber = declaration.wasteStream.wasteStreamNumber.number,
      period = declaration.yearMonth.toLmaPeriod(),
      transporters = declaration.transporters,
      totalWeight = declaration.totalWeight.toLong(),
      totalShipments = declaration.totalShipments.toLong(),
      type = LmaDeclaration.Type.FIRST_RECEIVAL,
      createdAt = Clock.System.now().toJavaInstant(),
      status = status,
      weightTicketIds = declaration.weightTicketIds,
    )
  }

  fun mapMonthlyReceivals(
    monthlyReceivals: List<MonthlyReceivalDeclaration>,
    status: LmaDeclarationDto.Status
  ) = monthlyReceivals.map { declaration ->
    LmaDeclarationDto(
      id = declaration.id,
      wasteStreamNumber = declaration.wasteStreamNumber.number,
      period = declaration.yearMonth.toLmaPeriod(),
      transporters = declaration.transporters,
      totalWeight = declaration.totalWeight.toLong(),
      totalShipments = declaration.totalShipments.toLong(),
      type = LmaDeclaration.Type.MONTHLY_RECEIVAL,
      createdAt = Clock.System.now().toJavaInstant(),
      status = status,
      weightTicketIds = declaration.weightTicketIds,
    )
  }
}
