package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.config.clock.TimeConfiguration
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.SignaturesRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.SignaturesDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.*

@Service
class WaybillDocumentService(
    private val signaturesRepository: SignaturesRepository,
    private val pdfGenerationClient: PdfGenerationClient,
    private val transportService: TransportService,
    private val storageClient: StorageClient,
) {

    fun getSignatureStatuses(id: UUID): SignatureStatusView =
        storageClient.listSignatures(id)
            .let { signatures ->
                SignatureStatusView(
                    id,
                    consignorSigned = signatures.contains("consignor.png"),
                    carrierSigned = signatures.contains("carrier.png"),
                    consigneeSigned = signatures.contains("consignee.png"),
                    pickupSigned = signatures.contains("pickup.png"),
                )
            }

    fun saveSignature(id: UUID, request: CreateSignatureRequest): SignatureStatusView {
        val transport = transportService.getTransportById(id)

        if (transport.transportType != TransportType.WASTE) {
            throw IllegalStateException("Niet mogelijk om te signeren voor transport met type ${transport.transportType}")
        }

        val signatures = signaturesRepository.findByIdOrNull(id) ?: SignaturesDto(transportId = id)

        when (request.party) {
            "consignor" -> {
                signatures.consignorEmail = request.email
                signatures.consignorSignedAt = ZonedDateTime.now(TimeConfiguration.DISPLAY_ZONE_ID)
            }

            "consignee" -> {
                signatures.consigneeEmail = request.email
                signatures.consigneeSignedAt = ZonedDateTime.now(TimeConfiguration.DISPLAY_ZONE_ID)
            }

            "carrier" -> {
                signatures.carrierEmail = request.email
                signatures.carrierSignedAt = ZonedDateTime.now(TimeConfiguration.DISPLAY_ZONE_ID)
            }

            "pickup" -> {
                signatures.pickupEmail = request.email
                signatures.pickupSignedAt = ZonedDateTime.now(TimeConfiguration.DISPLAY_ZONE_ID)
            }

            else -> throw IllegalArgumentException("Ongeldige partij: ${request.party}")
        }

        signaturesRepository.save(signatures)

        storageClient.saveSignature(id, request.signature, request.party)
        pdfGenerationClient.triggerPdfGeneration(id, request.party)

        return getSignatureStatuses(id)
    }
}

data class CreateSignatureRequest(
    val signature: String,
    @field:Email
    val email: String,
    @field:NotBlank
    val party: String,
)

data class SignatureStatusView(
    val transportId: UUID,
    val consignorSigned: Boolean,
    val carrierSigned: Boolean,
    val consigneeSigned: Boolean,
    val pickupSigned: Boolean,
)
