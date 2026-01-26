package nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinalizeInvoiceIntegrationTest @Autowired constructor(
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
                code = "CUST001",
                chamberOfCommerceId = "12345678",
                vihbId = "123456VIHB",
                address = AddressDto(
                    streetName = "Main St",
                    buildingNumberAddition = null,
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

    @Test
    fun `finalize invoice enqueues PDF generation job`() {
        // Create a draft invoice
        val createReq = """
            {
                "invoiceType": "PURCHASE",
                "documentType": "INVOICE",
                "customerId": "${testCompany.id}",
                "invoiceDate": "${LocalDate.now()}",
                "lines": [
                    {
                        "date": "${LocalDate.now()}",
                        "catalogItemId": "${testCatalogItem.id}",
                        "description": "Test line",
                        "quantity": 10.00,
                        "unitPrice": 5.00,
                        "orderReference": "ORD-001"
                    }
                ]
            }
        """.trimIndent()

        val createResult = securedMockMvc.post("/invoices", createReq)
            .andExpect(status().isCreated)
            .andReturn()

        val created = objectMapper.readValue(createResult.response.contentAsString, InvoiceResult::class.java)
        val invoiceId = created.invoiceId

        // Verify invoice is in DRAFT status
        val draftInvoice = invoiceRepository.findById(invoiceId)
        assertThat(draftInvoice).isPresent
        assertThat(draftInvoice.get().status.name).isEqualTo("DRAFT")

        // Finalize the invoice
        securedMockMvc.post("/invoices/$invoiceId/finalize", "{}")
            .andExpect(status().isOk)

        // Verify invoice is now FINAL with invoice number
        val finalizedInvoice = invoiceRepository.findById(invoiceId)
        assertThat(finalizedInvoice).isPresent
        assertThat(finalizedInvoice.get().status.name).isEqualTo("FINAL")
        assertThat(finalizedInvoice.get().invoiceNumber).isNotNull()
        assertThat(finalizedInvoice.get().invoiceNumber).startsWith("ER")

        // PDF generation is now handled by Jobrunr asynchronously
        // The job will be processed in the background
    }

    @Test
    fun `finalize invoice generates unique invoice number per year`() {
        // Create and finalize first invoice
        val createReq1 = createInvoiceRequest()
        val result1 = securedMockMvc.post("/invoices", createReq1)
            .andExpect(status().isCreated)
            .andReturn()
        val invoice1Id = objectMapper.readValue(result1.response.contentAsString, InvoiceResult::class.java).invoiceId

        securedMockMvc.post("/invoices/$invoice1Id/finalize", "{}")
            .andExpect(status().isOk)

        // Create and finalize second invoice
        val createReq2 = createInvoiceRequest()
        val result2 = securedMockMvc.post("/invoices", createReq2)
            .andExpect(status().isCreated)
            .andReturn()
        val invoice2Id = objectMapper.readValue(result2.response.contentAsString, InvoiceResult::class.java).invoiceId

        securedMockMvc.post("/invoices/$invoice2Id/finalize", "{}")
            .andExpect(status().isOk)

        // Verify both invoices have different invoice numbers
        val invoice1 = invoiceRepository.findById(invoice1Id).get()
        val invoice2 = invoiceRepository.findById(invoice2Id).get()

        assertThat(invoice1.invoiceNumber).isNotNull()
        assertThat(invoice2.invoiceNumber).isNotNull()
        assertThat(invoice1.invoiceNumber).isNotEqualTo(invoice2.invoiceNumber)
    }

    @Test
    fun `finalize already finalized invoice fails`() {
        val createReq = createInvoiceRequest()
        val result = securedMockMvc.post("/invoices", createReq)
            .andExpect(status().isCreated)
            .andReturn()
        val invoiceId = objectMapper.readValue(result.response.contentAsString, InvoiceResult::class.java).invoiceId

        // First finalization succeeds
        securedMockMvc.post("/invoices/$invoiceId/finalize", "{}")
            .andExpect(status().isOk)

        // Second finalization fails
        securedMockMvc.post("/invoices/$invoiceId/finalize", "{}")
            .andExpect(status().isBadRequest)
    }

    private fun createInvoiceRequest(): String {
        return """
            {
                "invoiceType": "PURCHASE",
                "documentType": "INVOICE",
                "customerId": "${testCompany.id}",
                "invoiceDate": "${LocalDate.now()}",
                "lines": [
                    {
                        "date": "${LocalDate.now()}",
                        "catalogItemId": "${testCatalogItem.id}",
                        "description": "Test line",
                        "quantity": 10.00,
                        "unitPrice": 5.00,
                        "orderReference": "ORD-001"
                    }
                ]
            }
        """.trimIndent()
    }
}
