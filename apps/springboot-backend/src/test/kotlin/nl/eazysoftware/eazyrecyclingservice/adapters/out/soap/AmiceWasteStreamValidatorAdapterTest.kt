package nl.eazysoftware.eazyrecyclingservice.adapters.out.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.toetsen.*
import nl.eazysoftware.eazyrecyclingservice.config.soap.ToetsenAfvalstroomNummerClient
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.ws.soap.SoapBody
import org.springframework.ws.soap.SoapFault
import org.springframework.ws.soap.SoapMessage
import org.springframework.ws.soap.client.SoapFaultClientException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class AmiceWasteStreamValidatorAdapterTest {

    @Mock
    private lateinit var toetsenAfvalstroomNummerClient: ToetsenAfvalstroomNummerClient

    @Mock
    private lateinit var companies: Companies

    private lateinit var adapter: AmiceWasteStreamValidatorAdapter

    private val testCompanyId = UUID.randomUUID()
    private val collectorCompanyId = UUID.randomUUID()
    private val dealerCompanyId = UUID.randomUUID()
    private val brokerCompanyId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        adapter = AmiceWasteStreamValidatorAdapter(toetsenAfvalstroomNummerClient, companies)
    }

    @Nested
    inner class ValidateTests {

        @Test
        fun `validate should return valid result when SOAP service returns valid response`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Company(CompanyId(testCompanyId)),
                collectorParty = null,
                dealerParty = null,
                brokerParty = null
            )
            val response = createValidResponse()

            whenever(companies.findById(CompanyId(testCompanyId))).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            val result = adapter.validate(wasteStream)

            assertTrue(result.isValid)
            assertEquals(0, result.errors.size)
            assertEquals("12345TEST001", result.wasteStreamNumber)
        }

        @Test
        fun `validate should return invalid result when SOAP service returns invalid response with errors`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Company(CompanyId(testCompanyId))
            )
            val response = createInvalidResponse(
                listOf(
                    createFout("ERR001", "Invalid waste code"),
                    createFout("ERR002", "Invalid location")
                )
            )

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            val result = adapter.validate(wasteStream)

            assertFalse(result.isValid)
            assertEquals(2, result.errors.size)
            assertEquals("ERR001", result.errors[0].code)
            assertEquals("Invalid waste code", result.errors[0].description)
            assertEquals("ERR002", result.errors[1].code)
            assertEquals("Invalid location", result.errors[1].description)
        }

        @Test
        fun `validate should handle SoapFaultClientException and return invalid result`() {
            val wasteStream = createWasteStream(consignorParty = Consignor.Company(CompanyId(testCompanyId)))
            val soapMessage: SoapMessage = mock()
            val soapBody: SoapBody = mock()
            val soapFault: SoapFault = mock()

            whenever(soapMessage.soapBody).thenReturn(soapBody)
            whenever(soapBody.fault).thenReturn(soapFault)
            whenever(soapFault.faultStringOrReason).thenReturn("SOAP service unavailable")

            val soapFaultException = SoapFaultClientException(soapMessage)

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenThrow(soapFaultException)

            val result = adapter.validate(wasteStream)

            assertFalse(result.isValid)
            assertEquals(1, result.errors.size)
            assertEquals("SOAP_FAULT", result.errors[0].code)
            assertTrue(result.errors[0].description.contains("SOAP Fault"))
        }

        @Test
        fun `validate should handle generic Exception and return invalid result`() {
            val wasteStream = createWasteStream(consignorParty = Consignor.Company(CompanyId(testCompanyId)))
            val exception = RuntimeException("Network timeout")

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenThrow(exception)

            val result = adapter.validate(wasteStream)

            assertFalse(result.isValid)
            assertEquals(1, result.errors.size)
            assertEquals("SOAP_ERROR", result.errors[0].code)
            assertTrue(result.errors[0].description.contains("RuntimeException"))
            assertTrue(result.errors[0].description.contains("Network timeout"))
        }

        @Test
        fun `validate should handle null result in response`() {
            val wasteStream = createWasteStream(consignorParty = Consignor.Company(CompanyId(testCompanyId)))
            val response = ToetsenAfvalstroomNummerResponse()
            response.toetsenAfvalstroomNummerResult = null

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            val result = adapter.validate(wasteStream)

            assertFalse(result.isValid)
            assertEquals(1, result.errors.size)
            assertEquals("SOAP_ERROR", result.errors[0].code)
            assertTrue(result.errors[0].description.contains("NullPointerException"))
        }
    }

    @Nested
    inner class ConsignorMappingTests {

        @Test
        fun `mapConsignor should map Company consignor from Netherlands`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Company(CompanyId(testCompanyId))
            )
            val company = createDutchCompany(testCompanyId)
            val response = createValidResponse()

            whenever(companies.findById(CompanyId(testCompanyId))).thenReturn(company)
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals("12345678", request.ontdoener.handelsregisternummer)
            assertEquals("Nederland", request.ontdoener.land)
            assertEquals(null, request.ontdoener.naam)
            assertFalse(request.ontdoener.isIsParticulier)
        }

        @Test
        fun `mapConsignor should map Company consignor from foreign country`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Company(CompanyId(testCompanyId))
            )
            val company = createForeignCompany(testCompanyId, "Germany")
            val response = createValidResponse()

            whenever(companies.findById(CompanyId(testCompanyId))).thenReturn(company)
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals("Foreign Company BV", request.ontdoener.naam)
            assertEquals("Germany", request.ontdoener.land)
            assertFalse(request.ontdoener.isIsParticulier)
        }

        @Test
        fun `mapConsignor should map Person consignor`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Person,
                pickupLocation = Location.NoLocation
            )
            val response = createValidResponse()

            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertTrue(request.ontdoener.isIsParticulier)
        }

        @Test
        fun `mapConsignor should handle null company from repository`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Company(CompanyId(testCompanyId))
            )
            val response = createValidResponse()

            whenever(companies.findById(CompanyId(testCompanyId))).thenReturn(null)
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertFalse(request.ontdoener.isIsParticulier)
            assertEquals(null, request.ontdoener.handelsregisternummer)
        }
    }

    @Nested
    inner class PickupLocationMappingTests {

        @Test
        fun `mapPickupLocation should map DutchAddress`() {
            val wasteStream = createWasteStream(
                pickupLocation = Location.DutchAddress(
                    Address(
                        streetName = StreetName("Teststraat"),
                        buildingNumber = "123",
                        buildingNumberAddition = "A",
                        postalCode = DutchPostalCode("1234AB"),
                        city = City("Amsterdam")
                    )
                )
            )
            val response = createValidResponse()

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals("1234AB", request.locatieHerkomstPostcode)
            assertEquals("123", request.locatieHerkomstHuisnummer)
            assertEquals("A", request.locatieHerkomstHuisnummerToevoeging)
            assertEquals("Nederland", request.locatieHerkomstLand)
        }

        @Test
        fun `mapPickupLocation should map ProximityDescription`() {
            val wasteStream = createWasteStream(
                pickupLocation = Location.ProximityDescription(
                    postalCodeDigits = "1234",
                    city = City("Rotterdam"),
                    description = "Bij de oude molen"
                )
            )
            val response = createValidResponse()

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals("1234", request.locatieHerkomstPostcode)
            assertEquals("Rotterdam", request.locatieHerkomstWoonplaats)
            assertEquals("Bij de oude molen", request.locatieHerkomstNabijheidsBeschrijving)
            assertEquals("Nederland", request.locatieHerkomstLand)
        }

        @Test
        fun `mapPickupLocation should map Company location`() {
            val wasteStream = createWasteStream(
                pickupLocation = Location.Company(
                    companyId = CompanyId(testCompanyId),
                    name = "Test Company",
                    address = Address(
                        streetName = StreetName("Bedrijfsweg"),
                        buildingNumber = "456",
                        buildingNumberAddition = "B",
                        postalCode = DutchPostalCode("5678CD"),
                        city = City("Utrecht")
                    )
                )
            )
            val response = createValidResponse()

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals("5678CD", request.locatieHerkomstPostcode)
            assertEquals("456", request.locatieHerkomstHuisnummer)
            assertEquals("B", request.locatieHerkomstHuisnummerToevoeging)
            assertEquals("Utrecht", request.locatieHerkomstWoonplaats)
            assertEquals("Bedrijfsweg", request.locatieHerkomstStraatnaam)
            assertEquals("Nederland", request.locatieHerkomstLand)
        }

        @Test
        fun `mapPickupLocation should map ProjectLocationSnapshot`() {
            val wasteStream = createWasteStream(
                pickupLocation = Location.ProjectLocationSnapshot(
                    projectLocationId = ProjectLocationId(UUID.randomUUID()),
                    companyId = CompanyId(testCompanyId),
                    address = Address(
                        streetName = StreetName("Projectweg"),
                        buildingNumber = "789",
                        buildingNumberAddition = null,
                        postalCode = DutchPostalCode("9012EF"),
                        city = City("Den Haag")
                    )
                )
            )
            val response = createValidResponse()

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals("9012EF", request.locatieHerkomstPostcode)
            assertEquals("789", request.locatieHerkomstHuisnummer)
            assertEquals(null, request.locatieHerkomstHuisnummerToevoeging)
            assertEquals("Den Haag", request.locatieHerkomstWoonplaats)
            assertEquals("Projectweg", request.locatieHerkomstStraatnaam)
            assertEquals("Nederland", request.locatieHerkomstLand)
        }

        @Test
        fun `mapPickupLocation should map NoLocation`() {
            val wasteStream = createWasteStream(
                pickupLocation = Location.NoLocation,
                consignorParty = Consignor.Person
            )
            val response = createValidResponse()

            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals(null, request.locatieHerkomstPostcode)
            assertEquals(null, request.locatieHerkomstHuisnummer)
            assertEquals(null, request.locatieHerkomstWoonplaats)
        }
    }

    @Nested
    inner class PartiesMappingTests {

        @Test
        fun `should map all parties when all are present and companies found`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Company(CompanyId(testCompanyId)),
                collectorParty = CompanyId(collectorCompanyId),
                dealerParty = CompanyId(dealerCompanyId),
                brokerParty = null
            )
            val response = createValidResponse()

            whenever(companies.findById(CompanyId(testCompanyId))).thenReturn(createDutchCompany(testCompanyId))
            whenever(companies.findById(CompanyId(collectorCompanyId))).thenReturn(createDutchCompany(collectorCompanyId))
            whenever(companies.findById(CompanyId(dealerCompanyId))).thenReturn(createDutchCompany(dealerCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals("12345678", request.afzender.handelsregisternummer)
            assertEquals("12345678", request.inzamelaar.handelsregisternummer)
            assertEquals("12345678", request.handelaar.handelsregisternummer)
            assertEquals(null, request.bemiddelaar)
        }

        @Test
        fun `should map broker but not dealer when broker is present`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Company(CompanyId(testCompanyId)),
                collectorParty = null,
                dealerParty = null,
                brokerParty = CompanyId(brokerCompanyId)
            )
            val response = createValidResponse()

            whenever(companies.findById(CompanyId(testCompanyId))).thenReturn(createDutchCompany(testCompanyId))
            whenever(companies.findById(CompanyId(brokerCompanyId))).thenReturn(createDutchCompany(brokerCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals("12345678", request.bemiddelaar.handelsregisternummer)
            assertEquals(null, request.handelaar)
            assertEquals(null, request.inzamelaar)
        }

        @Test
        fun `should not map afzender when consignorParty is Person`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Person,
                pickupLocation = Location.NoLocation,
                collectorParty = null,
                dealerParty = null,
                brokerParty = null
            )
            val response = createValidResponse()

            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals(null, request.afzender)
        }

        @Test
        fun `should map foreign company parties with name`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Company(CompanyId(testCompanyId)),
                collectorParty = CompanyId(collectorCompanyId),
                dealerParty = null,
                brokerParty = null
            )
            val response = createValidResponse()

            whenever(companies.findById(CompanyId(testCompanyId))).thenReturn(createForeignCompany(testCompanyId, "Belgium"))
            whenever(companies.findById(CompanyId(collectorCompanyId))).thenReturn(createForeignCompany(collectorCompanyId, "France"))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals("Foreign Company BV", request.afzender.naam)
            assertEquals("Belgium", request.afzender.land)
            assertEquals("Foreign Company BV", request.inzamelaar.naam)
            assertEquals("France", request.inzamelaar.land)
        }

        @Test
        fun `should handle null companies from repository for all parties`() {
            val wasteStream = createWasteStream(
                consignorParty = Consignor.Company(CompanyId(testCompanyId)),
                collectorParty = CompanyId(collectorCompanyId),
                dealerParty = CompanyId(dealerCompanyId),
                brokerParty = null
            )
            val response = createValidResponse()

            whenever(companies.findById(any())).thenReturn(null)
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals(null, request.afzender?.handelsregisternummer)
            assertEquals(null, request.inzamelaar?.handelsregisternummer)
            assertEquals(null, request.handelaar?.handelsregisternummer)
        }
    }

    @Nested
    inner class CollectionTypeMappingTests {

        @Test
        fun `should map DEFAULT collection type`() {
            val wasteStream = createWasteStream(
                collectionType = WasteCollectionType.DEFAULT
            )
            val response = createValidResponse()

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertFalse(request.isRouteInzameling)
            assertFalse(request.isInzamelaarsRegeling)
        }

        @Test
        fun `should map ROUTE collection type`() {
            val wasteStream = createWasteStream(
                collectionType = WasteCollectionType.ROUTE,
                pickupLocation = Location.NoLocation,
                collectorParty = CompanyId(collectorCompanyId)
            )
            val response = createValidResponse()

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertTrue(request.isRouteInzameling)
            assertFalse(request.isInzamelaarsRegeling)
        }

        @Test
        fun `should map COLLECTORS_SCHEME collection type`() {
            val wasteStream = createWasteStream(
                collectionType = WasteCollectionType.COLLECTORS_SCHEME,
                pickupLocation = Location.NoLocation,
                collectorParty = CompanyId(collectorCompanyId)
            )
            val response = createValidResponse()

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertFalse(request.isRouteInzameling)
            assertTrue(request.isInzamelaarsRegeling)
        }
    }

    @Nested
    inner class WasteTypeMappingTests {

        @Test
        fun `should map waste type with code normalization`() {
            val wasteStream = createWasteStream()
            val response = createValidResponse()

            whenever(companies.findById(any())).thenReturn(createDutchCompany(testCompanyId))
            whenever(toetsenAfvalstroomNummerClient.validate(any())).thenReturn(response)

            adapter.validate(wasteStream)

            val requestCaptor = argumentCaptor<ToetsenAfvalstroomNummer>()
            verify(toetsenAfvalstroomNummerClient).validate(requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertEquals("170101", request.afvalstof)
            assertEquals("Beton", request.gebruikelijkeNaamAfvalstof)
            assertEquals("R51", request.verwerkingsMethode)
        }
    }

    private fun createWasteStream(
        wasteStreamNumber: String = "12345TEST001",
        consignorParty: Consignor = Consignor.Company(CompanyId(testCompanyId)),
        pickupLocation: Location = Location.DutchAddress(
            Address(
                streetName = StreetName("Teststraat"),
                buildingNumber = "1",
                buildingNumberAddition = null,
                postalCode = DutchPostalCode("1234AB"),
                city = City("Amsterdam")
            )
        ),
        collectionType: WasteCollectionType = WasteCollectionType.DEFAULT,
        collectorParty: CompanyId? = null,
        dealerParty: CompanyId? = null,
        brokerParty: CompanyId? = null
    ): WasteStream {
        return WasteStream(
            wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
            wasteType = WasteType(
                name = "Beton",
                euralCode = EuralCode("17 01 01"),
                processingMethod = ProcessingMethod("R.51")
            ),
            collectionType = collectionType,
            pickupLocation = pickupLocation,
            deliveryLocation = WasteDeliveryLocation(ProcessorPartyId("12345")),
            consignorParty = consignorParty,
            consignorClassification = ConsignorClassification.PICKUP_PARTY,
            pickupParty = CompanyId(testCompanyId),
            dealerParty = dealerParty,
            collectorParty = collectorParty,
            brokerParty = brokerParty
        )
    }

    private fun createDutchCompany(id: UUID): Company {
        return Company(
            companyId = CompanyId(id),
            name = "Test Company BV",
            chamberOfCommerceId = "12345678",
            vihbNumber = null,
            processorId = null,
            address = Address(
                streetName = StreetName("Bedrijfsweg"),
                buildingNumber = "10",
                buildingNumberAddition = null,
                postalCode = DutchPostalCode("1234AB"),
                city = City("Amsterdam")
            ),
            roles = emptyList(),
            phone = PhoneNumber("+31612345678"),
            email = Email("test@company.nl"),
            isSupplier = false,
            isCustomer = true
        )
    }

    private fun createForeignCompany(id: UUID, country: String): Company {
        return Company(
            companyId = CompanyId(id),
            name = "Foreign Company BV",
            chamberOfCommerceId = "FOREIGN123",
            vihbNumber = null,
            processorId = null,
            address = Address(
                streetName = StreetName("Foreign Street"),
                buildingNumber = "1",
                buildingNumberAddition = null,
                postalCode = DutchPostalCode("0000AA"),
                city = City("Foreign City"),
                country = country
            ),
            roles = emptyList(),
            phone = PhoneNumber("+31612345678"),
            email = Email("test@foreign.com"),
            isSupplier = false,
            isCustomer = true
        )
    }

    private fun createValidResponse(): ToetsenAfvalstroomNummerResponse {
        val response = ToetsenAfvalstroomNummerResponse()
        val result = ToetsenAfvalstroomNummerResponse2()
        val details = ToetsenAfvalstroomNummerRetourBerichtDetails()

        details.afvalstroomGegevensValide = "True"
        details.aanvraagGegevens = createAanvraagGegevens()

        result.toetsenAfvalstroomNummerRetourBerichtDetails = details
        response.toetsenAfvalstroomNummerResult = result

        return response
    }

    private fun createInvalidResponse(fouten: List<Fout>): ToetsenAfvalstroomNummerResponse {
        val response = ToetsenAfvalstroomNummerResponse()
        val result = ToetsenAfvalstroomNummerResponse2()
        val details = ToetsenAfvalstroomNummerRetourBerichtDetails()

        details.afvalstroomGegevensValide = "False"
        details.aanvraagGegevens = createAanvraagGegevens()
        details.fout.addAll(fouten)

        result.toetsenAfvalstroomNummerRetourBerichtDetails = details
        response.toetsenAfvalstroomNummerResult = result

        return response
    }

    private fun createAanvraagGegevens(): AanvraagGegevens {
        val aanvraag = AanvraagGegevens()
        aanvraag.afvalstroomNummer = "12345TEST001"
        aanvraag.routeInzameling = "False"
        aanvraag.inzamelaarsRegeling = "False"

        val ontdoener = Ontdoener2()
        ontdoener.handelsregisternummer = "12345678"
        ontdoener.land = "Nederland"
        ontdoener.isIsParticulier = false
        aanvraag.ontdoener = ontdoener

        aanvraag.locatieHerkomstPostcode = "1234AB"
        aanvraag.locatieOntvangst = "12345"
        aanvraag.afvalstof = "170101"
        aanvraag.gebruikelijkeNaamAfvalstof = "Beton"
        aanvraag.verwerkingsMethode = "R51"

        return aanvraag
    }

    private fun createFout(code: String, description: String): Fout {
        val fout = Fout()
        fout.foutCode = code
        fout.foutomschrijving = description
        return fout
    }
}
