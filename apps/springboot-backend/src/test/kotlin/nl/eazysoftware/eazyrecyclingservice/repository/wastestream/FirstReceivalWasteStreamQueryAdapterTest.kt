package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketDirection
import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Integration test for FirstReceivalWasteStreamQueryAdapter using PostgreSQL Testcontainers.
 *
 * Tests the complete query logic including:
 * - Filtering by processor_id '08797'
 * - Time-based filtering (month boundaries)
 * - Aggregation of weight from weight_ticket_lines
 * - Aggregation of shipments from weight_tickets
 * - Collection of unique transporters
 * - Exclusion of already declared waste streams
 * - Exclusion of cancelled weight tickets
 */
@SpringBootTest
@ActiveProfiles("test")
class FirstReceivalWasteStreamQueryAdapterTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var queryAdapter: FirstReceivalWasteStreamQueryAdapter

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var companyRepository: CompanyJpaRepository

  @Autowired
  private lateinit var wasteStreamRepository: WasteStreamJpaRepository

  @Autowired
  private lateinit var weightTicketRepository: WeightTicketJpaRepository

  @Autowired
  private lateinit var pickupLocationRepository: PickupLocationRepository

  @Autowired
  private lateinit var euralRepository: EuralRepository

  @Autowired
  private lateinit var processingMethodRepository: ProcessingMethodRepository

  private lateinit var processorCompanyId: UUID
  private lateinit var consignorCompanyId: UUID
  private lateinit var carrierCompanyId1: UUID
  private lateinit var carrierCompanyId2: UUID
  private lateinit var pickupLocationId: String

  @BeforeEach
  fun setup() {
    cleanupDatabase()
    setupTestData()
  }

  @Test
  fun `should find waste streams with weight tickets in the specified month`() {
    // Given - a waste stream with weight tickets in November 2025
    val wasteStreamNumber = "087970000001"
    val yearMonth = YearMonth(2025, 11)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
      carrierPartyId = carrierCompanyId1,
      weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 1000.0)
    )
    createWeightTicket(
      carrierPartyId = carrierCompanyId2,
      weightedAt = OffsetDateTime.of(2025, 11, 20, 14, 30, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 1500.0)
    )

    // When
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then
    assertThat(results).hasSize(1)
    val declaration = results.first()
    assertThat(declaration.wasteStream.wasteStreamNumber.number).isEqualTo(wasteStreamNumber)
    assertThat(declaration.totalWeight).isEqualTo(2500)
    assertThat(declaration.totalShipments).isEqualTo(2)
    assertThat(declaration.transporters).hasSize(2)
    assertThat(declaration.transporters).containsExactlyInAnyOrder("VIHB001", "VIHB002")
    assertThat(declaration.yearMonth).isEqualTo(yearMonth)
  }

  @Test
  fun `should exclude waste streams with weight tickets outside the specified month`() {
    // Given - a waste stream with weight tickets in October (not November)
    val wasteStreamNumber = "087970000002"
    val yearMonth = YearMonth(2025, 11)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
      carrierPartyId = carrierCompanyId1,
      weightedAt = OffsetDateTime.of(2025, 10, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 1000.0)
    )

    // When
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then
    assertThat(results).isEmpty()
  }

  @Test
  fun `should exclude waste streams that already have declarations`() {
    // Given - a waste stream with weight tickets AND an existing declaration
    val wasteStreamNumber = "087970000003"
    val yearMonth = YearMonth(2025, 11)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
      carrierPartyId = carrierCompanyId1,
      weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 1000.0)
    )

    // Create an existing declaration
    createLmaDeclaration(wasteStreamNumber, "2025-11")

    // When
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then
    assertThat(results).isEmpty()
  }

  @Test
  fun `should exclude cancelled weight tickets from aggregation`() {
    // Given - a waste stream with one valid and one cancelled weight ticket
    val wasteStreamNumber = "087970000004"
    val yearMonth = YearMonth(2025, 11)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
      carrierPartyId = carrierCompanyId1,
      weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      status = "COMPLETED",
      lines = listOf(wasteStreamNumber to 1000.0)
    )
    createWeightTicket(
      carrierPartyId = carrierCompanyId2,
      weightedAt = OffsetDateTime.of(2025, 11, 20, 14, 30, 0, 0, ZoneOffset.UTC),
      status = "CANCELLED",
      lines = listOf(wasteStreamNumber to 5000.0) // Should be excluded
    )

    // When
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then
    assertThat(results).hasSize(1)
    val declaration = results.first()
    assertThat(declaration.totalWeight).isEqualTo(1000) // Only the valid ticket
    assertThat(declaration.totalShipments).isEqualTo(1)
    assertThat(declaration.transporters).containsExactly("VIHB001")
  }

  @Test
  fun `should only include waste streams for processor 08797`() {
    // Given - waste streams for different processors
    val wasteStreamOurs = "087970000005"
    val wasteStreamOthers = "999990000001"
    val yearMonth = YearMonth(2025, 11)

    // Create a different processor company
    val otherProcessorId = createCompany("Other Processor", "99999", "VIHB999", "34234321")

    createWasteStream(wasteStreamOurs, "ACTIVE")
    createWasteStream(wasteStreamOthers, "ACTIVE", processorParty = entityManager.getReference(CompanyDto::class.java, otherProcessorId))

    createWeightTicket(
      carrierPartyId = carrierCompanyId1,
      weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamOurs to 1000.0)
    )
    createWeightTicket(
      carrierPartyId = carrierCompanyId1,
      weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamOthers to 2000.0)
    )

    // When
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then
    assertThat(results).hasSize(1)
    assertThat(results.first().wasteStream.wasteStreamNumber.number).isEqualTo(wasteStreamOurs)
  }

  @Test
  fun `should aggregate multiple weight ticket lines for the same waste stream`() {
    // Given - a waste stream with multiple weight tickets, each with multiple lines
    val wasteStreamNumber = "087970000006"
    val yearMonth = YearMonth(2025, 11)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
      carrierPartyId = carrierCompanyId1,
      weightedAt = OffsetDateTime.of(2025, 11, 10, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 500.0, wasteStreamNumber to 300.0)
    )
    createWeightTicket(
      carrierPartyId = carrierCompanyId1,
      weightedAt = OffsetDateTime.of(2025, 11, 20, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 700.0, wasteStreamNumber to 1000.0)
    )

    // When
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then
    assertThat(results).hasSize(1)
    val declaration = results.first()
    assertThat(declaration.totalWeight).isEqualTo(2500) // Sum of all lines
    assertThat(declaration.totalShipments).isEqualTo(2) // Two distinct tickets
  }

  @Test
  fun `should handle month boundary correctly for December`() {
    // Given - weight tickets in December 2025
    val wasteStreamNumber = "087970000007"
    val yearMonth = YearMonth(2025, 12)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
      carrierPartyId = carrierCompanyId1,
      weightedAt = OffsetDateTime.of(2025, 12, 31, 23, 59, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 1000.0)
    )

    // When
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then
    assertThat(results).hasSize(1)
  }

  @Test
  fun `should return empty list when no waste streams match criteria`() {
    // Given - no matching data
    val yearMonth = YearMonth(2025, 11)

    // When
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then
    assertThat(results).isEmpty()
  }

  @Test
  fun `should handle waste streams with no transporters`() {
    // Given - a waste stream with weight tickets but carrier has no VIHB ID
    val wasteStreamNumber = "087970000008"
    val yearMonth = YearMonth(2025, 11)
    val carrierWithoutVihb = createCompany("Carrier No VIHB", null, null, "45677645")

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
      carrierPartyId = carrierWithoutVihb,
      weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 1000.0)
    )

    // When
    val results = queryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then
    assertThat(results).hasSize(1)
    assertThat(results.first().transporters).isEmpty()
  }

  // Helper methods for test data setup

  private fun setupTestData() {
    // Create reference data first
    euralRepository.save(nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural(
      code = "010101*",
      description = "Test Eural"
    ))
    processingMethodRepository.save(nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto(
      code = "R01",
      description = "Test Method"
    ))

    // Create processor company with processor_id '08797'
    processorCompanyId = createCompany("Test Processor", "08797", "VIHB_PROC", "98765432")

    // Create consignor company
    consignorCompanyId = createCompany("Test Consignor", null, "VIHB_CONS", "02398576")

    // Create carrier companies
    carrierCompanyId1 = createCompany("Carrier 1", null, "VIHB001", "09098787")
    carrierCompanyId2 = createCompany("Carrier 2", null, "VIHB002", "12341234")

    // Create pickup location
    pickupLocationId = createPickupLocation()

    entityManager.flush()
  }

  private fun createCompany(name: String, processorId: String?, vihbId: String?, chamberOfCommerceId: String): UUID {
    val company = CompanyDto(
      id = UUID.randomUUID(),
      name = name,
      processorId = processorId,
      vihbId = vihbId,
      chamberOfCommerceId = chamberOfCommerceId,
      address = AddressDto(
        streetName = "Test Street",
        buildingNumber = "1",
        postalCode = "1234AB",
        city = "Amsterdam",
        country = "Nederland",
      ),
      deletedAt = null,
    )
    return companyRepository.save(company).id
  }

  private fun createPickupLocation(): String {
    val location = PickupLocationDto.DutchAddressDto(
      streetName = "Pickup Street",
      buildingNumber = "10",
      buildingNumberAddition = null,
      postalCode = "5678CD",
      city = "Rotterdam",
      country = "Nederland"
    )
    return pickupLocationRepository.save(location).id
  }

  private fun createWasteStream(
    number: String,
    status: String,
    processorParty: CompanyDto = companyRepository.getReferenceById(processorCompanyId)
  ) {
    val wasteStream = WasteStreamDto(
      number = number,
      name = "Test Waste Stream",
      euralCode = euralRepository.getReferenceById("010101*"),
      processingMethodCode = processingMethodRepository.getReferenceById("R01"),
      wasteCollectionType = "DEFAULT",
      pickupLocation = pickupLocationRepository.getReferenceById(pickupLocationId),
      consignorParty = companyRepository.getReferenceById(consignorCompanyId),
      pickupParty = companyRepository.getReferenceById(consignorCompanyId),
      dealerParty = null,
      collectorParty = null,
      brokerParty = null,
      processorParty = processorParty,
      status = status,
      lastActivityAt = Instant.now(),
      consignorClassification = 1
    )
    wasteStreamRepository.save(wasteStream)
  }

  private fun createWeightTicket(
    carrierPartyId: UUID,
    weightedAt: OffsetDateTime,
    status: String = "COMPLETED",
    lines: List<Pair<String, Double>> = emptyList()
  ): Long {
    // Convert OffsetDateTime to Instant (which is what the DTO expects)
    val weightedAtInstant = weightedAt.toInstant()

    // Create weight ticket lines from the provided pairs (wasteStreamNumber, weightValue)
    val ticketLines = lines.map { (wasteStreamNumber, weightValue) ->
      WeightTicketLineDto(
        wasteStreamNumber = wasteStreamNumber,
        weightValue = weightValue.toBigDecimal(),
        weightUnit = WeightUnitDto.kg
      )
    }

    val ticket = WeightTicketDto(
      id = System.currentTimeMillis() + (Math.random() * 1000).toLong(),
      consignorParty = companyRepository.getReferenceById(consignorCompanyId),
      lines = ticketLines,
      tarraWeightValue = null,
      tarraWeightUnit = null,
      direction = WeightTicketDirection.INBOUND,
      pickupLocation = null,
      deliveryLocation = null,
      carrierParty = companyRepository.getReferenceById(carrierPartyId),
      truckLicensePlate = null,
      reclamation = null,
      note = null,
      status = WeightTicketStatusDto.valueOf(status),
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      weightedAt = weightedAtInstant,
      cancellationReason = null
    )
    return weightTicketRepository.save(ticket).id
  }

  private fun createLmaDeclaration(wasteStreamNumber: String, period: String) {
    jdbcTemplate.update(
      """
      INSERT INTO lma_declarations (id, waste_stream_number, period, transporters,
                                    total_weight, total_shipments, created_at)
      VALUES (?, ?, ?, ARRAY[]::text[], 0, 0, NOW())
      """,
      UUID.randomUUID().toString(), wasteStreamNumber, period
    )
  }

  private fun cleanupDatabase() {
    jdbcTemplate.execute("DELETE FROM weight_ticket_lines")
    jdbcTemplate.execute("DELETE FROM weight_tickets")
    jdbcTemplate.execute("DELETE FROM lma_declarations")
    jdbcTemplate.execute("DELETE FROM waste_streams")
    jdbcTemplate.execute("DELETE FROM pickup_locations")
    jdbcTemplate.execute("DELETE FROM companies")
    entityManager.clear()
  }
}
