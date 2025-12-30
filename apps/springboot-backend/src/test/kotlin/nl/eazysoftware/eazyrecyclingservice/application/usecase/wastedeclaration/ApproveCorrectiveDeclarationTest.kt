package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.*
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ApproveCorrectiveDeclarationTest {

  @Mock
  private lateinit var lmaDeclarations: LmaDeclarations

  @Mock
  private lateinit var amiceSessions: AmiceSessions

  @Mock
  private lateinit var wasteStreams: WasteStreams

  @Mock
  private lateinit var firstReceivalMessageMapper: FirstReceivalMessageMapper

  @Mock
  private lateinit var weightTickets: WeightTickets

  private lateinit var service: ApproveCorrectiveDeclarationService

  private val declarationId = "000000000031"
  private val wasteStreamNumber = "123450000001"

  @BeforeEach
  fun setUp() {
    service = ApproveCorrectiveDeclarationService(
      lmaDeclarations = lmaDeclarations,
      amiceSessions = amiceSessions,
      wasteStreams = wasteStreams,
      firstReceivalMessageMapper = firstReceivalMessageMapper,
      weightTickets = weightTickets
    )
  }

  @Test
  fun `approve should parse MMyyyy period format correctly for first receival`() {
    val declaration = createFirstReceivalDeclaration(period = "112025")
    val wasteStream = createWasteStream()
    val soapMessage = EersteOntvangstMeldingDetails()

    whenever(lmaDeclarations.findById(declarationId)).thenReturn(declaration)
    whenever(wasteStreams.findByNumber(WasteStreamNumber(wasteStreamNumber))).thenReturn(wasteStream)
    whenever(firstReceivalMessageMapper.mapToSoapMessage(
      declarationId = declarationId,
      wasteStream = wasteStream,
      transporters = declaration.transporters,
      totalWeight = declaration.totalWeight.toInt(),
      totalShipments = declaration.totalShipments.toShort(),
      yearMonth = YearMonth(2025, 11)
    )).thenReturn(soapMessage)

    val result = service.approve(declarationId)

    assertTrue(result.success)
    assertEquals("Melding goedgekeurd en verstuurd naar LMA", result.message)
    verify(amiceSessions).declareFirstReceivals(listOf(soapMessage))
    verify(lmaDeclarations).saveAll(argThat { list ->
      list.size == 1 && list[0].status == LmaDeclarationDto.Status.PENDING
    })
  }

  @Test
  fun `approve should parse various MMyyyy period formats correctly`() {
    val testCases = listOf(
      "012025" to YearMonth(2025, 1),  // January
      "062024" to YearMonth(2024, 6),  // June
      "122023" to YearMonth(2023, 12)  // December
    )

    testCases.forEach { (period, expectedYearMonth) ->
      reset(lmaDeclarations, wasteStreams, firstReceivalMessageMapper, amiceSessions)

      val declaration = createFirstReceivalDeclaration(period = period)
      val wasteStream = createWasteStream()
      val soapMessage = EersteOntvangstMeldingDetails()

      whenever(lmaDeclarations.findById(declarationId)).thenReturn(declaration)
      whenever(wasteStreams.findByNumber(WasteStreamNumber(wasteStreamNumber))).thenReturn(wasteStream)
      whenever(firstReceivalMessageMapper.mapToSoapMessage(
        declarationId = any(),
        wasteStream = any(),
        transporters = any(),
        totalWeight = any(),
        totalShipments = any(),
        yearMonth = eq(expectedYearMonth)
      )).thenReturn(soapMessage)

      val result = service.approve(declarationId)

      assertTrue(result.success, "Failed for period: $period")
      verify(firstReceivalMessageMapper).mapToSoapMessage(
        declarationId = any(),
        wasteStream = any(),
        transporters = any(),
        totalWeight = any(),
        totalShipments = any(),
        yearMonth = eq(expectedYearMonth)
      )
    }
  }

  @Test
  fun `approve should fail with invalid period format - wrong length`() {
    val declaration = createFirstReceivalDeclaration(period = "1120")
    val wasteStream = createWasteStream()

    whenever(lmaDeclarations.findById(declarationId)).thenReturn(declaration)
    whenever(wasteStreams.findByNumber(WasteStreamNumber(wasteStreamNumber))).thenReturn(wasteStream)

    val result = service.approve(declarationId)

    assertFalse(result.success)
    assertTrue(result.message.contains("Fout bij versturen van LMA melding"))
    verify(amiceSessions, never()).declareFirstReceivals(any())
    verify(lmaDeclarations).saveAll(argThat { list ->
      list.size == 1 && list[0].status == LmaDeclarationDto.Status.FAILED
    })
  }

  @Test
  fun `approve should fail with invalid period format - invalid month`() {
    val declaration = createFirstReceivalDeclaration(period = "132025")
    val wasteStream = createWasteStream()

    whenever(lmaDeclarations.findById(declarationId)).thenReturn(declaration)
    whenever(wasteStreams.findByNumber(WasteStreamNumber(wasteStreamNumber))).thenReturn(wasteStream)

    val result = service.approve(declarationId)

    assertFalse(result.success)
    assertTrue(result.message.contains("Fout bij versturen van LMA melding"))
    verify(amiceSessions, never()).declareFirstReceivals(any())
  }

  @Test
  fun `approve should fail with invalid period format - non-numeric`() {
    val declaration = createFirstReceivalDeclaration(period = "AB2025")
    val wasteStream = createWasteStream()

    whenever(lmaDeclarations.findById(declarationId)).thenReturn(declaration)
    whenever(wasteStreams.findByNumber(WasteStreamNumber(wasteStreamNumber))).thenReturn(wasteStream)

    val result = service.approve(declarationId)

    assertFalse(result.success)
    assertTrue(result.message.contains("Fout bij versturen van LMA melding"))
    verify(amiceSessions, never()).declareFirstReceivals(any())
  }

  @Test
  fun `approve should return error when declaration not found`() {
    whenever(lmaDeclarations.findById(declarationId)).thenReturn(null)

    val result = service.approve(declarationId)

    assertFalse(result.success)
    assertEquals("Declaratie niet gevonden: $declarationId", result.message)
    verify(amiceSessions, never()).declareFirstReceivals(any())
    verify(amiceSessions, never()).declareMonthlyReceivals(any())
  }

  @Test
  fun `approve should return error when declaration is not in waiting approval status`() {
    val declaration = createFirstReceivalDeclaration(period = "112025")
      .copy(status = LmaDeclarationDto.Status.PENDING)

    whenever(lmaDeclarations.findById(declarationId)).thenReturn(declaration)

    val result = service.approve(declarationId)

    assertFalse(result.success)
    assertTrue(result.message.contains("niet in goedkeuringsstatus"))
    verify(amiceSessions, never()).declareFirstReceivals(any())
  }

  @Test
  fun `approve should handle monthly receival declarations`() {
    val declaration = createMonthlyReceivalDeclaration(period = "112025")

    whenever(lmaDeclarations.findById(declarationId)).thenReturn(declaration)

    val result = service.approve(declarationId)

    assertTrue(result.success)
    verify(amiceSessions).declareMonthlyReceivals(argThat { list ->
      list.size == 1 && list[0].periodeMelding == "112025"
    })
  }

  @Test
  fun `approve should fail when waste stream not found`() {
    val declaration = createFirstReceivalDeclaration(period = "112025")

    whenever(lmaDeclarations.findById(declarationId)).thenReturn(declaration)
    whenever(wasteStreams.findByNumber(WasteStreamNumber(wasteStreamNumber))).thenReturn(null)

    val result = service.approve(declarationId)

    assertFalse(result.success)
    assertTrue(result.message.contains("Fout bij versturen van LMA melding"))
    verify(lmaDeclarations).saveAll(argThat { list ->
      list.size == 1 && list[0].status == LmaDeclarationDto.Status.FAILED
    })
  }

  private fun createFirstReceivalDeclaration(period: String) = LmaDeclarationDto(
    id = declarationId,
    wasteStreamNumber = wasteStreamNumber,
    period = period,
    transporters = listOf("TRANS-001"),
    totalWeight = 1000L,
    totalShipments = 10L,
    type = LmaDeclaration.Type.FIRST_RECEIVAL,
    status = LmaDeclarationDto.Status.WAITING_APPROVAL,
    createdAt = Instant.now(),
    amiceUUID = null,
    errors = null
  )

  private fun createMonthlyReceivalDeclaration(period: String) = LmaDeclarationDto(
    id = declarationId,
    wasteStreamNumber = wasteStreamNumber,
    period = period,
    transporters = listOf("TRANS-001"),
    totalWeight = 1000L,
    totalShipments = 10L,
    type = LmaDeclaration.Type.MONTHLY_RECEIVAL,
    status = LmaDeclarationDto.Status.WAITING_APPROVAL,
    createdAt = Instant.now(),
    amiceUUID = null,
    errors = null
  )

  private fun createWasteStream(): WasteStream {
    val pickupLocation = Location.NoLocation
    val deliveryLocation = WasteDeliveryLocation(
      processorPartyId = ProcessorPartyId("12345")
    )
    val consignor = Consignor.Person
    val companyId = CompanyId(UUID.randomUUID())
    val wasteType = WasteType(
      name = "Test Waste",
      euralCode = EuralCode("123456"),
      processingMethod = ProcessingMethod("R01")
    )

    return WasteStream(
      wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
      wasteType = wasteType,
      pickupLocation = pickupLocation,
      deliveryLocation = deliveryLocation,
      consignorParty = consignor,
      consignorClassification = ConsignorClassification.PICKUP_PARTY,
      pickupParty = companyId
    )
  }
}
