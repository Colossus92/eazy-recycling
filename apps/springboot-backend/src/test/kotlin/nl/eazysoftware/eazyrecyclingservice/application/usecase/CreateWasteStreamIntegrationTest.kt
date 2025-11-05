package nl.eazysoftware.eazyrecyclingservice.application.usecase

import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.CreateWasteStream
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.PickupLocationCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.WasteStreamCommand
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateWasteStreamIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var createWasteStream: CreateWasteStream

  @Autowired
  private lateinit var wasteStreamRepo: WasteStreams

  @Autowired
  private lateinit var companyRepository: CompanyJpaRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  private lateinit var testCompany: CompanyDto

  @BeforeAll
  fun setupOnce() {
    // Execute in a separate committed transaction
    transactionTemplate.execute {
      // Create and save company fresh (not detached)
      testCompany = companyRepository.save(
        TestCompanyFactory.createTestCompany(
          processorId = "12345"
        )
      )
    }
  }

  @AfterAll
  fun cleanupOnce() {
    companyRepository.deleteAll()
  }


  @BeforeEach
  fun setup() {
    wasteStreamRepo.deleteAll()
  }

  @Test
  fun `should generate first waste stream number for processor`() {
    // Given
    val processorId = ProcessorPartyId("12345")
    val command = createTestCommand(processorId)

    // When
    val result = createWasteStream.handle(command)

    // Then
    assertEquals("123450000001", result.wasteStreamNumber.number)
    assertTrue(wasteStreamRepo.existsById(result.wasteStreamNumber))
  }

  @Test
  fun `should generate sequential waste stream numbers`() {
    // Given
    val processorId = ProcessorPartyId("12345")
    val command1 = createTestCommand(processorId)
    val command2 = createTestCommand(processorId)
    val command3 = createTestCommand(processorId)

    // When
    val result1 = createWasteStream.handle(command1)
    val result2 = createWasteStream.handle(command2)
    val result3 = createWasteStream.handle(command3)

    // Then
    assertEquals("123450000001", result1.wasteStreamNumber.number)
    assertEquals("123450000002", result2.wasteStreamNumber.number)
    assertEquals("123450000003", result3.wasteStreamNumber.number)
  }

  @Test
  fun `should retrieve highest number correctly from database`() {
    // Given
    val processorId = ProcessorPartyId("12345")

    // Create waste streams with gaps in numbering
    val command1 = createTestCommand(processorId)
    createWasteStream.handle(command1) // 999990000001

    val command2 = createTestCommand(processorId)
    createWasteStream.handle(command2) // 999990000002

    val command3 = createTestCommand(processorId)
    createWasteStream.handle(command3) // 999990000003

    // When - verify next number after existing sequence
    val command4 = createTestCommand(processorId)
    val result = createWasteStream.handle(command4)

    // Then
    assertEquals("123450000004", result.wasteStreamNumber.number)
  }

  @Test
  fun `should persist waste stream with generated number`() {
    // Given
    val processorId = ProcessorPartyId("12345")
    val command = createTestCommand(processorId)

    // When
    val result = createWasteStream.handle(command)

    // Then
    val persisted = wasteStreamRepo.findByNumber(result.wasteStreamNumber)
    assertNotNull(persisted)
    assertEquals("123450000001", persisted?.wasteStreamNumber?.number)
    assertEquals(command.wasteType.name, persisted?.wasteType?.name)
  }

  private fun createTestCommand(processorId: ProcessorPartyId): WasteStreamCommand {
    return WasteStreamCommand(
      wasteType = WasteType(
          name = "Test Waste",
          euralCode = EuralCode("010101"),
          processingMethod = ProcessingMethod("R01")
      ),
      collectionType = WasteCollectionType.DEFAULT,
      pickupLocation = PickupLocationCommand.DutchAddressCommand(
            streetName = "Test Street",
            postalCode = "1234AB",
            buildingNumber = "1",
            city = "Test City",
      ),
      deliveryLocation = WasteDeliveryLocation(processorPartyId = processorId),
      consignorParty = Consignor.Company(CompanyId(UUID.randomUUID())),
      pickupParty = CompanyId(UUID.randomUUID()),
      dealerParty = null,
      collectorParty = null,
      brokerParty = null,
      consignorClassification = 1,
    )
  }
}
