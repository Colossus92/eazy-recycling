package nl.eazysoftware.eazyrecyclingservice.controller.invoice

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice.InvoiceResult
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.invoice.InvoiceJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvoiceControllerIntegrationTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val invoiceRepository: InvoiceJpaRepository,
    val companyRepository: CompanyJpaRepository,
    val catalogItemRepository: CatalogItemJpaRepository,
    val vatRateRepository: VatRateJpaRepository,
) : BaseIntegrationTest() {

    private lateinit var securedMockMvc: SecuredMockMvc
    private lateinit var testCompany: CompanyDto
    private lateinit var testCatalogItem: CatalogItemDto
    private lateinit var testVatRate: VatRateDto

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
        invoiceRepository.deleteAll()

        testCompany = companyRepository.save(
            CompanyDto(
                id = UUID.randomUUID(),
                name = "Test Customer BV",
                chamberOfCommerceId = "12345678",
                vihbId = "123456VIHB",
                address = AddressDto(
                    streetName = "Main St",
                    buildingNumberAddition = "HQ",
                    buildingNumber = "1",
                    postalCode = "1234 AB",
                    city = "Amsterdam",
                    country = "Nederland"
                )
            )
        )

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
                id = UUID.randomUUID(),
                code = "MAT001",
                name = "Test Material",
                type = CatalogItemType.MATERIAL,
                unitOfMeasure = "kg",
                vatRate = testVatRate,
                category = null,
                consignorParty = null,
                defaultPrice = null,
                status = "ACTIVE",
                purchaseAccountNumber = "7000",
                salesAccountNumber = "8000",
            )
        )
    }

    private fun createInvoiceRequest(
        invoiceType: String = "SALE",
        documentType: String = "INVOICE",
        customerId: String = testCompany.id.toString(),
        invoiceDate: LocalDate = LocalDate.now(),
        lines: List<InvoiceController.InvoiceLineRequest> = listOf(
            InvoiceController.InvoiceLineRequest(
                date = LocalDate.now(),
                catalogItemId = testCatalogItem.id,
                description = "Test line",
                quantity = BigDecimal("10.00"),
                unitPrice = BigDecimal("5.00"),
                orderReference = "ORD-001",
            )
        )
    ) = InvoiceController.CreateInvoiceRequest(
        invoiceType = invoiceType,
        documentType = documentType,
        customerId = customerId,
        invoiceDate = invoiceDate,
        originalInvoiceId = null,
        lines = lines,
    )

    @Test
    fun `create invoice - success`() {
        val req = createInvoiceRequest()

        val mvcResult = securedMockMvc.post(
            "/invoices",
            objectMapper.writeValueAsString(req)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = objectMapper.readValue(mvcResult.response.contentAsString, InvoiceResult::class.java)
        assertThat(created.invoiceId).isNotNull()

        val invoice = invoiceRepository.findById(created.invoiceId)
        assertThat(invoice).isPresent
        assertThat(invoice.get().status.name).isEqualTo("DRAFT")
        assertThat(invoice.get().customerName).isEqualTo("Test Customer BV")
    }

    @Test
    fun `get all invoices - returns list`() {
        val req = createInvoiceRequest()
        securedMockMvc.post("/invoices", objectMapper.writeValueAsString(req))
            .andExpect(status().isCreated)

        securedMockMvc.get("/invoices")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].customerName").value("Test Customer BV"))
            .andExpect(jsonPath("$[0].status").value("DRAFT"))
    }

    @Test
    fun `get invoice by id - success`() {
        val req = createInvoiceRequest()
        val mvcResult = securedMockMvc.post("/invoices", objectMapper.writeValueAsString(req))
            .andReturn()
        val created = objectMapper.readValue(mvcResult.response.contentAsString, InvoiceResult::class.java)

        securedMockMvc.get("/invoices/${created.invoiceId}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(created.invoiceId.toString()))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.customer.name").value("Test Customer BV"))
            .andExpect(jsonPath("$.lines.length()").value(1))
    }

    @Test
    fun `get invoice by id - not found returns 404`() {
        securedMockMvc.get("/invoices/${UUID.randomUUID()}")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `update invoice - success`() {
        val req = createInvoiceRequest()
        val mvcResult = securedMockMvc.post("/invoices", objectMapper.writeValueAsString(req))
            .andReturn()
        val created = objectMapper.readValue(mvcResult.response.contentAsString, InvoiceResult::class.java)

        val updateReq = InvoiceController.UpdateInvoiceRequest(
            invoiceDate = LocalDate.now().plusDays(1),
            lines = listOf(
                InvoiceController.InvoiceLineRequest(
                    date = LocalDate.now(),
                    catalogItemId = testCatalogItem.id,
                    description = "Updated line",
                    quantity = BigDecimal("20.00"),
                    unitPrice = BigDecimal("10.00"),
                    orderReference = "ORD-002",
                )
            )
        )

        securedMockMvc.put(
            "/invoices/${created.invoiceId}",
            objectMapper.writeValueAsString(updateReq)
        )
            .andExpect(status().isOk)

        securedMockMvc.get("/invoices/${created.invoiceId}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lines[0].quantity").value(20.00))
    }

    @Test
    fun `delete invoice - success`() {
        val req = createInvoiceRequest()
        val mvcResult = securedMockMvc.post("/invoices", objectMapper.writeValueAsString(req))
            .andReturn()
        val created = objectMapper.readValue(mvcResult.response.contentAsString, InvoiceResult::class.java)

        securedMockMvc.delete("/invoices/${created.invoiceId}")
            .andExpect(status().isNoContent)

        securedMockMvc.get("/invoices/${created.invoiceId}")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create invoice with multiple lines - calculates totals correctly`() {
        val req = createInvoiceRequest(
            lines = listOf(
                InvoiceController.InvoiceLineRequest(
                    date = LocalDate.now(),
                    catalogItemId = testCatalogItem.id,
                    description = "Line 1",
                    quantity = BigDecimal("10.00"),
                    unitPrice = BigDecimal("5.00"),
                    orderReference = null,
                ),
                InvoiceController.InvoiceLineRequest(
                    date = LocalDate.now(),
                    catalogItemId = testCatalogItem.id,
                    description = "Line 2",
                    quantity = BigDecimal("5.00"),
                    unitPrice = BigDecimal("20.00"),
                    orderReference = null,
                )
            )
        )

        val mvcResult = securedMockMvc.post("/invoices", objectMapper.writeValueAsString(req))
            .andExpect(status().isCreated)
            .andReturn()
        val created = objectMapper.readValue(mvcResult.response.contentAsString, InvoiceResult::class.java)

        securedMockMvc.get("/invoices/${created.invoiceId}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lines.length()").value(2))
            .andExpect(jsonPath("$.totals.totalExclVat").value(150.00))
            .andExpect(jsonPath("$.totals.totalVat").value(31.50))
            .andExpect(jsonPath("$.totals.totalInclVat").value(181.50))
    }
}
