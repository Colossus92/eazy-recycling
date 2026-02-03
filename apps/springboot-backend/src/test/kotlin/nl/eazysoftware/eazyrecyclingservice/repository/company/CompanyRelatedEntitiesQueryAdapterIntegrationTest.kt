package nl.eazysoftware.eazyrecyclingservice.repository.company

import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceDocumentType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceStatus
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceType
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TimingMode
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamStatus
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketDirection
import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TimingConstraintDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.invoice.InvoiceDto
import nl.eazysoftware.eazyrecyclingservice.repository.invoice.InvoiceJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.invoice.InvoiceLineDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class CompanyRelatedEntitiesQueryAdapterIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var wasteStreamsByCompanyQueryAdapter: WasteStreamsByCompanyQueryAdapter

    @Autowired
    private lateinit var weightTicketsByCompanyQueryAdapter: WeightTicketsByCompanyQueryAdapter

    @Autowired
    private lateinit var transportsByCompanyQueryAdapter: TransportsByCompanyQueryAdapter

    @Autowired
    private lateinit var invoicesByCompanyQueryAdapter: InvoicesByCompanyQueryAdapter

    @Autowired
    private lateinit var companyRepository: CompanyJpaRepository

    @Autowired
    private lateinit var wasteStreamRepository: WasteStreamJpaRepository

    @Autowired
    private lateinit var weightTicketRepository: WeightTicketJpaRepository

    @Autowired
    private lateinit var transportRepository: TransportRepository

    @Autowired
    private lateinit var invoiceRepository: InvoiceJpaRepository

    @Autowired
    private lateinit var catalogItemRepository: CatalogItemJpaRepository

    @Autowired
    private lateinit var vatRateRepository: VatRateJpaRepository

    @Autowired
    private lateinit var euralRepository: EuralRepository

    @Autowired
    private lateinit var processingMethodRepository: ProcessingMethodRepository

    private lateinit var testCompany: CompanyDto
    private lateinit var otherCompany: CompanyDto
    private lateinit var processorCompany: CompanyDto
    private lateinit var testCatalogItem: CatalogItemDto
    private lateinit var testVatRate: VatRateDto
    private lateinit var testEural: Eural
    private lateinit var testProcessingMethod: ProcessingMethodDto

    @BeforeEach
    fun setup() {
        invoiceRepository.deleteAll()
        transportRepository.deleteAll()
        weightTicketRepository.deleteAll()
        wasteStreamRepository.deleteAll()

        testCompany = companyRepository.save(
            TestCompanyFactory.createTestCompany(
                processorId = "11111",
                chamberOfCommerceId = "11111111",
                vihbId = "111111VIHB",
                name = "Test Consignor Company"
            )
        )

        otherCompany = companyRepository.save(
            TestCompanyFactory.createTestCompany(
                processorId = "22222",
                chamberOfCommerceId = "22222222",
                vihbId = "222222VIHB",
                name = "Other Company"
            )
        )

        processorCompany = companyRepository.save(
            TestCompanyFactory.createTestCompany(
                processorId = "08797",
                chamberOfCommerceId = "08797000",
                vihbId = "087970VIHB",
                name = "Processor Company"
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

        testEural = euralRepository.findById("16 01 17").orElseGet {
            euralRepository.save(Eural(code = "16 01 17", description = "Paper and cardboard"))
        }

        testProcessingMethod = processingMethodRepository.findById("A.01").orElseGet {
            processingMethodRepository.save(ProcessingMethodDto(code = "A.01", description = "Recycling"))
        }
    }

    @Nested
    inner class GetWasteStreamsByCompanyTests {

        @Test
        fun `returns empty list when company has no waste streams`() {
            val result = wasteStreamsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).isEmpty()
        }

        @Test
        fun `returns waste streams for company`() {
            val pickupLocation = createTestPickupLocation()
            wasteStreamRepository.save(
                WasteStreamDto(
                    number = "111110000001",
                    name = "Glass Waste",
                    euralCode = testEural,
                    processingMethodCode = testProcessingMethod,
                    wasteCollectionType = "DEFAULT",
                    pickupLocation = pickupLocation,
                    processorParty = processorCompany,
                    consignorParty = testCompany,
                    consignorClassification = 1,
                    pickupParty = testCompany,
                    dealerParty = null,
                    collectorParty = null,
                    brokerParty = null,
                    catalogItem = testCatalogItem,
                    status = WasteStreamStatus.DRAFT.name
                )
            )

            val result = wasteStreamsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].wasteStreamNumber).isEqualTo("111110000001")
            assertThat(result[0].wasteName).isEqualTo("Glass Waste")
            assertThat(result[0].pickupLocation).contains("Test Street")
            assertThat(result[0].status).isEqualTo("DRAFT")
        }

        @Test
        fun `returns only waste streams for specified company`() {
            val pickupLocation1 = createTestPickupLocation()
            wasteStreamRepository.save(
                WasteStreamDto(
                    number = "111110000002",
                    name = "Glass Waste",
                    euralCode = testEural,
                    processingMethodCode = testProcessingMethod,
                    wasteCollectionType = "DEFAULT",
                    pickupLocation = pickupLocation1,
                    processorParty = processorCompany,
                    consignorParty = testCompany,
                    consignorClassification = 1,
                    pickupParty = testCompany,
                    dealerParty = null,
                    collectorParty = null,
                    brokerParty = null,
                    catalogItem = testCatalogItem,
                    status = WasteStreamStatus.DRAFT.name
                )
            )

            val pickupLocation2 = createTestPickupLocation()
            wasteStreamRepository.save(
                WasteStreamDto(
                    number = "222220000001",
                    name = "Plastic Waste",
                    euralCode = testEural,
                    processingMethodCode = testProcessingMethod,
                    wasteCollectionType = "DEFAULT",
                    pickupLocation = pickupLocation2,
                    processorParty = processorCompany,
                    consignorParty = otherCompany,
                    consignorClassification = 1,
                    pickupParty = otherCompany,
                    dealerParty = null,
                    collectorParty = null,
                    brokerParty = null,
                    catalogItem = testCatalogItem,
                    status = WasteStreamStatus.DRAFT.name
                )
            )

            val result = wasteStreamsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].wasteStreamNumber).isEqualTo("111110000002")
        }

        @Test
        fun `formats pickup location correctly for Dutch address`() {
            val pickupLocation = PickupLocationDto.DutchAddressDto(
                streetName = "Main Street",
                buildingNumber = "42",
                buildingNumberAddition = "A",
                postalCode = "1234AB",
                city = "Amsterdam",
                country = "Netherlands"
            )

            wasteStreamRepository.save(
                WasteStreamDto(
                    number = "111110000003",
                    name = "Test Waste",
                    euralCode = testEural,
                    processingMethodCode = testProcessingMethod,
                    wasteCollectionType = "DEFAULT",
                    pickupLocation = pickupLocation,
                    processorParty = processorCompany,
                    consignorParty = testCompany,
                    consignorClassification = 1,
                    pickupParty = testCompany,
                    dealerParty = null,
                    collectorParty = null,
                    brokerParty = null,
                    catalogItem = testCatalogItem,
                    status = WasteStreamStatus.DRAFT.name
                )
            )

            val result = wasteStreamsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].pickupLocation).isEqualTo("Main Street 42, Amsterdam")
        }

        @Test
        fun `formats pickup location correctly for company location`() {
            val pickupLocation = PickupLocationDto.PickupCompanyDto(
                company = testCompany,
                name = testCompany.name,
                streetName = "Test Street",
                buildingNumber = "1",
                buildingNumberAddition = null,
                postalCode = "1234AB",
                city = "Test City",
                country = "Netherlands"
            )

            wasteStreamRepository.save(
                WasteStreamDto(
                    number = "111110000004",
                    name = "Test Waste",
                    euralCode = testEural,
                    processingMethodCode = testProcessingMethod,
                    wasteCollectionType = "DEFAULT",
                    pickupLocation = pickupLocation,
                    processorParty = processorCompany,
                    consignorParty = testCompany,
                    consignorClassification = 1,
                    pickupParty = testCompany,
                    dealerParty = null,
                    collectorParty = null,
                    brokerParty = null,
                    catalogItem = testCatalogItem,
                    status = WasteStreamStatus.DRAFT.name
                )
            )

            val result = wasteStreamsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].pickupLocation).contains("Test Consignor Company")
        }
    }

    @Nested
    inner class GetWeightTicketsByCompanyTests {

        @Test
        fun `returns empty list when company has no weight tickets`() {
            val result = weightTicketsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).isEmpty()
        }

        @Test
        fun `returns weight tickets for company`() {
            val pickupLocation = createTestPickupLocation()
            weightTicketRepository.save(
                WeightTicketDto(
                    number = 1001L,
                    consignorParty = testCompany,
                    carrierParty = null,
                    pickupLocation = pickupLocation,
                    deliveryLocation = null,
                    direction = WeightTicketDirection.INBOUND,
                    truckLicensePlate = "AA-123-BB",
                    reclamation = null,
                    note = "Test note",
                    status = WeightTicketStatusDto.DRAFT,
                    weightedAt = Instant.now(),
                    secondWeighingValue = null,
                    secondWeighingUnit = null,
                    tarraWeightValue = null,
                    tarraWeightUnit = null,
                    cancellationReason = null,
                    linkedInvoiceId = null
                )
            )

            val result = weightTicketsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(1001L)
            assertThat(result[0].status).isEqualTo("DRAFT")
            assertThat(result[0].pickupLocation).contains("Test Street")
        }

        @Test
        fun `returns total weight from weight ticket lines`() {
            val pickupLocation = createTestPickupLocation()
            val weightTicket = WeightTicketDto(
                number = 1002L,
                consignorParty = testCompany,
                carrierParty = null,
                pickupLocation = pickupLocation,
                deliveryLocation = null,
                direction = WeightTicketDirection.INBOUND,
                truckLicensePlate = null,
                reclamation = null,
                note = null,
                status = WeightTicketStatusDto.DRAFT,
                weightedAt = Instant.now(),
                secondWeighingValue = null,
                secondWeighingUnit = null,
                tarraWeightValue = null,
                tarraWeightUnit = null,
                cancellationReason = null,
                linkedInvoiceId = null
            )

            weightTicket.lines.add(
                WeightTicketLineDto(
                    id = UUID.randomUUID(),
                    weightTicket = weightTicket,
                    wasteStreamNumber = "111110000001",
                    weightValue = BigDecimal("100.50"),
                    weightUnit = WeightUnitDto.kg,
                    catalogItem = testCatalogItem,
                    catalogItemId = testCatalogItem.id
                )
            )
            weightTicket.lines.add(
                WeightTicketLineDto(
                    id = UUID.randomUUID(),
                    weightTicket = weightTicket,
                    wasteStreamNumber = "111110000002",
                    weightValue = BigDecimal("50.25"),
                    weightUnit = WeightUnitDto.kg,
                    catalogItem = testCatalogItem,
                    catalogItemId = testCatalogItem.id
                )
            )

            weightTicketRepository.save(weightTicket)

            val result = weightTicketsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].totalWeight).isEqualTo(150.75)
        }

        @Test
        fun `returns only weight tickets for specified company`() {
            val pickupLocation1 = createTestPickupLocation()
            weightTicketRepository.save(
                WeightTicketDto(
                    number = 1003L,
                    consignorParty = testCompany,
                    carrierParty = null,
                    pickupLocation = pickupLocation1,
                    deliveryLocation = null,
                    direction = WeightTicketDirection.INBOUND,
                    truckLicensePlate = null,
                    reclamation = null,
                    note = null,
                    status = WeightTicketStatusDto.DRAFT,
                    weightedAt = null,
                    secondWeighingValue = null,
                    secondWeighingUnit = null,
                    tarraWeightValue = null,
                    tarraWeightUnit = null,
                    cancellationReason = null,
                    linkedInvoiceId = null
                )
            )

            val pickupLocation2 = createTestPickupLocation()
            weightTicketRepository.save(
                WeightTicketDto(
                    number = 1004L,
                    consignorParty = otherCompany,
                    carrierParty = null,
                    pickupLocation = pickupLocation2,
                    deliveryLocation = null,
                    direction = WeightTicketDirection.INBOUND,
                    truckLicensePlate = null,
                    reclamation = null,
                    note = null,
                    status = WeightTicketStatusDto.DRAFT,
                    weightedAt = null,
                    secondWeighingValue = null,
                    secondWeighingUnit = null,
                    tarraWeightValue = null,
                    tarraWeightUnit = null,
                    cancellationReason = null,
                    linkedInvoiceId = null
                )
            )

            val result = weightTicketsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(1003L)
        }
    }

    @Nested
    inner class GetTransportsByCompanyTests {

        @Test
        fun `returns empty list when company has no transports`() {
            val result = transportsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).isEmpty()
        }

        @Test
        fun `returns transports for company`() {
            val pickupLocation = createTestPickupLocation()
            val deliveryLocation = createTestPickupLocation()

            transportRepository.save(
                TransportDto(
                    id = UUID.randomUUID(),
                    displayNumber = "25-000001",
                    consignorParty = testCompany,
                    carrierParty = processorCompany,
                    pickupLocation = pickupLocation,
                    pickupTiming = TimingConstraintDto(
                        date = LocalDate.of(2025, 2, 15),
                        mode = TimingMode.FIXED,
                        windowStart = LocalTime.of(10, 0),
                        windowEnd = LocalTime.of(12, 0)
                    ),
                    deliveryLocation = deliveryLocation,
                    deliveryTiming = TimingConstraintDto(
                        date = LocalDate.of(2025, 2, 15),
                        mode = TimingMode.FIXED,
                        windowStart = LocalTime.of(14, 0),
                        windowEnd = LocalTime.of(16, 0)
                    ),
                    transportType = TransportType.WASTE,
                    truck = null,
                    driver = null,
                    note = "Test transport",
                    sequenceNumber = 1
                )
            )

            val result = transportsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].displayNumber).isEqualTo("25-000001")
            assertThat(result[0].status).isEqualTo("UNPLANNED")
        }

        @Test
        fun `returns only transports for specified company`() {
            val pickupLocation1 = createTestPickupLocation()
            val deliveryLocation1 = createTestPickupLocation()
            transportRepository.save(
                TransportDto(
                    id = UUID.randomUUID(),
                    displayNumber = "25-000003",
                    consignorParty = testCompany,
                    carrierParty = processorCompany,
                    pickupLocation = pickupLocation1,
                    pickupTiming = TimingConstraintDto(
                        date = LocalDate.of(2025, 2, 16),
                        mode = TimingMode.FIXED,
                        windowStart = LocalTime.of(10, 0),
                        windowEnd = LocalTime.of(12, 0)
                    ),
                    deliveryLocation = deliveryLocation1,
                    deliveryTiming = TimingConstraintDto(
                        date = LocalDate.of(2025, 2, 16),
                        mode = TimingMode.FIXED,
                        windowStart = LocalTime.of(14, 0),
                        windowEnd = LocalTime.of(16, 0)
                    ),
                    transportType = TransportType.WASTE,
                    truck = null,
                    driver = null,
                    note = null,
                    sequenceNumber = 1
                )
            )

            val pickupLocation2 = createTestPickupLocation()
            val deliveryLocation2 = createTestPickupLocation()
            transportRepository.save(
                TransportDto(
                    id = UUID.randomUUID(),
                    displayNumber = "25-000004",
                    consignorParty = otherCompany,
                    carrierParty = processorCompany,
                    pickupLocation = pickupLocation2,
                    pickupTiming = TimingConstraintDto(
                        date = LocalDate.of(2025, 2, 17),
                        mode = TimingMode.FIXED,
                        windowStart = LocalTime.of(10, 0),
                        windowEnd = LocalTime.of(12, 0)
                    ),
                    deliveryLocation = deliveryLocation2,
                    deliveryTiming = TimingConstraintDto(
                        date = LocalDate.of(2025, 2, 17),
                        mode = TimingMode.FIXED,
                        windowStart = LocalTime.of(14, 0),
                        windowEnd = LocalTime.of(16, 0)
                    ),
                    transportType = TransportType.WASTE,
                    truck = null,
                    driver = null,
                    note = null,
                    sequenceNumber = 1
                )
            )

            val result = transportsByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].displayNumber).isEqualTo("25-000003")
        }
    }

    @Nested
    inner class GetInvoicesByCompanyTests {

        @Test
        fun `returns empty list when company has no invoices`() {
            val result = invoicesByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).isEmpty()
        }

        @Test
        fun `returns invoices for company`() {
            invoiceRepository.save(
                InvoiceDto(
                    id = UUID.randomUUID(),
                    invoiceNumber = "INV-2025-0001",
                    invoiceType = InvoiceType.PURCHASE,
                    documentType = InvoiceDocumentType.INVOICE,
                    status = InvoiceStatus.DRAFT,
                    invoiceDate = LocalDate.of(2025, 2, 1),
                    customerCompanyId = testCompany.id,
                    customerNumber = "CUST001",
                    customerName = "Test Consignor Company",
                    customerStreetName = "Test Street",
                    customerBuildingNumber = "42",
                    customerBuildingNumberAddition = null,
                    customerPostalCode = "1234AB",
                    customerCity = "Amsterdam",
                    customerCountry = "Netherlands",
                    customerVatNumber = null,
                    originalInvoiceId = null,
                    creditedInvoiceNumber = null,
                    weightTicket = null,
                    finalizedAt = null,
                    finalizedBy = null
                )
            )

            val result = invoicesByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].invoiceNumber).isEqualTo("INV-2025-0001")
            assertThat(result[0].invoiceType).isEqualTo("PURCHASE")
            assertThat(result[0].status).isEqualTo("DRAFT")
        }

        @Test
        fun `calculates total including VAT from invoice lines`() {
            val invoice = InvoiceDto(
                id = UUID.randomUUID(),
                invoiceNumber = "INV-2025-0002",
                invoiceType = InvoiceType.PURCHASE,
                documentType = InvoiceDocumentType.INVOICE,
                status = InvoiceStatus.DRAFT,
                invoiceDate = LocalDate.of(2025, 2, 1),
                customerCompanyId = testCompany.id,
                customerNumber = "CUST001",
                customerName = "Test Consignor Company",
                customerStreetName = "Test Street",
                customerBuildingNumber = "42",
                customerBuildingNumberAddition = null,
                customerPostalCode = "1234AB",
                customerCity = "Amsterdam",
                customerCountry = "Netherlands",
                customerVatNumber = null,
                originalInvoiceId = null,
                creditedInvoiceNumber = null,
                weightTicket = null,
                finalizedAt = null,
                finalizedBy = null
            )

            invoice.lines.add(
                InvoiceLineDto(
                    id = UUID.randomUUID(),
                    invoice = invoice,
                    lineNumber = 1,
                    lineDate = LocalDate.of(2025, 2, 1),
                    description = "Test item",
                    orderReference = null,
                    vatCode = "VAT21",
                    vatPercentage = BigDecimal("21"),
                    glAccountCode = "8000",
                    quantity = BigDecimal("10"),
                    unitPrice = BigDecimal("100.00"),
                    totalExclVat = BigDecimal("1000.00"),
                    unitOfMeasure = "kg",
                    catalogItemId = testCatalogItem.id,
                    catalogItemCode = testCatalogItem.code,
                    catalogItemName = testCatalogItem.name,
                    catalogItemType = CatalogItemType.WASTE_STREAM
                )
            )

            invoiceRepository.save(invoice)

            val result = invoicesByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            // Total = 10 * 100 * (1 + 21/100) = 1000 * 1.21 = 1210
            assertThat(result[0].totalInclVat).isEqualByComparingTo(BigDecimal("1210.00"))
        }

        @Test
        fun `returns only invoices for specified company`() {
            invoiceRepository.save(
                InvoiceDto(
                    id = UUID.randomUUID(),
                    invoiceNumber = "INV-2025-0003",
                    invoiceType = InvoiceType.PURCHASE,
                    documentType = InvoiceDocumentType.INVOICE,
                    status = InvoiceStatus.DRAFT,
                    invoiceDate = LocalDate.of(2025, 2, 1),
                    customerCompanyId = testCompany.id,
                    customerNumber = "CUST001",
                    customerName = "Test Consignor Company",
                    customerStreetName = "Test Street",
                    customerBuildingNumber = "42",
                    customerBuildingNumberAddition = null,
                    customerPostalCode = "1234AB",
                    customerCity = "Amsterdam",
                    customerCountry = "Netherlands",
                    customerVatNumber = null,
                    originalInvoiceId = null,
                    creditedInvoiceNumber = null,
                    weightTicket = null,
                    finalizedAt = null,
                    finalizedBy = null
                )
            )

            invoiceRepository.save(
                InvoiceDto(
                    id = UUID.randomUUID(),
                    invoiceNumber = "INV-2025-0004",
                    invoiceType = InvoiceType.PURCHASE,
                    documentType = InvoiceDocumentType.INVOICE,
                    status = InvoiceStatus.DRAFT,
                    invoiceDate = LocalDate.of(2025, 2, 1),
                    customerCompanyId = otherCompany.id,
                    customerNumber = "CUST002",
                    customerName = "Other Company",
                    customerStreetName = "Other Street",
                    customerBuildingNumber = "99",
                    customerBuildingNumberAddition = null,
                    customerPostalCode = "5678CD",
                    customerCity = "Rotterdam",
                    customerCountry = "Netherlands",
                    customerVatNumber = null,
                    originalInvoiceId = null,
                    creditedInvoiceNumber = null,
                    weightTicket = null,
                    finalizedAt = null,
                    finalizedBy = null
                )
            )

            val result = invoicesByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].invoiceNumber).isEqualTo("INV-2025-0003")
        }

        @Test
        fun `returns zero total when invoice has no lines`() {
            invoiceRepository.save(
                InvoiceDto(
                    id = UUID.randomUUID(),
                    invoiceNumber = "INV-2025-0005",
                    invoiceType = InvoiceType.PURCHASE,
                    documentType = InvoiceDocumentType.INVOICE,
                    status = InvoiceStatus.DRAFT,
                    invoiceDate = LocalDate.of(2025, 2, 1),
                    customerCompanyId = testCompany.id,
                    customerNumber = "CUST001",
                    customerName = "Test Consignor Company",
                    customerStreetName = "Test Street",
                    customerBuildingNumber = "42",
                    customerBuildingNumberAddition = null,
                    customerPostalCode = "1234AB",
                    customerCity = "Amsterdam",
                    customerCountry = "Netherlands",
                    customerVatNumber = null,
                    originalInvoiceId = null,
                    creditedInvoiceNumber = null,
                    weightTicket = null,
                    finalizedAt = null,
                    finalizedBy = null
                )
            )

            val result = invoicesByCompanyQueryAdapter.execute(testCompany.id)

            assertThat(result).hasSize(1)
            assertThat(result[0].totalInclVat).isEqualByComparingTo(BigDecimal.ZERO)
        }
    }

    private fun createTestPickupLocation(): PickupLocationDto {
        return PickupLocationDto.DutchAddressDto(
            streetName = "Test Street",
            buildingNumber = "42",
            buildingNumberAddition = "A",
            postalCode = "1234AB",
            city = "Test City",
            country = "Netherlands"
        )
    }
}
