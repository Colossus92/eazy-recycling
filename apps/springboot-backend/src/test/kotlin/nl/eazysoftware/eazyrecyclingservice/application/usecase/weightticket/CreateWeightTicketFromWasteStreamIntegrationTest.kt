package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.WeightTicketJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.WeightTicketStatusDto
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class CreateWeightTicketFromWasteStreamIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var createWeightTicketFromWasteStream: CreateWeightTicketFromWasteStream

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var wasteStreamRepository: WasteStreamJpaRepository

    @Autowired
    private lateinit var weightTicketRepository: WeightTicketJpaRepository

    @Autowired
    private lateinit var companyRepository: CompanyJpaRepository

    @Autowired
    private lateinit var catalogItemRepository: CatalogItemJpaRepository

    @Autowired
    private lateinit var vatRateRepository: VatRateJpaRepository

    @Autowired
    private lateinit var euralRepository: EuralRepository

    @Autowired
    private lateinit var processingMethodRepository: ProcessingMethodRepository

    @Autowired
    private lateinit var pickupLocationRepository: PickupLocationRepository

    private lateinit var testConsignorCompanyId: UUID
    private lateinit var processorCompanyId: UUID
    private lateinit var testCatalogItemId: UUID
    private lateinit var pickupLocationId: String

    @BeforeEach
    fun setup() {
        weightTicketRepository.deleteAll()
        wasteStreamRepository.deleteAll()

        setupReferenceData()
    }

    private fun setupReferenceData() {
        euralRepository.findById("16 01 17").orElseGet {
            euralRepository.save(Eural(code = "16 01 17", description = "Paper and cardboard"))
        }

        processingMethodRepository.findById("A.01").orElseGet {
            processingMethodRepository.save(ProcessingMethodDto(code = "A.01", description = "Recycling"))
        }

        testConsignorCompanyId = createCompany("Test Consignor Company", null, "111111VIHB", "11111111")
        processorCompanyId = createCompany("Processor Company", "08797", "087970VIHB", "08797000")
        pickupLocationId = createPickupLocation()

        val testVatRate = vatRateRepository.save(
            VatRateDto(
                vatCode = "VAT21-TEST",
                percentage = BigDecimal("21"),
                validFrom = Instant.now().minusSeconds(86400),
                validTo = null,
                countryCode = "NL",
                description = "Standard VAT 21%",
            )
        )

        testCatalogItemId = catalogItemRepository.save(
            CatalogItemDto(
                id = UUID.randomUUID(),
                code = "WASTE001-TEST",
                name = "Test Waste Stream Catalog Item",
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
        ).id

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

    private fun createWasteStream(number: String, withCatalogItem: Boolean = true) {
        val wasteStream = WasteStreamDto(
            number = number,
            name = "Test Waste Stream",
            euralCode = euralRepository.getReferenceById("16 01 17"),
            processingMethodCode = processingMethodRepository.getReferenceById("A.01"),
            wasteCollectionType = "DEFAULT",
            pickupLocation = pickupLocationRepository.getReferenceById(pickupLocationId),
            consignorParty = companyRepository.getReferenceById(testConsignorCompanyId),
            pickupParty = companyRepository.getReferenceById(testConsignorCompanyId),
            dealerParty = null,
            collectorParty = null,
            brokerParty = null,
            catalogItem = if (withCatalogItem) catalogItemRepository.getReferenceById(testCatalogItemId) else null,
            processorParty = companyRepository.getReferenceById(processorCompanyId),
            status = "ACTIVE",
            consignorClassification = 1
        )
        wasteStreamRepository.save(wasteStream)
        entityManager.flush()
    }

    @Test
    fun `creates weight ticket from waste stream with catalog item`() {
        createWasteStream("087970000001")

        val command = CreateWeightTicketFromWasteStreamCommand(
            wasteStreamNumber = "087970000001"
        )

        val result = createWeightTicketFromWasteStream.execute(command)

        assertThat(result.weightTicketId).isPositive()

        val savedWeightTicket = weightTicketRepository.findByNumber(result.weightTicketId)
        assertThat(savedWeightTicket).isNotNull
        assertThat(savedWeightTicket!!.consignorParty.id).isEqualTo(testConsignorCompanyId)
        assertThat(savedWeightTicket.status).isEqualTo(WeightTicketStatusDto.DRAFT)
        assertThat(savedWeightTicket.direction.name).isEqualTo("INBOUND")
        assertThat(savedWeightTicket.lines).hasSize(1)
        assertThat(savedWeightTicket.lines[0].wasteStreamNumber).isEqualTo("087970000001")
        assertThat(savedWeightTicket.lines[0].weightValue).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(savedWeightTicket.lines[0].catalogItem.id).isEqualTo(testCatalogItemId)
    }

    @Test
    fun `copies pickup location from waste stream to weight ticket`() {
        createWasteStream("087970000002")

        val command = CreateWeightTicketFromWasteStreamCommand(
            wasteStreamNumber = "087970000002"
        )

        val result = createWeightTicketFromWasteStream.execute(command)

        val savedWeightTicket = weightTicketRepository.findByNumber(result.weightTicketId)
        assertThat(savedWeightTicket).isNotNull
        assertThat(savedWeightTicket!!.pickupLocation).isNotNull
    }

    @Test
    fun `throws exception when waste stream not found`() {
        val command = CreateWeightTicketFromWasteStreamCommand(
            wasteStreamNumber = "999999999999"
        )

        assertThatThrownBy { createWeightTicketFromWasteStream.execute(command) }
            .isInstanceOf(EntityNotFoundException::class.java)
            .hasMessageContaining("999999999999")
    }

    @Test
    fun `throws exception when waste stream has no catalog item`() {
        createWasteStream("087970000003", withCatalogItem = false)

        val command = CreateWeightTicketFromWasteStreamCommand(
            wasteStreamNumber = "087970000003"
        )

        assertThatThrownBy { createWeightTicketFromWasteStream.execute(command) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("087970000003")
    }

    @Test
    fun `creates weight ticket with DRAFT status`() {
        createWasteStream("087970000004")

        val command = CreateWeightTicketFromWasteStreamCommand(
            wasteStreamNumber = "087970000004"
        )

        val result = createWeightTicketFromWasteStream.execute(command)

        val savedWeightTicket = weightTicketRepository.findByNumber(result.weightTicketId)
        assertThat(savedWeightTicket).isNotNull
        assertThat(savedWeightTicket!!.status).isEqualTo(WeightTicketStatusDto.DRAFT)
    }

    @Test
    fun `creates weight ticket with INBOUND direction`() {
        createWasteStream("087970000005")

        val command = CreateWeightTicketFromWasteStreamCommand(
            wasteStreamNumber = "087970000005"
        )

        val result = createWeightTicketFromWasteStream.execute(command)

        val savedWeightTicket = weightTicketRepository.findByNumber(result.weightTicketId)
        assertThat(savedWeightTicket).isNotNull
        assertThat(savedWeightTicket!!.direction.name).isEqualTo("INBOUND")
    }

    @Test
    fun `creates weight ticket with zero weight line`() {
        createWasteStream("087970000006")

        val command = CreateWeightTicketFromWasteStreamCommand(
            wasteStreamNumber = "087970000006"
        )

        val result = createWeightTicketFromWasteStream.execute(command)

        val savedWeightTicket = weightTicketRepository.findByNumber(result.weightTicketId)
        assertThat(savedWeightTicket).isNotNull
        assertThat(savedWeightTicket!!.lines).hasSize(1)
        assertThat(savedWeightTicket.lines[0].weightValue).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `creates weight ticket with no carrier, truck, or delivery location`() {
        createWasteStream("087970000007")

        val command = CreateWeightTicketFromWasteStreamCommand(
            wasteStreamNumber = "087970000007"
        )

        val result = createWeightTicketFromWasteStream.execute(command)

        val savedWeightTicket = weightTicketRepository.findByNumber(result.weightTicketId)
        assertThat(savedWeightTicket).isNotNull
        assertThat(savedWeightTicket!!.carrierParty).isNull()
        assertThat(savedWeightTicket.truckLicensePlate).isNull()
        assertThat(savedWeightTicket.deliveryLocation).isNull()
    }

    @Test
    fun `creates multiple weight tickets from same waste stream with unique IDs`() {
        createWasteStream("087970000008")

        val command = CreateWeightTicketFromWasteStreamCommand(
            wasteStreamNumber = "087970000008"
        )

        val result1 = createWeightTicketFromWasteStream.execute(command)
        val result2 = createWeightTicketFromWasteStream.execute(command)

        assertThat(result1.weightTicketId).isNotEqualTo(result2.weightTicketId)

        val savedWeightTicket1 = weightTicketRepository.findByNumber(result1.weightTicketId)
        val savedWeightTicket2 = weightTicketRepository.findByNumber(result2.weightTicketId)

        assertThat(savedWeightTicket1).isNotNull
        assertThat(savedWeightTicket2).isNotNull
    }
}
