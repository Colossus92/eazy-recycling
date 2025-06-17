package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.repository.SignaturesRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
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

    fun saveSignature(id: UUID): Any {
        TODO("Not yet implemented")
    }
}

data class SignatureStatusView(
    val transportId: UUID,
    val consignorSigned: Boolean,
    val carrierSigned: Boolean,
    val consigneeSigned: Boolean,
    val pickupSigned: Boolean,
)
