package nl.eazysoftware.eazyrecyclingservice.application.usecase

import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.CreateAndActivateWasteStream
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.CreateDraftWasteStream
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.PickupLocationCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.WasteStreamCommand
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.Tenant
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
  private lateinit var createDraftWasteStream: CreateDraftWasteStream

  @Autowired
  private lateinit var createAndActivateWasteStream: CreateAndActivateWasteStream

  @Autowired
  private lateinit var wasteStreamRepo: WasteStreams

  @Autowired
  private lateinit var companyRepository: CompanyJpaRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  private lateinit var testCompany: CompanyDto
  private lateinit var tenantCompany: CompanyDto

  @BeforeAll
  fun setupOnce() {
    // Execute in a separate committed transaction
    transactionTemplate.execute {
      // Create and save external processor company
      testCompany = companyRepository.save(
        TestCompanyFactory.createTestCompany(
          processorId = "12345",
          vihbId = "123456VIHB"
        )
      )
      // Create and save tenant company
      tenantCompany = companyRepository.save(
        TestCompanyFactory.createTestCompany(
          processorId = "08797",
          vihbId = "087970VIHB"
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
  fun `should generate waste stream number when processor is current tenant`() {
    // Given - using the current tenant's processor ID
    val tenantProcessorId = Tenant.processorPartyId
    val command = createTestCommand(tenantProcessorId, wasteStreamNumber = null)

    // When
    val result = createDraftWasteStream.handle(command)

    // Then - number should be auto-generated starting with tenant processor ID
    assertThat(result.wasteStreamNumber.number).startsWith(tenantProcessorId.number)
    assertTrue(wasteStreamRepo.existsById(result.wasteStreamNumber))
  }


  @Test
  fun `should generate sequential waste stream numbers`() {
    // Given - tenant processor generates numbers automatically
    val tenantProcessorId = Tenant.processorPartyId
    val command1 = createTestCommand(tenantProcessorId)
    val command2 = createTestCommand(tenantProcessorId)
    val command3 = createTestCommand(tenantProcessorId)

    // When
    val result1 = createDraftWasteStream.handle(command1)
    val result2 = createDraftWasteStream.handle(command2)
    val result3 = createDraftWasteStream.handle(command3)

    // Then
    assertEquals("087970000001", result1.wasteStreamNumber.number)
    assertEquals("087970000002", result2.wasteStreamNumber.number)
    assertEquals("087970000003", result3.wasteStreamNumber.number)
  }
  @Test
  fun `should accept provided waste stream number when processor is external`() {
    // Given - external processor (not the current tenant)
    val externalProcessorId = ProcessorPartyId("12345")
    val providedNumber = WasteStreamNumber("123450000001")
    val command = createTestCommand(externalProcessorId, wasteStreamNumber = providedNumber)

    // When
    val result = createDraftWasteStream.handle(command)

    // Then - should use the provided number
    assertEquals("123450000001", result.wasteStreamNumber.number)
    assertTrue(wasteStreamRepo.existsById(result.wasteStreamNumber))
  }

  @Test
  fun `should fail when processor is external and no waste stream number provided`() {
    // Given - external processor without waste stream number
    val externalProcessorId = ProcessorPartyId("12345")
    val command = createTestCommand(externalProcessorId, wasteStreamNumber = null)

    // When/Then - should throw IllegalArgumentException
    val exception = assertThrows<IllegalArgumentException> {
      createDraftWasteStream.handle(command)
    }
    assertThat(exception.message).contains("Afvalstroomnummer is verplicht")
  }

  @Test
  fun `should fail when waste stream number already exists - CreateDraft`() {
    // Given - create first waste stream with external processor
    val externalProcessorId = ProcessorPartyId("12345")
    val duplicateNumber = WasteStreamNumber("123450000099")
    val command1 = createTestCommand(externalProcessorId, wasteStreamNumber = duplicateNumber)
    createDraftWasteStream.handle(command1)

    // When/Then - creating second waste stream with same number should fail
    val command2 = createTestCommand(externalProcessorId, wasteStreamNumber = duplicateNumber)
    val exception = assertThrows<IllegalArgumentException> {
      createDraftWasteStream.handle(command2)
    }
    assertThat(exception.message).contains("Afvalstroom met nummer 123450000099 bestaat al")
  }

  // ==================== CreateAndActivateWasteStream Tests ====================

  @Test
  fun `CreateAndActivate - should accept provided waste stream number when processor is external`() {
    // Given - external processor with provided number
    val externalProcessorId = ProcessorPartyId("12345")
    val providedNumber = WasteStreamNumber("123450000002")
    val command = createTestCommand(externalProcessorId, wasteStreamNumber = providedNumber)

    // When
    val result = createAndActivateWasteStream.handle(command)

    // Then - should use provided number and validation should succeed (skip external validation)
    assertThat(result.wasteStreamNumber).isEqualTo("123450000002")
    assertThat(result.isValid).isTrue()
  }

  @Test
  fun `CreateAndActivate - should fail when processor is external and no waste stream number provided`() {
    // Given - external processor without waste stream number
    val externalProcessorId = ProcessorPartyId("12345")
    val command = createTestCommand(externalProcessorId, wasteStreamNumber = null)

    // When/Then - should throw IllegalArgumentException
    val exception = assertThrows<IllegalArgumentException> {
      createAndActivateWasteStream.handle(command)
    }
    assertThat(exception.message).contains("Afvalstroomnummer is verplicht")
  }

  @Test
  fun `CreateAndActivate - should fail when waste stream number already exists`() {
    // Given - create first waste stream with external processor
    val externalProcessorId = ProcessorPartyId("12345")
    val duplicateNumber = WasteStreamNumber("123450000098")
    val command1 = createTestCommand(externalProcessorId, wasteStreamNumber = duplicateNumber)
    createAndActivateWasteStream.handle(command1)

    // When/Then - creating second waste stream with same number should fail
    val command2 = createTestCommand(externalProcessorId, wasteStreamNumber = duplicateNumber)
    val exception = assertThrows<IllegalArgumentException> {
      createAndActivateWasteStream.handle(command2)
    }
    assertThat(exception.message).contains("Afvalstroom met nummer 123450000098 bestaat al")
  }


  private fun createTestCommand(
    processorId: ProcessorPartyId,
    wasteStreamNumber: WasteStreamNumber? = null
  ): WasteStreamCommand {
    return WasteStreamCommand(
      wasteStreamNumber = wasteStreamNumber,
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
