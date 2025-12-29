package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketDirection
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclaration
import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
class DetectLateDeclarationsIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var detectLateDeclarations: DetectLateDeclarations

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
  fun `should create FIRST_RECEIVAL declaration for undeclared waste stream with no prior declarations`() {
    val wasteStreamNumber = "087970000001"

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = OffsetDateTime.of(2025, 10, 15, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 1000.0)
    )

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    val declarations = findLmaDeclarations(wasteStreamNumber, "WAITING_APPROVAL")
    assertThat(declarations).hasSize(1)
    assertThat(declarations.first().id).hasSize(12)
    assertThat(declarations.first().totalWeight).isEqualTo(1000L)
    assertThat(declarations.first().totalShipments).isEqualTo(1L)
    assertThat(declarations.first().status).isEqualTo(LmaDeclarationDto.Status.WAITING_APPROVAL)
    assertThat(declarations.first().type).isEqualTo(LmaDeclaration.Type.FIRST_RECEIVAL)
  }

  @Test
  fun `should create MONTHLY_RECEIVAL declaration for undeclared waste stream with prior declarations`() {
    val wasteStreamNumber = "087970000002"

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createCompletedLmaDeclaration(wasteStreamNumber, "102025")

    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = OffsetDateTime.of(2025, 10, 18, 10, 0, 0, 0, ZoneOffset.UTC),
      lines = listOf(wasteStreamNumber to 2500.0)
    )

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    val declarations = findLmaDeclarations(wasteStreamNumber, "WAITING_APPROVAL")
    assertThat(declarations).hasSize(1)
    assertThat(declarations.first().id).hasSize(12)
    assertThat(declarations.first().totalWeight).isEqualTo(2500L)
    assertThat(declarations.first().totalShipments).isEqualTo(1L)
    assertThat(declarations.first().type).isEqualTo(LmaDeclaration.Type.MONTHLY_RECEIVAL)
  }

  @Test
  fun `should overwrite pending FIRST_RECIEVAL declaration when new undeclared tickets are added for same period`() {
    val wasteStreamNumber = "087970000003"
    val weightedAt = OffsetDateTime.of(2025, 10, 15, 10, 0, 0, 0, ZoneOffset.UTC)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = weightedAt,
      lines = listOf(wasteStreamNumber to 1000.0)
    )

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    val firstDeclarations = findLmaDeclarations(wasteStreamNumber, "WAITING_APPROVAL")
    assertThat(firstDeclarations).hasSize(1)
    val firstDeclarationId = firstDeclarations.first().id
    assertThat(firstDeclarationId).hasSize(12)
    assertThat(firstDeclarations.first().totalWeight).isEqualTo(1000L)
    assertThat(firstDeclarations.first().totalShipments).isEqualTo(1L)
    assertThat(firstDeclarations.first().type).isEqualTo(LmaDeclaration.Type.FIRST_RECEIVAL)

    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = weightedAt.plusDays(1),
      lines = listOf(wasteStreamNumber to 500.0)
    )

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    val secondDeclarations = findLmaDeclarations(wasteStreamNumber, "WAITING_APPROVAL")
    assertThat(secondDeclarations).hasSize(1)
    assertThat(secondDeclarations.first().id).isNotEqualTo(firstDeclarationId)
    assertThat(secondDeclarations.first().id).hasSize(12)
    assertThat(secondDeclarations.first().totalWeight).isEqualTo(1500L)
    assertThat(secondDeclarations.first().totalShipments).isEqualTo(2L)
    assertThat(secondDeclarations.first().type).isEqualTo(LmaDeclaration.Type.FIRST_RECEIVAL)
  }

  @Test
  fun `should overwrite pending MONTHLY_RECEIVAL declaration when new undeclared tickets are added for same period`() {
    val wasteStreamNumber = "087970000004"
    val weightedAt = OffsetDateTime.of(2025, 10, 18, 10, 0, 0, 0, ZoneOffset.UTC)

    createWasteStream(wasteStreamNumber, "ACTIVE")
    createCompletedLmaDeclaration(wasteStreamNumber, "102025")

    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = weightedAt,
      lines = listOf(wasteStreamNumber to 2000.0)
    )

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    val firstDeclarations = findLmaDeclarations(wasteStreamNumber, "WAITING_APPROVAL")
    assertThat(firstDeclarations).hasSize(1)
    val firstDeclaration = firstDeclarations.first()
    assertThat(firstDeclaration.id).hasSize(12)
    assertThat(firstDeclaration.totalWeight).isEqualTo(2000L)
    assertThat(firstDeclaration.totalShipments).isEqualTo(1L)
    assertThat(firstDeclaration.type).isEqualTo(LmaDeclaration.Type.MONTHLY_RECEIVAL)


    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = weightedAt.plusDays(1),
      lines = listOf(wasteStreamNumber to 800.0)
    )
    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = weightedAt.plusDays(2),
      lines = listOf(wasteStreamNumber to 1200.0)
    )

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    val secondDeclarations = findLmaDeclarations(wasteStreamNumber, "WAITING_APPROVAL")
    assertThat(secondDeclarations).hasSize(1)
    assertThat(secondDeclarations.first().id).isNotEqualTo(firstDeclaration.id)
    assertThat(secondDeclarations.first().id).hasSize(12)
    assertThat(secondDeclarations.first().totalWeight).isEqualTo(4000L)
    assertThat(secondDeclarations.first().totalShipments).isEqualTo(3L)
    assertThat(secondDeclarations.first().type).isEqualTo(LmaDeclaration.Type.MONTHLY_RECEIVAL)
  }

  @Test
  fun `should not overwrite pending declarations for different periods`() {
    val wasteStreamNumber = "087970000005"

    createWasteStream(wasteStreamNumber, "ACTIVE")

    val octoberDate = OffsetDateTime.of(2025, 10, 15, 10, 0, 0, 0, ZoneOffset.UTC)
    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = octoberDate,
      lines = listOf(wasteStreamNumber to 1000.0)
    )

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    val novemberDate = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC)
    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = novemberDate,
      lines = listOf(wasteStreamNumber to 2000.0)
    )

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    val allDeclarations = findAllLmaDeclarations(wasteStreamNumber, "WAITING_APPROVAL")
    assertThat(allDeclarations).hasSize(2)
  }

  @Test
  fun `should not overwrite completed or failed late declarations`() {
    val wasteStreamNumber = "087970000006"
    val weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC)

    createWasteStream(wasteStreamNumber, "ACTIVE")

    val completedDeclarationId = "000000000123"
    createLmaDeclarationWithStatus(wasteStreamNumber, "112025", "COMPLETED", completedDeclarationId, 1000L, 1L)

    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = weightedAt,
      lines = listOf(wasteStreamNumber to 500.0)
    )

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    val completedDeclaration = jdbcTemplate.queryForMap(
      "SELECT * FROM lma_declarations WHERE id = ?",
      completedDeclarationId
    )
    assertThat(completedDeclaration).isNotNull
    assertThat(completedDeclaration["status"]).isEqualTo("COMPLETED")

    val allDeclarations = findAllLmaDeclarations(wasteStreamNumber, null)
    assertThat(allDeclarations).hasSize(2)
  }

  @Test
  fun `should overwrite multiple pending late declarations for same waste stream and period`() {
    val wasteStreamNumber = "087970000007"
    val weightedAt = OffsetDateTime.of(2025, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC)

    createWasteStream(wasteStreamNumber, "ACTIVE")

    createLmaDeclarationWithStatus(wasteStreamNumber, "112025", "WAITING_APPROVAL", "000000000001", 1000L, 1L)
    createLmaDeclarationWithStatus(wasteStreamNumber, "112025", "WAITING_APPROVAL", "000000000002", 2000L, 2L)

    createWeightTicket(
      carrierPartyId = carrierCompanyId,
      weightedAt = weightedAt,
      lines = listOf(wasteStreamNumber to 5000.0)
    )

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    val declarations = findLmaDeclarations(wasteStreamNumber, "WAITING_APPROVAL")
    assertThat(declarations).hasSize(1)
    assertThat(declarations.first().id).isNotIn("000000000001", "000000000002")
    assertThat(declarations.first().totalWeight).isEqualTo(5000L)
  }

  private fun findLmaDeclarations(wasteStreamNumber: String, status: String): List<LmaDeclarationDto> {
    return jdbcTemplate.query(
      "SELECT * FROM lma_declarations WHERE waste_stream_number = ? AND status = ?",
      { rs, _ ->
        LmaDeclarationDto(
          id = rs.getString("id"),
          amiceUUID = rs.getString("amice_uuid")?.let { UUID.fromString(it) },
          wasteStreamNumber = rs.getString("waste_stream_number"),
          period = rs.getString("period"),
          transporters = (rs.getArray("transporters")?.array as Array<*>).map { it.toString() },
          totalWeight = rs.getLong("total_weight"),
          totalShipments = rs.getLong("total_shipments"),
          type = LmaDeclaration.Type.valueOf(rs.getString("type")),
          createdAt = rs.getTimestamp("created_at").toInstant(),
          errors = null,
          status = LmaDeclarationDto.Status.valueOf(rs.getString("status"))
        )
      },
      wasteStreamNumber,
      status
    )
  }

  private fun findAllLmaDeclarations(wasteStreamNumber: String, status: String?): List<LmaDeclarationDto> {
    return if (status != null) {
      findLmaDeclarations(wasteStreamNumber, status)
    } else {
      jdbcTemplate.query(
        "SELECT * FROM lma_declarations WHERE waste_stream_number = ?",
        { rs, _ ->
          LmaDeclarationDto(
            id = rs.getString("id"),
            amiceUUID = rs.getString("amice_uuid")?.let { UUID.fromString(it) },
            wasteStreamNumber = rs.getString("waste_stream_number"),
            period = rs.getString("period"),
            transporters = (rs.getArray("transporters")?.array as Array<*>).map { it.toString() },
            totalWeight = rs.getLong("total_weight"),
            totalShipments = rs.getLong("total_shipments"),
            type = LmaDeclaration.Type.valueOf(rs.getString("type")),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            errors = null,
            status = LmaDeclarationDto.Status.valueOf(rs.getString("status"))
          )
        },
        wasteStreamNumber
      )
    }
  }

  private fun createCompletedLmaDeclaration(wasteStreamNumber: String, period: String) {
    jdbcTemplate.update(
      """
      INSERT INTO lma_declarations (id, amice_uuid, waste_stream_number, period, transporters,
                                    total_weight, total_shipments, type, created_at, errors, status)
      VALUES (?, ?, ?, ?, ARRAY[]::text[], 0, 0, ?, NOW(), ARRAY[]::text[], 'COMPLETED')
      """,
      UUID.randomUUID().toString(), UUID.randomUUID(), wasteStreamNumber, period, LmaDeclaration.Type.MONTHLY_RECEIVAL.name
    )
  }

  private fun createLmaDeclarationWithStatus(
    wasteStreamNumber: String,
    period: String,
    status: String,
    id: String,
    totalWeight: Long,
    totalShipments: Long
  ) {
    jdbcTemplate.update(
      """
      INSERT INTO lma_declarations (id, amice_uuid, waste_stream_number, period, transporters,
                                    total_weight, total_shipments, type, created_at, errors, status)
      VALUES (?, ?, ?, ?, ARRAY[]::text[], ?, ?, ?, NOW(), ARRAY[]::text[], ?)
      """,
      id, UUID.randomUUID(), wasteStreamNumber, period, totalWeight, totalShipments, LmaDeclaration.Type.FIRST_RECEIVAL.name, status
    )
  }

  private fun setupTestData() {
    euralRepository.save(
      nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural(
        code = "010101*",
        description = "Test Eural"
      )
    )
    processingMethodRepository.save(
      nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto(
        code = "R01",
        description = "Test Method"
      )
    )

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

    val ticket = WeightTicketDto(
      id = ticketId,
      consignorParty = companyRepository.getReferenceById(consignorCompanyId),
      lines = mutableListOf(),
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
    
    // Add lines with parent reference for bidirectional relationship
    val ticketLines = lines.map { (wasteStreamNumber, weightValue) ->
      WeightTicketLineDto(
        id = UUID.randomUUID(),
        weightTicket = ticket,
        wasteStreamNumber = wasteStreamNumber,
        catalogItemId = UUID.randomUUID(),
        weightValue = weightValue.toBigDecimal(),
        weightUnit = WeightUnitDto.kg
      )
    }
    ticket.lines.addAll(ticketLines)
    
    return weightTicketRepository.save(ticket).id
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
