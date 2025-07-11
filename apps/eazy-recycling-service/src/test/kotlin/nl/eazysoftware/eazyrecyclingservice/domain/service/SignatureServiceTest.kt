package nl.eazysoftware.eazyrecyclingservice.domain.service

import io.github.jan.supabase.SupabaseClient
import nl.eazysoftware.eazyrecyclingservice.repository.SignaturesRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.SignaturesDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SignatureServiceTest {

    @Mock
    private lateinit var signaturesRepository: SignaturesRepository

    @Mock
    private lateinit var supabase: SupabaseClient

    @Mock
    private lateinit var transportService: TransportService

    private lateinit var waybillDocumentService: WaybillDocumentService


    private val testId = UUID.randomUUID()
    private val testSignature = "base64EncodedSignature"
    private val testEmail = "test@example.com"
    private val testDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"))

    @BeforeEach
    fun setUp() {
        waybillDocumentService = WaybillDocumentService(signaturesRepository, supabase, transportService)
    }

    @Test
    fun `getSignatureStatuses should return all signatures as false when no signatures exist`() {
        // Given
        `when`(signaturesRepository.findById(testId)).thenReturn(Optional.empty())

        // When
        val result = waybillDocumentService.getSignatureStatuses(testId)

        // Then
        assertEquals(testId, result.transportId)
        assertFalse(result.consignorSigned)
        assertFalse(result.carrierSigned)
        assertFalse(result.consigneeSigned)
        assertFalse(result.pickupSigned)
    }

    @Test
    fun `getSignatureStatuses should return correct status when all signatures exist`() {
        // Given
        val signatures = SignaturesDto(
            transportId = testId,
            consignorSignature = testSignature,
            consignorEmail = testEmail,
            consignorSignedAt = testDateTime,
            pickupSignature = testSignature,
            pickupEmail = testEmail,
            pickupSignedAt = testDateTime,
            carrierSignature = testSignature,
            carrierEmail = testEmail,
            carrierSignedAt = testDateTime,
            consigneeSignature = testSignature,
            consigneeEmail = testEmail,
            consigneeSignedAt = testDateTime
        )
        `when`(signaturesRepository.findById(testId)).thenReturn(Optional.of(signatures))

        // When
        val result = waybillDocumentService.getSignatureStatuses(testId)

        // Then
        assertEquals(testId, result.transportId)
        assertTrue(result.consignorSigned)
        assertTrue(result.carrierSigned)
        assertTrue(result.consigneeSigned)
        assertTrue(result.pickupSigned)
    }

    @Test
    fun `getSignatureStatuses should return correct status when only some signatures exist`() {
        // Given
        val signatures = SignaturesDto(
            transportId = testId,
            consignorSignature = testSignature,
            consignorEmail = testEmail,
            consignorSignedAt = testDateTime,
            pickupSignature = null,
            pickupEmail = null,
            pickupSignedAt = null,
            carrierSignature = testSignature,
            carrierEmail = testEmail,
            carrierSignedAt = testDateTime,
            consigneeSignature = null,
            consigneeEmail = null,
            consigneeSignedAt = null
        )
        `when`(signaturesRepository.findById(testId)).thenReturn(Optional.of(signatures))

        // When
        val result = waybillDocumentService.getSignatureStatuses(testId)

        // Then
        assertEquals(testId, result.transportId)
        assertTrue(result.consignorSigned)
        assertTrue(result.carrierSigned)
        assertFalse(result.consigneeSigned)
        assertFalse(result.pickupSigned)
    }

    @Test
    fun `getSignatureStatuses should handle empty signature strings correctly`() {
        // Given
        val signatures = SignaturesDto(
            transportId = testId,
            consignorSignature = "",  // Empty string should be treated as signed
            consignorEmail = testEmail,
            consignorSignedAt = testDateTime,
            pickupSignature = null,
            pickupEmail = null,
            pickupSignedAt = null,
            carrierSignature = "   ", // Whitespace should be treated as signed
            carrierEmail = testEmail,
            carrierSignedAt = testDateTime,
            consigneeSignature = null,
            consigneeEmail = null,
            consigneeSignedAt = null
        )
        `when`(signaturesRepository.findById(testId)).thenReturn(Optional.of(signatures))

        // When
        val result = waybillDocumentService.getSignatureStatuses(testId)

        // Then
        assertEquals(testId, result.transportId)
        assertTrue(result.consignorSigned)
        assertTrue(result.carrierSigned)
        assertFalse(result.consigneeSigned)
        assertFalse(result.pickupSigned)
    }

    @Test
    fun `saveSignature should throw IllegalStateException when transport type is CONTAINER`() {
        // Given
        val transportId = UUID.randomUUID()
        val transport = TransportDto(
            id = transportId,
            transportType = TransportType.CONTAINER,
            consignorParty = mock(),
            carrierParty = mock(),
            pickupLocation = mock(),
            pickupDateTime = LocalDateTime.now(),
            deliveryLocation = mock(),
            deliveryDateTime = LocalDateTime.now(),
            note = "",
            pickupCompany = mock(),
            deliveryCompany = mock(),
            driver = mock(),
            sequenceNumber = 1
        )
        
        val request = CreateSignatureRequest(
            party = "consignor",
            signature = testSignature,
            email = testEmail
        )
        
        `when`(transportService.getTransportById(transportId)).thenReturn(transport)
        
        // When & Then
        val exception = assertThrows(IllegalStateException::class.java) {
            waybillDocumentService.saveSignature(transportId, request)
        }
        
        // Verify the exception message contains the transport type
        assertTrue(exception.message!!.contains(TransportType.CONTAINER.toString()))
    }
}
