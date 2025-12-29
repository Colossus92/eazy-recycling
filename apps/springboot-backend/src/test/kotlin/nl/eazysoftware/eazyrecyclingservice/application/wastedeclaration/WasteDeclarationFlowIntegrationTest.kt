package nl.eazysoftware.eazyrecyclingservice.application.wastedeclaration

import jakarta.persistence.EntityManager
import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketDirection
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclaration
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.FirstReceivalWasteStreamQueryAdapter
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.MonthlyReceivalWasteStreamQueryAdapter
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.time.Clock

/**
 * Integration test for the waste declaration flow according to ADR-0014.
 *
 * Tests the complete declaration flow including:
 * - Finding lines eligible for declaration (undeclared or changed weight)
 * - Marking lines as declared after successful declaration
 * - Ensuring declared lines are not picked up again
 * - Handling weight corrections (changed weights after declaration)
 */
@SpringBootTest
@ActiveProfiles("test")
class WasteDeclarationFlowIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var firstReceivalQueryAdapter: FirstReceivalWasteStreamQueryAdapter

  @Autowired
  private lateinit var monthlyReceivalQueryAdapter: MonthlyReceivalWasteStreamQueryAdapter

  @Autowired
  private lateinit var weightTickets: WeightTickets

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
  private lateinit var carrierCompanyId: UUID
  private lateinit var pickupLocationId: String

  @BeforeEach
  fun setup() {
    cleanupDatabase()
    setupTestData()
  }

  @Test
  fun `happy flow - should find undeclared lines and mark them as declared`() {
    // Given - a waste stream with undeclared weight ticket lines
    val wasteStreamNumber = "087970000001"
    val yearMonth = YearMonth(2025, 11)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    val weightTicketId = createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 1000.0)
    )

    // When - query for first receival declarations
    val declarations = firstReceivalQueryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then - should find the undeclared line
    assertThat(declarations).hasSize(1)
    assertThat(declarations.first().wasteStream.wasteStreamNumber.number).isEqualTo(wasteStreamNumber)
    assertThat(declarations.first().totalWeight).isEqualTo(1000)

    // When - mark lines as declared using the specific weight ticket ID
    val declaredAt = Clock.System.now()
    val updatedCount = weightTickets.markLinesAsDeclared(
      wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
      weightTicketIds = listOf(weightTicketId),
      declaredAt = declaredAt
    )

    // Then - lines should be marked as declared
    assertThat(updatedCount).isEqualTo(1)

    // Verify the declared_weight and last_declared_at are set
    val declaredWeight = jdbcTemplate.queryForObject(
      "SELECT declared_weight FROM weight_ticket_lines WHERE weight_ticket_id = ? AND waste_stream_number = ?",
      BigDecimal::class.java,
      weightTicketId,
      wasteStreamNumber
    )
    assertThat(declaredWeight).isEqualByComparingTo(BigDecimal("1000.00"))

    val lastDeclaredAt = jdbcTemplate.queryForObject(
      "SELECT last_declared_at FROM weight_ticket_lines WHERE weight_ticket_id = ? AND waste_stream_number = ?",
      java.sql.Timestamp::class.java,
      weightTicketId,
      wasteStreamNumber
    )
    assertThat(lastDeclaredAt).isNotNull()
  }

  @Test
  fun `should not find already declared lines in subsequent queries`() {
    // Given - a waste stream with declared weight ticket lines
    val wasteStreamNumber = "087970000002"
    val yearMonth = YearMonth(2025, 11)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    val weightTicketId = createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 1500.0)
    )

    // First query should find the undeclared line
    val firstDeclarations = firstReceivalQueryAdapter.findFirstReceivalDeclarations(yearMonth)
    assertThat(firstDeclarations).hasSize(1)

    // Mark as declared using the specific weight ticket ID
    weightTickets.markLinesAsDeclared(
      wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
      weightTicketIds = listOf(weightTicketId),
      declaredAt = Clock.System.now()
    )

    // Create a declaration record (simulating what DeclareFirstReceivals does)
    createLmaDeclaration(wasteStreamNumber, "112025")

    entityManager.flush()
    entityManager.clear()

    // When - query again for first receivals (should be empty since now it has a declaration)
    val secondDeclarations = firstReceivalQueryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then - should not find the already declared waste stream
    assertThat(secondDeclarations).isEmpty()
  }

  @Test
  fun `should find lines with changed weight for correction`() {
    // Given - a waste stream with declared weight ticket lines where weight has changed
    val wasteStreamNumber = "087970000003"
    val yearMonth = YearMonth(2025, 11)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    val weightTicketId = createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 1000.0)
    )

    // Mark as declared with original weight using specific weight ticket ID
    weightTickets.markLinesAsDeclared(
      wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
      weightTicketIds = listOf(weightTicketId),
      declaredAt = Clock.System.now()
    )

    // Create existing declaration (for monthly receivals query)
    createLmaDeclaration(wasteStreamNumber, "102025") // Previous period declaration

    // Simulate weight change (update weight_value but not declared_weight)
    jdbcTemplate.update(
      "UPDATE weight_ticket_lines SET weight_value = 1500.00 WHERE weight_ticket_id = ? AND waste_stream_number = ?",
      weightTicketId,
      wasteStreamNumber
    )

    entityManager.flush()
    entityManager.clear()

    // When - query for monthly receivals (waste stream has prior declaration)
    val declarations = monthlyReceivalQueryAdapter.findMonthlyReceivalDeclarations(yearMonth)

    // Then - should find the line needing correction
    assertThat(declarations).hasSize(1)
    assertThat(declarations.first().wasteStreamNumber.number).isEqualTo(wasteStreamNumber)
    assertThat(declarations.first().totalWeight).isEqualTo(1500) // The corrected weight
  }

  @Test
  fun `should aggregate multiple undeclared lines for the same waste stream`() {
    // Given - a waste stream with multiple undeclared weight ticket lines
    val wasteStreamNumber = "087970000004"
    val yearMonth = YearMonth(2025, 11)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = OffsetDateTime.of(2025, 11, 10, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 500.0)
    )
    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = OffsetDateTime.of(2025, 11, 20, 14, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 700.0)
    )

    // When
    val declarations = firstReceivalQueryAdapter.findFirstReceivalDeclarations(yearMonth)

    // Then
    assertThat(declarations).hasSize(1)
    assertThat(declarations.first().totalWeight).isEqualTo(1200) // Sum of 500 + 700
    assertThat(declarations.first().totalShipments).isEqualTo(2)
  }

  @Test
  fun `should only mark undeclared or changed lines as declared`() {
    // Given - two waste streams, one already declared, one not
    val wasteStream1 = "087970000005"
    val wasteStream2 = "087970000006"

    createWasteStream(wasteStream1, "ACTIVE")
    createWasteStream(wasteStream2, "ACTIVE")

    // Create weight tickets for both
    val weightTicketId1 = createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStream1 to 1000.0)
    )
    val weightTicketId2 = createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = OffsetDateTime.of(2025, 11, 16, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStream2 to 2000.0)
    )

    // Mark wasteStream1 as already declared
    weightTickets.markLinesAsDeclared(
      wasteStreamNumber = WasteStreamNumber(wasteStream1),
      weightTicketIds = listOf(weightTicketId1),
      declaredAt = Clock.System.now()
    )

    entityManager.flush()
    entityManager.clear()

    // When - try to mark wasteStream1 again (should be no-op since already declared with same weight)
    val updatedCount1 = weightTickets.markLinesAsDeclared(
      wasteStreamNumber = WasteStreamNumber(wasteStream1),
      weightTicketIds = listOf(weightTicketId1),
      declaredAt = Clock.System.now()
    )

    // Then - wasteStream1 should not be updated (already declared with same weight)
    assertThat(updatedCount1).isEqualTo(0)

    // When - mark wasteStream2 as declared
    val updatedCount2 = weightTickets.markLinesAsDeclared(
      wasteStreamNumber = WasteStreamNumber(wasteStream2),
      weightTicketIds = listOf(weightTicketId2),
      declaredAt = Clock.System.now()
    )

    // Then - wasteStream2 should be updated
    assertThat(updatedCount2).isEqualTo(1)
  }

  @Test
  fun `should handle empty list of weight ticket IDs`() {
    val wasteStreamNumber = "087970000007"

    // When
    val updatedCount = weightTickets.markLinesAsDeclared(
      wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
      weightTicketIds = emptyList(),
      declaredAt = Clock.System.now()
    )

    // Then
    assertThat(updatedCount).isEqualTo(0)
  }

  // Helper methods

  private fun setupTestData() {
    euralRepository.save(nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural(
      code = "010101*",
      description = "Test Eural"
    ))
    processingMethodRepository.save(nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto(
      code = "R01",
      description = "Test Method"
    ))

    processorCompanyId = createCompany("Test Processor", "08797", "VIHB_PROC", "98765432")
    consignorCompanyId = createCompany("Test Consignor", null, "VIHB_CONS", "02398576")
    carrierCompanyId = createCompany("Carrier", null, "VIHB001", "09098787")
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

  private fun createWasteStream(number: String, status: String) {
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
      catalogItem = null,
      processorParty = companyRepository.getReferenceById(processorCompanyId),
      status = status,
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
    val weightedAtInstant = weightedAt.toInstant()

    val ticketId = System.currentTimeMillis() + (Math.random() * 1000).toLong()
    val ticketLines = lines.map { (wasteStreamNumber, weightValue) ->
      WeightTicketLineDto(
        id = UUID.randomUUID(),
        weightTicketId = ticketId,
        wasteStreamNumber = wasteStreamNumber,
        catalogItemId = UUID.randomUUID(),
        weightValue = weightValue.toBigDecimal(),
        weightUnit = WeightUnitDto.kg
      )
    }

    val ticket = WeightTicketDto(
      id = ticketId,
      consignorParty = companyRepository.getReferenceById(consignorCompanyId),
      lines = ticketLines.toMutableList(),
      secondWeighingValue = null,
      secondWeighingUnit = null,
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
      weightedAt = weightedAtInstant,
      cancellationReason = null
    )
    return weightTicketRepository.save(ticket).id
  }

  private fun createLmaDeclaration(wasteStreamNumber: String, period: String) {
    jdbcTemplate.update(
      """
      INSERT INTO lma_declarations (id, amice_uuid, waste_stream_number, period, transporters,
                                    total_weight, total_shipments, type, created_at, errors, status)
      VALUES (?, ?, ?, ?, ARRAY[]::text[], 0, 0, ?, NOW(), ARRAY[]::text[], 'COMPLETED')
      """,
      UUID.randomUUID().toString(), UUID.randomUUID(), wasteStreamNumber, period, LmaDeclaration.Type.FIRST_RECEIVAL.name
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
