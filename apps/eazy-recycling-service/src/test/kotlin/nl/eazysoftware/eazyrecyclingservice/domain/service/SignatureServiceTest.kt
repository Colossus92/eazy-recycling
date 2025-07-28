package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.repository.SignaturesRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.*

private const val CONSIGNOR_PNG = "consignor.png"
private const val CONSIGNEE_PNG = "consignee.png"
private const val CARRIER_PNG = "carrier.png"
private const val PICKUP_PNG = "pickup.png"

@ExtendWith(MockitoExtension::class)
class SignatureServiceTest {

    @Mock
    private lateinit var signaturesRepository: SignaturesRepository

    @Mock
    private lateinit var transportService: TransportService

    @Mock
    private lateinit var pdfGenerationClient: PdfGenerationClient

    @Mock
    private lateinit var storageClient: StorageClient

    private lateinit var waybillDocumentService: WaybillDocumentService


    private val testId = UUID.randomUUID()
    private val testSignature = "base64EncodedSignature"
    private val testEmail = "test@example.com"

    @BeforeEach
    fun setUp() {
        waybillDocumentService = WaybillDocumentService(
            signaturesRepository,
            pdfGenerationClient,
            transportService,
            storageClient,
        )
    }

    @Test
    fun `getSignatureStatuses should return all signatures as false when no signatures exist`() {
        // Given
        whenever(storageClient.listSignatures(testId)).thenReturn(listOf())

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
        whenever(storageClient.listSignatures(testId)).thenReturn(listOf(CONSIGNOR_PNG, CARRIER_PNG, PICKUP_PNG, CONSIGNEE_PNG))
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
        whenever(storageClient.listSignatures(testId)).thenReturn(listOf(CONSIGNOR_PNG, CARRIER_PNG))

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
