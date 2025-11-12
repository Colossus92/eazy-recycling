package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.AmiceSessions
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarationSessions
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationSessionDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class SessionResultResolverTest {

  @Mock
  private lateinit var lmaDeclarations: LmaDeclarations

  @Mock
  private lateinit var amiceSessions: AmiceSessions

  @Mock
  private lateinit var lmaDeclarationSessions: LmaDeclarationSessions

  private lateinit var sessionResultResolver: SessionResultResolver

  private val sessionId = UUID.randomUUID()
  private val declarationId1 = "DECL-001"
  private val declarationId2 = "DECL-002"
  private val meldingUUID1 = UUID.randomUUID().toString()
  private val meldingUUID2 = UUID.randomUUID().toString()

  @BeforeEach
  fun setUp() {
    sessionResultResolver = SessionResultResolver(
      lmaDeclarations = lmaDeclarations,
      amiceSessions = amiceSessions,
      lmaDeclarationSessions = lmaDeclarationSessions
    )
  }

  @Test
  fun `processSession should mark session as failed when response details are null`() {
    // Given
    val session = createPendingSession()
    val response = OpvragenResultaatVerwerkingMeldingSessieResponse().apply {
      opvragenResultaatVerwerkingMeldingSessieResponseDetails = null
    }

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.FAILED, savedSession.status)
    assertTrue(savedSession.errors?.contains("Response details are null") ?: false)
  }

  @Test
  fun `processSession should wait and retry when session is still being processed`() {
    // Given
    val session = createPendingSession()
    val response = createResponseWithError("MeldingSessieNogNietAlleMeldingenVerwerkt", "Van de opgegeven MeldingSessie zijn nog niet alle" +
      "meldingen verwerkt. Probeer het later nog eens.")

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)

    // When
    sessionResultResolver.processSession(session)

    // Then
    verify(lmaDeclarationSessions, never()).save(any())
    verify(lmaDeclarations, never()).saveAll(any())
  }

  @Test
  fun `processSession should mark session as failed when there are errors in response`() {
    // Given
    val session = createPendingSession()
    val response = createResponseWithError("SomeErrorCode", "Some error description")

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.FAILED, savedSession.status)
    assertTrue(savedSession.errors?.any { it.contains("SomeErrorCode") } ?: false)
  }

  @Test
  fun `processSession should mark session as failed when no status melding sessie found`() {
    // Given
    val session = createPendingSession()
    val responseDetails = OpvragenResultaatVerwerkingMeldingSessieResponseDetails()
    val response = OpvragenResultaatVerwerkingMeldingSessieResponse()
    response.opvragenResultaatVerwerkingMeldingSessieResponseDetails = responseDetails

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.FAILED, savedSession.status)
    assertTrue(savedSession.errors?.contains("No status melding sessie found in response") ?: false)
  }

  @Test
  fun `processSession should successfully process eerste ontvangst meldingen`() {
    // Given
    val session = createPendingSession()
    val declarations = listOf(
      createDeclaration(declarationId1),
      createDeclaration(declarationId2)
    )
    val response = createResponseWithEersteOntvangstMeldingen()

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)
    whenever(lmaDeclarations.findByIds(session.declarationIds)).thenReturn(declarations)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val declarationListCaptor = argumentCaptor<List<LmaDeclarationDto>>()
    verify(lmaDeclarations).saveAll(declarationListCaptor.capture())
    val savedDeclarations = declarationListCaptor.firstValue

    assertEquals(2, savedDeclarations.size)
    assertEquals(LmaDeclarationDto.Status.COMPLETED, savedDeclarations[0].status)
    assertEquals(UUID.fromString(meldingUUID1), savedDeclarations[0].amiceUUID)
    assertEquals(LmaDeclarationDto.Status.COMPLETED, savedDeclarations[1].status)
    assertEquals(UUID.fromString(meldingUUID2), savedDeclarations[1].amiceUUID)

    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.COMPLETED, savedSession.status)
  }

  @Test
  fun `processSession should mark declarations as failed when they have errors`() {
    // Given
    val session = createPendingSession()
    val declarations = listOf(createDeclaration(declarationId1))
    val response = createResponseWithEersteOntvangstMeldingenWithErrors()

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)
    whenever(lmaDeclarations.findByIds(session.declarationIds)).thenReturn(declarations)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val declarationListCaptor = argumentCaptor<List<LmaDeclarationDto>>()
    verify(lmaDeclarations).saveAll(declarationListCaptor.capture())
    val savedDeclarations = declarationListCaptor.firstValue

    assertEquals(1, savedDeclarations.size)
    assertEquals(LmaDeclarationDto.Status.FAILED, savedDeclarations[0].status)
    assertNotNull(savedDeclarations[0].errors)
    assertTrue(savedDeclarations[0].errors!!.isNotEmpty())
    assertTrue(savedDeclarations[0].errors!![0].contains("ValidationError"))

    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.FAILED, savedSession.status)
  }

  @Test
  fun `processSession should mark declarations as failed when technisch akkoord is false`() {
    // Given
    val session = createPendingSession()
    val declarations = listOf(createDeclaration(declarationId1))
    val response = createResponseWithTechnischAkkoordFalse()

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)
    whenever(lmaDeclarations.findByIds(session.declarationIds)).thenReturn(declarations)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val declarationListCaptor = argumentCaptor<List<LmaDeclarationDto>>()
    verify(lmaDeclarations).saveAll(declarationListCaptor.capture())
    val savedDeclarations = declarationListCaptor.firstValue

    assertEquals(1, savedDeclarations.size)
    assertEquals(LmaDeclarationDto.Status.FAILED, savedDeclarations[0].status)
  }

  @Test
  fun `processSession should successfully process maandelijkse ontvangst meldingen`() {
    // Given
    val session = createPendingSession()
    val declarations = listOf(createDeclaration(declarationId1))
    val response = createResponseWithMaandelijkseOntvangstMeldingen()

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)
    whenever(lmaDeclarations.findByIds(session.declarationIds)).thenReturn(declarations)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val declarationListCaptor = argumentCaptor<List<LmaDeclarationDto>>()
    verify(lmaDeclarations).saveAll(declarationListCaptor.capture())
    val savedDeclarations = declarationListCaptor.firstValue

    assertEquals(1, savedDeclarations.size)
    assertEquals(LmaDeclarationDto.Status.COMPLETED, savedDeclarations[0].status)

    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.COMPLETED, savedSession.status)
  }

  @Test
  fun `processSession should successfully process afgifte meldingen`() {
    // Given
    val session = createPendingSession()
    val declarations = listOf(createDeclaration(declarationId1))
    val response = createResponseWithAfgifteMeldingen()

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)
    whenever(lmaDeclarations.findByIds(session.declarationIds)).thenReturn(declarations)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val declarationListCaptor = argumentCaptor<List<LmaDeclarationDto>>()
    verify(lmaDeclarations).saveAll(declarationListCaptor.capture())
    val savedDeclarations = declarationListCaptor.firstValue

    assertEquals(1, savedDeclarations.size)
    assertEquals(LmaDeclarationDto.Status.COMPLETED, savedDeclarations[0].status)

    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.COMPLETED, savedSession.status)
  }

  @Test
  fun `processSession should mark session as failed when no meldingen found`() {
    // Given
    val session = createPendingSession()
    val response = OpvragenResultaatVerwerkingMeldingSessieResponse().apply {
      val statusDetails = StatusMeldingSessieDetails()
      val responseDetails = OpvragenResultaatVerwerkingMeldingSessieResponseDetails()
      responseDetails.getAanvraagFoutOrStatusMeldingSessie().add(statusDetails)
      opvragenResultaatVerwerkingMeldingSessieResponseDetails = responseDetails
    }

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.FAILED, savedSession.status)
    assertTrue(savedSession.errors?.contains("No meldingen found in response") ?: false)
  }

  @Test
  fun `processSession should add error when declaration not found in database`() {
    // Given
    val session = createPendingSession()
    val declarations = listOf(createDeclaration(declarationId1)) // Only one declaration
    val response = createResponseWithEersteOntvangstMeldingen() // Two meldingen

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)
    whenever(lmaDeclarations.findByIds(session.declarationIds)).thenReturn(declarations)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val declarationListCaptor = argumentCaptor<List<LmaDeclarationDto>>()
    verify(lmaDeclarations).saveAll(declarationListCaptor.capture())
    val savedDeclarations = declarationListCaptor.firstValue
    assertEquals(1, savedDeclarations.size) // Only one declaration was found

    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.FAILED, savedSession.status)
    assertTrue(savedSession.errors?.any { it.contains("not found") } ?: false)
  }

  @Test
  fun `processSession should mark session as failed when exception occurs`() {
    // Given
    val session = createPendingSession()
    val exception = RuntimeException("SOAP service unavailable")

    whenever(amiceSessions.retrieve(sessionId)).thenThrow(exception)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.FAILED, savedSession.status)
    assertTrue(savedSession.errors?.any { it.contains("Exception") && it.contains("SOAP service unavailable") } ?: false)
  }

  @Test
  fun `processSession should mark session as failed when some declarations succeeded and some failed`() {
    // Given
    val session = createPendingSession()
    val declarations = listOf(
      createDeclaration(declarationId1),
      createDeclaration(declarationId2)
    )
    val response = createResponseWithMixedResults()

    whenever(amiceSessions.retrieve(sessionId)).thenReturn(response)
    whenever(lmaDeclarations.findByIds(session.declarationIds)).thenReturn(declarations)

    // When
    sessionResultResolver.processSession(session)

    // Then
    val declarationListCaptor = argumentCaptor<List<LmaDeclarationDto>>()
    verify(lmaDeclarations).saveAll(declarationListCaptor.capture())
    val savedDeclarations = declarationListCaptor.firstValue

    assertEquals(2, savedDeclarations.size)
    assertEquals(LmaDeclarationDto.Status.COMPLETED, savedDeclarations[0].status)
    assertEquals(LmaDeclarationDto.Status.FAILED, savedDeclarations[1].status)

    val sessionCaptor = argumentCaptor<LmaDeclarationSessionDto>()
    verify(lmaDeclarationSessions).save(sessionCaptor.capture())
    val savedSession = sessionCaptor.firstValue
    assertEquals(LmaDeclarationSessionDto.Status.FAILED, savedSession.status)
  }

  // Helper methods

  private fun createPendingSession() = LmaDeclarationSessionDto(
    id = sessionId,
    declarationIds = listOf(declarationId1, declarationId2),
    type = LmaDeclarationSessionDto.Type.FIRST_RECEIVAL,
    status = LmaDeclarationSessionDto.Status.PENDING,
    createdAt = java.time.Instant.now(),
    errors = null
  )

  private fun createDeclaration(id: String) = LmaDeclarationDto(
    id = id,
    status = LmaDeclarationDto.Status.PENDING,
    amiceUUID = null,
    errors = null,
    wasteStreamNumber = "WS-001",
    period = "012025",
    transporters = listOf("TRANS-001"),
    totalWeight = 1000L,
    totalShipments = 10L,
    createdAt = java.time.Instant.now()
  )

  private fun createResponseWithError(errorCode: String, errorDescription: String): OpvragenResultaatVerwerkingMeldingSessieResponse {
    val fout = FoutDetails()
    fout.foutCode = errorCode
    fout.foutOmschrijving = errorDescription

    val responseDetails = OpvragenResultaatVerwerkingMeldingSessieResponseDetails()
    responseDetails.getAanvraagFoutOrStatusMeldingSessie().add(fout)

    val response = OpvragenResultaatVerwerkingMeldingSessieResponse()
    response.opvragenResultaatVerwerkingMeldingSessieResponseDetails = responseDetails
    return response
  }

  private fun createResponseWithEersteOntvangstMeldingen(): OpvragenResultaatVerwerkingMeldingSessieResponse {
    val melding1 = VerwerkingEersteOntvangstMeldingDetails()
    melding1.meldingsNummerMelder = declarationId1
    melding1.meldingUUID = meldingUUID1
    melding1.isTechnischAkkoord = true

    val melding2 = VerwerkingEersteOntvangstMeldingDetails()
    melding2.meldingsNummerMelder = declarationId2
    melding2.meldingUUID = meldingUUID2
    melding2.isTechnischAkkoord = true

    val statusDetails = StatusMeldingSessieDetails()
    statusDetails.getEersteOntvangstMeldingen().addAll(listOf(melding1, melding2))

    val responseDetails = OpvragenResultaatVerwerkingMeldingSessieResponseDetails()
    responseDetails.getAanvraagFoutOrStatusMeldingSessie().add(statusDetails)

    val response = OpvragenResultaatVerwerkingMeldingSessieResponse()
    response.opvragenResultaatVerwerkingMeldingSessieResponseDetails = responseDetails
    return response
  }

  private fun createResponseWithEersteOntvangstMeldingenWithErrors(): OpvragenResultaatVerwerkingMeldingSessieResponse {
    val fout = FoutDetails()
    fout.foutCode = "ValidationError"
    fout.foutOmschrijving = "Invalid waste code"

    val melding = VerwerkingEersteOntvangstMeldingDetails()
    melding.meldingsNummerMelder = declarationId1
    melding.meldingUUID = meldingUUID1
    melding.isTechnischAkkoord = true
    melding.getMeldingFouten().add(fout)

    val statusDetails = StatusMeldingSessieDetails()
    statusDetails.getEersteOntvangstMeldingen().add(melding)

    val responseDetails = OpvragenResultaatVerwerkingMeldingSessieResponseDetails()
    responseDetails.getAanvraagFoutOrStatusMeldingSessie().add(statusDetails)

    val response = OpvragenResultaatVerwerkingMeldingSessieResponse()
    response.opvragenResultaatVerwerkingMeldingSessieResponseDetails = responseDetails
    return response
  }

  private fun createResponseWithTechnischAkkoordFalse(): OpvragenResultaatVerwerkingMeldingSessieResponse {
    val melding = VerwerkingEersteOntvangstMeldingDetails()
    melding.meldingsNummerMelder = declarationId1
    melding.meldingUUID = meldingUUID1
    melding.isTechnischAkkoord = false

    val statusDetails = StatusMeldingSessieDetails()
    statusDetails.getEersteOntvangstMeldingen().add(melding)

    val responseDetails = OpvragenResultaatVerwerkingMeldingSessieResponseDetails()
    responseDetails.getAanvraagFoutOrStatusMeldingSessie().add(statusDetails)

    val response = OpvragenResultaatVerwerkingMeldingSessieResponse()
    response.opvragenResultaatVerwerkingMeldingSessieResponseDetails = responseDetails
    return response
  }

  private fun createResponseWithMaandelijkseOntvangstMeldingen(): OpvragenResultaatVerwerkingMeldingSessieResponse {
    val melding = VerwerkingMaandelijkseOntvangstMeldingDetails()
    melding.meldingsNummerMelder = declarationId1
    melding.meldingUUID = meldingUUID1
    melding.isTechnischAkkoord = true

    val statusDetails = StatusMeldingSessieDetails()
    statusDetails.getMaandelijkseOntvangstMeldingen().add(melding)

    val responseDetails = OpvragenResultaatVerwerkingMeldingSessieResponseDetails()
    responseDetails.getAanvraagFoutOrStatusMeldingSessie().add(statusDetails)

    val response = OpvragenResultaatVerwerkingMeldingSessieResponse()
    response.opvragenResultaatVerwerkingMeldingSessieResponseDetails = responseDetails
    return response
  }

  private fun createResponseWithAfgifteMeldingen(): OpvragenResultaatVerwerkingMeldingSessieResponse {
    val melding = VerwerkingAfgifteMeldingDetails()
    melding.meldingsNummerMelder = declarationId1
    melding.meldingUUID = meldingUUID1
    melding.isTechnischAkkoord = true

    val statusDetails = StatusMeldingSessieDetails()
    statusDetails.getAfgifteMeldingen().add(melding)

    val responseDetails = OpvragenResultaatVerwerkingMeldingSessieResponseDetails()
    responseDetails.getAanvraagFoutOrStatusMeldingSessie().add(statusDetails)

    val response = OpvragenResultaatVerwerkingMeldingSessieResponse()
    response.opvragenResultaatVerwerkingMeldingSessieResponseDetails = responseDetails
    return response
  }

  private fun createResponseWithMixedResults(): OpvragenResultaatVerwerkingMeldingSessieResponse {
    val fout = FoutDetails()
    fout.foutCode = "ValidationError"
    fout.foutOmschrijving = "Invalid data"

    val melding1 = VerwerkingEersteOntvangstMeldingDetails()
    melding1.meldingsNummerMelder = declarationId1
    melding1.meldingUUID = meldingUUID1
    melding1.isTechnischAkkoord = true

    val melding2 = VerwerkingEersteOntvangstMeldingDetails()
    melding2.meldingsNummerMelder = declarationId2
    melding2.meldingUUID = meldingUUID2
    melding2.isTechnischAkkoord = false
    melding2.getMeldingFouten().add(fout)

    val statusDetails = StatusMeldingSessieDetails()
    statusDetails.getEersteOntvangstMeldingen().addAll(listOf(melding1, melding2))

    val responseDetails = OpvragenResultaatVerwerkingMeldingSessieResponseDetails()
    responseDetails.getAanvraagFoutOrStatusMeldingSessie().add(statusDetails)

    val response = OpvragenResultaatVerwerkingMeldingSessieResponse()
    response.opvragenResultaatVerwerkingMeldingSessieResponseDetails = responseDetails
    return response
  }
}
