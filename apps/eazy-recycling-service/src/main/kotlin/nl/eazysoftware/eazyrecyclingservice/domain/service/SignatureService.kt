package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.repository.SignaturesRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.SignaturesDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Service
class SignatureService(
    private val signaturesRepository: SignaturesRepository,
) {
    fun getSignatureStatuses(id: UUID): SignatureStatusView =
        signaturesRepository.findByIdOrNull(id)
            ?.let { signatures -> SignatureStatusView(
                id,
                consignorSigned = signatures.consignorSignature != null,
                carrierSigned = signatures.carrierSignature != null,
                consigneeSigned = signatures.consigneeSignature != null,
                pickupSigned = signatures.pickupSignature != null,
            )}
            ?: SignatureStatusView(id,
                consignorSigned = false,
                carrierSigned = false,
                consigneeSigned = false,
                pickupSigned = false
            )

    fun saveSignature(id: UUID, request: CreateSignatureRequest): Any {
        val signatures = signaturesRepository.findByIdOrNull(id) ?: SignaturesDto(transportId = id)

        when (request.party) {
            "consignor" -> {
                signatures.consignorSignature = request.signature
                signatures.consignorEmail = request.email
                signatures.consignorSignedAt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"))
            }
            "consignee" -> {
                signatures.consigneeSignature = request.signature
                signatures.consigneeEmail = request.email
                signatures.consigneeSignedAt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"))
            }
            "carrier" -> {
                signatures.carrierSignature = request.signature
                signatures.carrierEmail = request.email
                signatures.carrierSignedAt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"))
            }
            "pickup" -> {
                signatures.pickupSignature = request.signature
                signatures.pickupEmail = request.email
                signatures.pickupSignedAt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"))
            }
            else -> throw IllegalArgumentException("Ongeldige partij: ${request.party}")
        }

        signaturesRepository.save(signatures)

        return getSignatureStatuses(id)
    }
}

data class CreateSignatureRequest(
    val signature: String,
    val email: String,
    val party: String,
)

data class SignatureStatusView(
    val transportId: UUID,
    val consignorSigned: Boolean,
    val carrierSigned: Boolean,
    val consigneeSigned: Boolean,
    val pickupSigned: Boolean,
)
