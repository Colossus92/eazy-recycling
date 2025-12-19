package nl.eazysoftware.eazyrecyclingservice.controller.weightticket

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.CancelWeightTicketRequest
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestWeightTicketFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.invoice.InvoiceJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.WeightTicketJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import java.math.BigDecimal
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class WeightTicketControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var weightTicketRepository: WeightTicketJpaRepository

    @Autowired
    private lateinit var companyRepository: CompanyJpaRepository

    @Autowired
    private lateinit var invoiceRepository: InvoiceJpaRepository

    @Autowired
    private lateinit var catalogItemRepository: CatalogItemJpaRepository

    @Autowired
    private lateinit var vatRateRepository: VatRateJpaRepository

    private lateinit var testConsignorCompany: CompanyDto
    private lateinit var testCarrierCompany: CompanyDto
    private lateinit var testCatalogItem: CatalogItemDto
    private lateinit var testVatRate: VatRateDto

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
        weightTicketRepository.deleteAll()

        // Create test companies for each test (will be rolled back after each test due to @Transactional)
        testConsignorCompany = companyRepository.save(TestCompanyFactory.createTestCompany(
            processorId = "12345",
            name = "Test Consignor Company"
        ))

        testCarrierCompany = companyRepository.save(TestCompanyFactory.createTestCompany(
            processorId = "67890",
            chamberOfCommerceId = "87654321",
            vihbId = "123456VXXX",
            name = "Test Carrier Company"
        ))

        testVatRate = vatRateRepository.save(
            VatRateDto(
                vatCode = "VAT21",
                percentage = BigDecimal("21"),
                validFrom = Instant.now().minusSeconds(86400),
                validTo = null,
                countryCode = "NL",
                description = "Standard VAT 21%",
            )
        )

        testCatalogItem = catalogItemRepository.save(
            CatalogItemDto(
                code = "WASTE001",
                name = "Test Waste Stream",
                type = CatalogItemType.WASTE_STREAM,
                unitOfMeasure = "kg",
                vatRate = testVatRate,
                category = null,
                consignorParty = null,
                defaultPrice = BigDecimal("10.00"),
                status = "ACTIVE",
                purchaseAccountNumber = "7000",
                salesAccountNumber = "8000",
            )
        )
    }

    @Test
    fun `should create weight ticket`() {
        // Given
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            carrierParty = testCarrierCompany.id,
            consignorCompanyId = testConsignorCompany.id,
            lines = listOf(
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789012",
                    weightValue = "150.75"
                )
            ),
            truckLicensePlate = "AA-123-BB",
            reclamation = "Test reclamation",
            note = "Test note"
        )

        // When & Then
        val result = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").exists())
            .andReturn()

        // Extract the generated weight ticket ID from response
        val response = objectMapper.readTree(result.response.contentAsString)
        val generatedId = response.get("id").asLong()

        // Verify weight ticket was saved in the database
        val savedWeightTicket = weightTicketRepository.findById(generatedId)
        assertThat(savedWeightTicket).isPresent
        assertThat(savedWeightTicket.get().id).isEqualTo(generatedId)
        assertThat(savedWeightTicket.get().truckLicensePlate).isEqualTo("AA-123-BB")
        assertThat(savedWeightTicket.get().reclamation).isEqualTo("Test reclamation")
        assertThat(savedWeightTicket.get().note).isEqualTo("Test note")
        assertThat(savedWeightTicket.get().lines).hasSize(1)
        assertThat(savedWeightTicket.get().lines[0].wasteStreamNumber).isEqualTo("123456789012")
        assertThat(savedWeightTicket.get().lines[0].weightValue).isEqualByComparingTo("150.75")
    }

    @Test
    fun `should create weight ticket without optional fields`() {
        // Given
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            carrierParty = null,
            consignorCompanyId = testConsignorCompany.id,
            truckLicensePlate = null,
            reclamation = null,
            note = null
        )

        // When & Then
        val result = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").exists())
            .andReturn()

        // Extract the generated weight ticket ID from response
        val response = objectMapper.readTree(result.response.contentAsString)
        val generatedId = response.get("id").asLong()

        // Verify weight ticket was saved in the database
        val savedWeightTicket = weightTicketRepository.findById(generatedId)
        assertThat(savedWeightTicket).isPresent
        assertThat(savedWeightTicket.get().id).isEqualTo(generatedId)
        assertThat(savedWeightTicket.get().truckLicensePlate).isNull()
        assertThat(savedWeightTicket.get().reclamation).isNull()
        assertThat(savedWeightTicket.get().note).isNull()
    }

    @Test
    fun `should create multiple weight tickets with sequential IDs`() {
        // Given
        val firstRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id,
            note = "First ticket"
        )

        val secondRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id,
            note = "Second ticket"
        )

        // When - create first weight ticket
        val result1 = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(firstRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val id1 = objectMapper.readTree(result1.response.contentAsString).get("id").asLong()

        // When - create second weight ticket
        val result2 = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(secondRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val id2 = objectMapper.readTree(result2.response.contentAsString).get("id").asLong()

        // Then - IDs should be different and sequential
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id2).isGreaterThan(id1)
    }

    @Test
    fun `should create weight ticket with multiple lines`() {
        // Given
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id,
            lines = listOf(
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789011",
                    weightValue = "100.00"
                ),
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789012",
                    weightValue = "250.50"
                ),
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789013",
                    weightValue = "75.25"
                )
            ),
            note = "Multiple lines test"
        )

        // When & Then
        val result = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val generatedId = objectMapper.readTree(result.response.contentAsString).get("id").asLong()

        // Verify all lines were saved
        val savedWeightTicket = weightTicketRepository.findById(generatedId)
        assertThat(savedWeightTicket).isPresent
        assertThat(savedWeightTicket.get().lines).hasSize(3)
        assertThat(savedWeightTicket.get().lines[0].wasteStreamNumber).isEqualTo("123456789011")
        assertThat(savedWeightTicket.get().lines[0].weightValue).isEqualByComparingTo("100.00")
        assertThat(savedWeightTicket.get().lines[1].wasteStreamNumber).isEqualTo("123456789012")
        assertThat(savedWeightTicket.get().lines[1].weightValue).isEqualByComparingTo("250.50")
        assertThat(savedWeightTicket.get().lines[2].wasteStreamNumber).isEqualTo("123456789013")
        assertThat(savedWeightTicket.get().lines[2].weightValue).isEqualByComparingTo("75.25")
    }

    @Test
    fun `can get weight ticket by id with full details`() {
        // Given - create weight ticket and extract generated ID
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            carrierParty = testCarrierCompany.id,
            consignorCompanyId = testConsignorCompany.id,
            truckLicensePlate = "CC-789-DD",
            reclamation = "Detail test reclamation",
            note = "Detail test note"
        )
        val createResult = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val weightTicketId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        // When & Then
        securedMockMvc.get("/weight-tickets/${weightTicketId}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(weightTicketId))
            .andExpect(jsonPath("$.truckLicensePlate").value("CC-789-DD"))
            .andExpect(jsonPath("$.reclamation").value("Detail test reclamation"))
            .andExpect(jsonPath("$.note").value("Detail test note"))
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.consignorParty").exists())
            .andExpect(jsonPath("$.carrierParty").exists())
    }

    @Test
    fun `can get all weight tickets`() {
        // Given - create multiple weight tickets
        val firstRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id,
            note = "First ticket for list"
        )
        val secondRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id,
            note = "Second ticket for list"
        )

        securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(firstRequest)
        ).andExpect(status().isCreated)

        securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(secondRequest)
        ).andExpect(status().isCreated)

        // When & Then
        securedMockMvc.get("/weight-tickets")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[1].id").exists())
    }

    @Test
    fun `can update created weight ticket`() {
        // Given - create weight ticket and extract generated ID
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            carrierParty = testCarrierCompany.id,
            consignorCompanyId = testConsignorCompany.id,
            lines = listOf(
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789012",
                    weightValue = "100.00"
                )
            ),
            truckLicensePlate = "EE-111-FF",
            note = "Original note"
        )
        val createResult = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val weightTicketId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        // When - update the weight ticket with new lines
        val updatedRequest = weightTicketRequest.copy(
            lines = listOf(
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789012",
                    weightValue = "200.00"
                ),
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789013",
                    weightValue = "300.00"
                )
            ),
            truckLicensePlate = "GG-222-HH",
            note = "Updated note"
        )

        securedMockMvc.put(
            "/weight-tickets/${weightTicketId}",
            objectMapper.writeValueAsString(updatedRequest)
        )
            .andExpect(status().isNoContent)

        // Then - verify the update was applied including lines
        val updatedWeightTicket = weightTicketRepository.findById(weightTicketId)
        assertThat(updatedWeightTicket).isPresent
        assertThat(updatedWeightTicket.get().truckLicensePlate).isEqualTo("GG-222-HH")
        assertThat(updatedWeightTicket.get().note).isEqualTo("Updated note")
        assertThat(updatedWeightTicket.get().lines).hasSize(2)
        assertThat(updatedWeightTicket.get().lines[0].wasteStreamNumber).isEqualTo("123456789012")
        assertThat(updatedWeightTicket.get().lines[0].weightValue).isEqualTo("200.00")
        assertThat(updatedWeightTicket.get().lines[1].wasteStreamNumber).isEqualTo("123456789013")
      assertThat(updatedWeightTicket.get().lines[1].weightValue).isEqualTo("300.00")
    }

    @Test
    fun `can delete created weight ticket`() {
        // Given - create weight ticket and extract generated ID
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id,
            note = "To be deleted"
        )
        val createResult = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val weightTicketId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        val cancelWeightTicketRequest = CancelWeightTicketRequest(
          cancellationReason = "Good Reason"
        )

        // When - delete the weight ticket (soft delete)
        securedMockMvc.post(
          "/weight-tickets/${weightTicketId}/cancel",
          objectMapper.writeValueAsString(cancelWeightTicketRequest)
        )
            .andExpect(status().isNoContent)

        // Then - verify the weight ticket status is set to CANCELLED
        val deletedWeightTicket = weightTicketRepository.findById(weightTicketId)
        assertThat(deletedWeightTicket).isPresent
        assertThat(deletedWeightTicket.get().status.name).isEqualTo("CANCELLED")
        assertThat(deletedWeightTicket.get().cancellationReason).isEqualTo("Good Reason")
    }

    @Test
    fun `can complete weight ticket with draft status and lines`() {
        // Given - create weight ticket with lines
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id,
            lines = listOf(
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789012",
                    weightValue = "150.75"
                )
            ),
            note = "To be completed"
        )
        val createResult = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val weightTicketId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        // Verify initial status is DRAFT
        val draftWeightTicket = weightTicketRepository.findById(weightTicketId)
        assertThat(draftWeightTicket).isPresent
        assertThat(draftWeightTicket.get().status.name).isEqualTo("DRAFT")

        // When - complete the weight ticket
        securedMockMvc.post("/weight-tickets/${weightTicketId}/complete", "")
            .andExpect(status().isNoContent)

        // Then - verify the weight ticket status is set to COMPLETED
        val completedWeightTicket = weightTicketRepository.findById(weightTicketId)
        assertThat(completedWeightTicket).isPresent
        assertThat(completedWeightTicket.get().status.name).isEqualTo("COMPLETED")
    }

    @Test
    fun `can create invoice from completed weight ticket`() {
        // Given - create and complete a weight ticket
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id,
            lines = listOf(
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789012",
                    weightValue = "150.75",
                    catalogItemId = testCatalogItem.id!!
                )
            ),
            note = "Weight ticket for invoice"
        )
        val createResult = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val weightTicketId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        // Complete the weight ticket
        securedMockMvc.post("/weight-tickets/${weightTicketId}/complete", "")
            .andExpect(status().isNoContent)

        // When - create invoice from weight ticket
        val invoiceResult = securedMockMvc.post("/weight-tickets/${weightTicketId}/invoice", "")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.invoiceId").exists())
            .andExpect(jsonPath("$.weightTicketId").value(weightTicketId))
            .andReturn()

        val response = objectMapper.readTree(invoiceResult.response.contentAsString)
        val invoiceId = response.get("invoiceId").asLong()

        // Then - verify invoice was created with correct data
        val savedInvoice = invoiceRepository.findById(invoiceId)
        assertThat(savedInvoice).isPresent
        assertThat(savedInvoice.get().sourceWeightTicketId).isEqualTo(weightTicketId)
        assertThat(savedInvoice.get().customerName).isEqualTo("Test Consignor Company")
        assertThat(savedInvoice.get().status.name).isEqualTo("DRAFT")

        // Verify weight ticket was updated
        val updatedWeightTicket = weightTicketRepository.findById(weightTicketId)
        assertThat(updatedWeightTicket).isPresent
        assertThat(updatedWeightTicket.get().linkedInvoiceId).isEqualTo(invoiceId)
        assertThat(updatedWeightTicket.get().status.name).isEqualTo("INVOICED")
    }

    @Test
    fun `cannot create invoice from draft weight ticket`() {
        // Given - create a weight ticket (still in DRAFT status)
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id,
            lines = listOf(
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789012",
                    weightValue = "100.00",
                    catalogItemId = testCatalogItem.id!!
                )
            )
        )
        val createResult = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val weightTicketId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        // When & Then - attempt to create invoice should fail
        securedMockMvc.post("/weight-tickets/${weightTicketId}/invoice", "")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `cannot create invoice from weight ticket that already has linked invoice`() {
        // Given - create, complete, and invoice a weight ticket
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id,
            lines = listOf(
                TestWeightTicketFactory.createTestWeightTicketLine(
                    wasteStreamNumber = "123456789012",
                    weightValue = "200.00",
                    catalogItemId = testCatalogItem.id!!
                )
            )
        )
        val createResult = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val weightTicketId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        // Complete and create first invoice
        securedMockMvc.post("/weight-tickets/${weightTicketId}/complete", "")
            .andExpect(status().isNoContent)
        securedMockMvc.post("/weight-tickets/${weightTicketId}/invoice", "")
            .andExpect(status().isCreated)

        // When & Then - attempt to create second invoice should fail
        securedMockMvc.post("/weight-tickets/${weightTicketId}/invoice", "")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `cannot create invoice from non-existent weight ticket`() {
        // When & Then
        securedMockMvc.post("/weight-tickets/999999/invoice", "")
            .andExpect(status().isNotFound)
    }
}
