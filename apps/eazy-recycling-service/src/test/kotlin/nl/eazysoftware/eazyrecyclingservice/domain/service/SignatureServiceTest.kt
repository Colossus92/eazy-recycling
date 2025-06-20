package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.repository.SignaturesRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.SignaturesDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SignatureServiceTest {

    @Mock
    private lateinit var signaturesRepository: SignaturesRepository

    private lateinit var signatureService: SignatureService

    private val testId = UUID.randomUUID()
    private val testSignature = "base64EncodedSignature"
    private val testEmail = "test@example.com"
    private val testDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"))

    @BeforeEach
    fun setUp() {
        signatureService = SignatureService(signaturesRepository)
    }

    @Test
    fun `getSignatureStatuses should return all signatures as false when no signatures exist`() {
        // Given
        `when`(signaturesRepository.findById(testId)).thenReturn(Optional.empty())

        // When
        val result = signatureService.getSignatureStatuses(testId)

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
        val result = signatureService.getSignatureStatuses(testId)

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
        val result = signatureService.getSignatureStatuses(testId)

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
        val result = signatureService.getSignatureStatuses(testId)

        // Then
        assertEquals(testId, result.transportId)
        assertTrue(result.consignorSigned)
        assertTrue(result.carrierSigned)
        assertFalse(result.consigneeSigned)
        assertFalse(result.pickupSigned)
    }
}
