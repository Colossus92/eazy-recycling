package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MaandelijkseOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
class LmaDeclarationRepositoryTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var lmaDeclarations: LmaDeclarations

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var companyRepository: CompanyJpaRepository

  @Autowired
  private lateinit var wasteStreamRepository: WasteStreamJpaRepository

  @Autowired
  private lateinit var pickupLocationRepository: PickupLocationRepository

  @Autowired
  private lateinit var euralRepository: EuralRepository

  @Autowired
  private lateinit var processingMethodRepository: ProcessingMethodRepository

  private lateinit var processorCompanyId: UUID
  private lateinit var consignorCompanyId: UUID
  private lateinit var pickupLocationId: String

  @BeforeEach
  fun setup() {
    cleanupDatabase()
    setupTestData()
  }

  @Test
  fun `should return paginated declarations with waste name and pickup location`() {
    // Given - create waste stream and declaration
    val wasteStreamNumber = "087970000001"
    createWasteStream(wasteStreamNumber, "Test Waste Stream")
    createLmaDeclaration(wasteStreamNumber, "112025", 5000, 10)

    // When
    val pageable = PageRequest.of(0, 10)
    val result = lmaDeclarations.findAll(pageable)

    // Then
    assertThat(result.content).hasSize(1)
    assertThat(result.totalElements).isEqualTo(1)

    val declaration = result.content.first()
    assertThat(declaration.wasteStreamNumber).isEqualTo(WasteStreamNumber(wasteStreamNumber))
    assertThat(declaration.wasteName).isEqualTo("Test Waste Stream")
    assertThat(declaration.totalWeight).isEqualTo(5000)
    assertThat(declaration.totalTransports).isEqualTo(10)
    assertThat(declaration.period).isEqualTo(YearMonth(2025, 11))
    assertThat(declaration.status).isEqualTo("PENDING")
    assertThat(declaration.pickupLocation).isInstanceOf(Location.DutchAddress::class.java)
  }

  @Test
  fun `should handle declarations without waste stream`() {
    // Given - declaration without corresponding waste stream
    createLmaDeclaration("999999999999", "112025", 1000, 5)

    // When
    val pageable = PageRequest.of(0, 10)
    val result = lmaDeclarations.findAll(pageable)

    // Then
    assertThat(result.content).hasSize(1)
    val declaration = result.content.first()
    assertThat(declaration.wasteName).isEqualTo("Unknown")
    assertThat(declaration.pickupLocation).isEqualTo(Location.NoLocation)
  }

  @Test
  fun `should support pagination`() {
    // Given - multiple declarations
    for (i in 1..15) {
      val wasteStreamNumber = "087970%06d".format(i)
      createWasteStream(wasteStreamNumber, "Waste Stream $i")
      createLmaDeclaration(wasteStreamNumber, "112025", 1000 * i, i)
    }

    // When - request first page
    val page1 = lmaDeclarations.findAll(PageRequest.of(0, 10))

    // Then
    assertThat(page1.content).hasSize(10)
    assertThat(page1.totalElements).isEqualTo(15)
    assertThat(page1.totalPages).isEqualTo(2)
    assertThat(page1.hasNext()).isTrue()

    // When - request second page
    val page2 = lmaDeclarations.findAll(PageRequest.of(1, 10))

    // Then
    assertThat(page2.content).hasSize(5)
    assertThat(page2.hasNext()).isFalse()
  }

  @Test
  fun `should parse period correctly`() {
    // Given - declarations with different periods
    createWasteStream("087970000001", "Test 1")
    createWasteStream("087970000002", "Test 2")
    createLmaDeclaration("087970000001", "012025", 1000, 1) // January 2025
    createLmaDeclaration("087970000002", "122024", 2000, 2) // December 2024

    // When
    val result = lmaDeclarations.findAll(PageRequest.of(0, 10))

    // Then
    val declarations = result.content.sortedBy { it.wasteStreamNumber.number }
    assertThat(declarations[0].period).isEqualTo(YearMonth(2025, 1))
    assertThat(declarations[1].period).isEqualTo(YearMonth(2024, 12))
  }

  @Test
  fun `saveAllPendingFirstReceivals should save declarations and update waste stream timestamps`() {
    // Given - create waste stream with old timestamp
    val wasteStreamNumber = "087970000001"
    createWasteStream(wasteStreamNumber, "Test Waste Stream")

    // Set last_modified_at to an old timestamp
    val oldTimestamp = Instant.now().minus(10, ChronoUnit.DAYS)
    jdbcTemplate.update(
      "UPDATE waste_streams SET last_modified_at = ? WHERE number = ?",
      Timestamp.from(oldTimestamp), wasteStreamNumber
    )

    val firstReceival = EersteOntvangstMeldingDetails().apply {
      meldingsNummerMelder = "DECL-001"
      afvalstroomNummer = wasteStreamNumber
      periodeMelding = "112025"
      vervoerders = "Transporter A, Transporter B"
      totaalGewicht = 5000
      aantalVrachten = 10
    }

    // When
    lmaDeclarations.saveAllPendingFirstReceivals(listOf(firstReceival))

    // Then - verify declaration was saved
    val savedDeclarations = lmaDeclarations.findAll(PageRequest.of(0, 10))
    assertThat(savedDeclarations.content).hasSize(1)
    assertThat(savedDeclarations.content.first().wasteStreamNumber.number).isEqualTo(wasteStreamNumber)
    assertThat(savedDeclarations.content.first().status).isEqualTo("PENDING")

    // Verify waste stream timestamp was updated
    val updatedTimestamp = jdbcTemplate.queryForObject(
      "SELECT last_modified_at FROM waste_streams WHERE number = ?",
      Timestamp::class.java,
      wasteStreamNumber
    )?.toInstant()
    assertThat(updatedTimestamp).isAfter(oldTimestamp)
  }

  @Test
  fun `saveAllPendingMonthlyReceivals should save declarations and update waste stream timestamps`() {
    // Given - create waste stream with old timestamp
    val wasteStreamNumber = "087970000002"
    createWasteStream(wasteStreamNumber, "Monthly Waste Stream")

    // Set last_modified_at to an old timestamp
    val oldTimestamp = Instant.now().minus(10, ChronoUnit.DAYS)
    jdbcTemplate.update(
      "UPDATE waste_streams SET last_modified_at = ? WHERE number = ?",
      Timestamp.from(oldTimestamp), wasteStreamNumber
    )

    val monthlyReceival = MaandelijkseOntvangstMeldingDetails().apply {
      meldingsNummerMelder = "DECL-002"
      afvalstroomNummer = wasteStreamNumber
      periodeMelding = "102025"
      vervoerders = "Transporter C"
      totaalGewicht = 3000
      aantalVrachten = 5
    }

    // When
    lmaDeclarations.saveAllPendingMonthlyReceivals(listOf(monthlyReceival))

    // Then - verify declaration was saved
    val savedDeclarations = lmaDeclarations.findAll(PageRequest.of(0, 10))
    assertThat(savedDeclarations.content).hasSize(1)
    assertThat(savedDeclarations.content.first().wasteStreamNumber.number).isEqualTo(wasteStreamNumber)
    assertThat(savedDeclarations.content.first().status).isEqualTo("PENDING")

    // Verify waste stream timestamp was updated
    val updatedTimestamp = jdbcTemplate.queryForObject(
      "SELECT last_modified_at FROM waste_streams WHERE number = ?",
      Timestamp::class.java,
      wasteStreamNumber
    )?.toInstant()
    assertThat(updatedTimestamp).isAfter(oldTimestamp)
  }

  @Test
  fun `saveAllPendingFirstReceivals should update multiple waste streams`() {
    // Given - create multiple waste streams
    val wasteStreamNumber1 = "087970000003"
    val wasteStreamNumber2 = "087970000004"
    createWasteStream(wasteStreamNumber1, "Waste Stream 1")
    createWasteStream(wasteStreamNumber2, "Waste Stream 2")

    val oldTimestamp = Instant.now().minus(10, ChronoUnit.DAYS)
    jdbcTemplate.update(
      "UPDATE waste_streams SET last_modified_at = ? WHERE number = ?",
      Timestamp.from(oldTimestamp), wasteStreamNumber1
    )
    jdbcTemplate.update(
      "UPDATE waste_streams SET last_modified_at = ? WHERE number = ?",
      Timestamp.from(oldTimestamp), wasteStreamNumber2
    )

    val firstReceivals = listOf(
      EersteOntvangstMeldingDetails().apply {
        meldingsNummerMelder = "DECL-003"
        afvalstroomNummer = wasteStreamNumber1
        periodeMelding = "112025"
        vervoerders = "Transporter A"
        totaalGewicht = 1000
        aantalVrachten = 2
      },
      EersteOntvangstMeldingDetails().apply {
        meldingsNummerMelder = "DECL-004"
        afvalstroomNummer = wasteStreamNumber2
        periodeMelding = "112025"
        vervoerders = "Transporter B"
        totaalGewicht = 2000
        aantalVrachten = 4
      }
    )

    // When
    lmaDeclarations.saveAllPendingFirstReceivals(firstReceivals)

    // Then - verify both declarations were saved
    val savedDeclarations = lmaDeclarations.findAll(PageRequest.of(0, 10))
    assertThat(savedDeclarations.content).hasSize(2)

    // Verify both waste stream timestamps were updated
    val updatedTimestamp1 = jdbcTemplate.queryForObject(
      "SELECT last_modified_at FROM waste_streams WHERE number = ?",
      Timestamp::class.java,
      wasteStreamNumber1
    )?.toInstant()
    val updatedTimestamp2 = jdbcTemplate.queryForObject(
      "SELECT last_modified_at FROM waste_streams WHERE number = ?",
      Timestamp::class.java,
      wasteStreamNumber2
    )?.toInstant()
    assertThat(updatedTimestamp1).isAfter(oldTimestamp)
    assertThat(updatedTimestamp2).isAfter(oldTimestamp)
  }

  @Test
  fun `saveAllPendingFirstReceivals should handle non-existent waste streams gracefully`() {
    // Given - declaration for non-existent waste stream
    val nonExistentWasteStreamNumber = "999999999999"

    val firstReceival = EersteOntvangstMeldingDetails().apply {
      meldingsNummerMelder = "DECL-005"
      afvalstroomNummer = nonExistentWasteStreamNumber
      periodeMelding = "112025"
      vervoerders = "Transporter X"
      totaalGewicht = 500
      aantalVrachten = 1
    }

    // When - should not throw exception
    lmaDeclarations.saveAllPendingFirstReceivals(listOf(firstReceival))

    // Then - declaration should still be saved
    val savedDeclarations = lmaDeclarations.findAll(PageRequest.of(0, 10))
    assertThat(savedDeclarations.content).hasSize(1)
    assertThat(savedDeclarations.content.first().wasteStreamNumber.number).isEqualTo(nonExistentWasteStreamNumber)
  }

  // Helper methods

  private fun setupTestData() {
    // Create reference data
    euralRepository.save(nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural(
      code = "010101*",
      description = "Test Eural"
    ))
    processingMethodRepository.save(nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto(
      code = "R01",
      description = "Test Method"
    ))

    // Create processor company
    processorCompanyId = createCompany("Test Processor", "08797", "VIHB_PROC", "98765432")

    // Create consignor company
    consignorCompanyId = createCompany("Test Consignor", null, "VIHB_CONS", "02398576")

    // Create pickup location
    pickupLocationId = createPickupLocation()
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

  private fun createWasteStream(number: String, name: String) {
    val wasteStream = WasteStreamDto(
      number = number,
      name = name,
      euralCode = euralRepository.getReferenceById("010101*"),
      processingMethodCode = processingMethodRepository.getReferenceById("R01"),
      wasteCollectionType = "DEFAULT",
      pickupLocation = pickupLocationRepository.getReferenceById(pickupLocationId),
      consignorParty = companyRepository.getReferenceById(consignorCompanyId),
      pickupParty = companyRepository.getReferenceById(consignorCompanyId),
      dealerParty = null,
      collectorParty = null,
      brokerParty = null,
      processorParty = companyRepository.getReferenceById(processorCompanyId),
      status = "ACTIVE",
      consignorClassification = 1
    )
    wasteStreamRepository.save(wasteStream)
  }

  private fun createLmaDeclaration(wasteStreamNumber: String, period: String, totalWeight: Int, totalShipments: Int) {
    jdbcTemplate.update(
      """
      INSERT INTO lma_declarations (id, amice_uuid, waste_stream_number, period, transporters,
                                    total_weight, total_shipments, created_at, errors, status)
      VALUES (?, ?, ?, ?, ARRAY[]::text[], ?, ?, NOW(), ARRAY[]::text[], 'PENDING')
      """,
      UUID.randomUUID().toString(), UUID.randomUUID(), wasteStreamNumber, period, totalWeight.toLong(), totalShipments.toLong()
    )
  }

  private fun cleanupDatabase() {
    jdbcTemplate.execute("DELETE FROM lma_declarations")
    jdbcTemplate.execute("DELETE FROM waste_streams")
    jdbcTemplate.execute("DELETE FROM pickup_locations")
    jdbcTemplate.execute("DELETE FROM companies")
  }
}
