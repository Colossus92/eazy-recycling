package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.SignaturesRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.SignaturesDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
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
        whenever(storageClient.listSignatures(testId)).thenReturn(
            listOf(
                CONSIGNOR_PNG,
                CARRIER_PNG,
                PICKUP_PNG,
                CONSIGNEE_PNG
            )
        )
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

    @Test
    fun `saveSignature should throw exception when transport type is not WASTE`() {
        val id = UUID.randomUUID()
        val request = mock<CreateSignatureRequest>()
        val transport = mock<TransportDto>()

        whenever(transportService.getTransportById(id)).thenReturn(transport)
        whenever(transport.transportType).thenReturn(TransportType.CONTAINER)

        assertThrows(IllegalStateException::class.java) { waybillDocumentService.saveSignature(id, request) }
        verifyNoInteractions(signaturesRepository)
        verifyNoInteractions(storageClient)
        verifyNoInteractions(pdfGenerationClient)
    }

    @Test
    fun `saveSignature should throw exception when invalid party`() {
        val id = UUID.randomUUID()
        val request = mock<CreateSignatureRequest>()
        val transport = mock<TransportDto>()

        whenever(transportService.getTransportById(id)).thenReturn(transport)
        whenever(transport.transportType).thenReturn(TransportType.WASTE)
        whenever(request.party).thenReturn("invalid")

        assertThrows(IllegalArgumentException::class.java) { waybillDocumentService.saveSignature(id, request) }
        verify(signaturesRepository, never()).save(any())
        verifyNoInteractions(storageClient)
        verifyNoInteractions(pdfGenerationClient)
    }

    @Nested
    inner class SaveSignature {

        private lateinit var captor: ArgumentCaptor<SignaturesDto>
        private val id: UUID = UUID.randomUUID()

        @BeforeEach
        fun setUp() {
            captor = ArgumentCaptor.forClass(SignaturesDto::class.java)
            val transport = mock<TransportDto>()
            whenever(transport.transportType).thenReturn(TransportType.WASTE)
            whenever(transportService.getTransportById(id)).thenReturn(transport)
        }

        @Test
        fun `saveSignature creates new signature object with consignor when no signatures exist`() {
            val request = CreateSignatureRequest(
                party = "consignor",
                signature = testSignature,
                email = testEmail
            )

            waybillDocumentService.saveSignature(id, request)

            verify(signaturesRepository).save(captor.capture())

            captor.value.run {
                assertEquals(testEmail, consignorEmail)
                assertNotNull(consignorSignedAt)
                assertNull(consigneeEmail)
                assertNull(consigneeSignedAt)
                assertNull(carrierEmail)
                assertNull(carrierSignedAt)
                assertNull(pickupEmail)
                assertNull(pickupSignedAt)
            }
            verify(storageClient).saveSignature(id, request.signature, request.party)
            pdfGenerationClient.triggerPdfGeneration(id, request.party)
        }

        @Test
        fun `saveSignature creates new signature object with consignee when no signatures exist`() {
            val request = CreateSignatureRequest(
                party = "consignee",
                signature = testSignature,
                email = testEmail
            )

            waybillDocumentService.saveSignature(id, request)

            verify(signaturesRepository).save(captor.capture())

            captor.value.run {
                assertEquals(testEmail, consigneeEmail)
                assertNotNull(consigneeSignedAt)
                assertNull(consignorEmail)
                assertNull(consignorSignedAt)
                assertNull(carrierEmail)
                assertNull(carrierSignedAt)
                assertNull(pickupEmail)
                assertNull(pickupSignedAt)
            }
            verify(storageClient).saveSignature(id, request.signature, request.party)
            pdfGenerationClient.triggerPdfGeneration(id, request.party)
        }

        @Test
        fun `saveSignature creates new signature object with carrier when no signatures exist`() {
            val request = CreateSignatureRequest(
                party = "carrier",
                signature = testSignature,
                email = testEmail
            )

            waybillDocumentService.saveSignature(id, request)

            verify(signaturesRepository).save(captor.capture())

            captor.value.run {
                assertEquals(testEmail, carrierEmail)
                assertNotNull(carrierSignedAt)
                assertNull(consignorEmail)
                assertNull(consignorSignedAt)
                assertNull(consigneeEmail)
                assertNull(consigneeSignedAt)
                assertNull(pickupEmail)
                assertNull(pickupSignedAt)
            }
            verify(storageClient).saveSignature(id, request.signature, request.party)
            pdfGenerationClient.triggerPdfGeneration(id, request.party)
        }

        @Test
        fun `saveSignature creates new signature object with pickup when no signatures exist`() {
            val request = CreateSignatureRequest(
                party = "pickup",
                signature = testSignature,
                email = testEmail
            )

            waybillDocumentService.saveSignature(id, request)

            verify(signaturesRepository).save(captor.capture())

            captor.value.run {
                assertEquals(testEmail, pickupEmail)
                assertNotNull(pickupSignedAt)
                assertNull(consignorEmail)
                assertNull(consignorSignedAt)
                assertNull(consigneeEmail)
                assertNull(consigneeSignedAt)
                assertNull(carrierEmail)
                assertNull(carrierSignedAt)
            }
            verify(storageClient).saveSignature(id, request.signature, request.party)
            pdfGenerationClient.triggerPdfGeneration(id, request.party)
        }

        @Test
        fun `saveSignature updates existing signature object`() {
            val request = CreateSignatureRequest(
                party = "consignor",
                signature = testSignature,
                email = "different@email.com"
            )
            val signedAt = ZonedDateTime.of(
                LocalDateTime.of(
                    1992,
                    1,
                    29,
                    6,
                    0,
                    0
                ),
                ZoneId.of("Europe/Amsterdam")
            )

            whenever(signaturesRepository.findById(id)).thenReturn(
                Optional.of(
                    SignaturesDto(
                        transportId = id,
                        consignorEmail = testEmail,
                        consignorSignedAt = signedAt,
                        consigneeEmail = testEmail,
                        consigneeSignedAt = signedAt,
                        carrierEmail = testEmail,
                        carrierSignedAt = signedAt,
                        pickupEmail = testEmail,
                        pickupSignedAt = signedAt
                    )
                )
            )

            waybillDocumentService.saveSignature(id, request)
            verify(signaturesRepository).save(captor.capture())

            captor.value.run {
                assertEquals("different@email.com", consignorEmail)
                assertThat(consignorSignedAt).isAfter(signedAt)
                assertEquals(testEmail, consigneeEmail)
                assertEquals(consigneeSignedAt, signedAt)
                assertEquals(testEmail, carrierEmail)
                assertEquals(carrierSignedAt, signedAt)
                assertEquals(testEmail, pickupEmail)
                assertEquals(pickupSignedAt, signedAt)
            }
            verify(storageClient).saveSignature(id, request.signature, request.party)
            pdfGenerationClient.triggerPdfGeneration(id, request.party)
        }
    }


}
