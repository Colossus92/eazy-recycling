package nl.eazysoftware.eazyrecyclingservice.domain.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import nl.eazysoftware.eazyrecyclingservice.repository.SignaturesRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.SignaturesDto
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@Service
class SignatureService(
    private val signaturesRepository: SignaturesRepository,
    private val supabaseClient: SupabaseClient
) {
    private val logger = LoggerFactory.getLogger(SignatureService::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

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

    fun saveSignature(id: UUID, request: CreateSignatureRequest): SignatureStatusView {
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

        triggerPdfGeneration(id, request.party)

        return getSignatureStatuses(id)
    }
    
    /**
     * Triggers the Supabase edge function to generate and send a PDF asynchronously.
     * The transaction does not wait for this function to complete.
     */
    private fun triggerPdfGeneration(transportId: UUID, partyType: String) {
        coroutineScope.launch {
            try {
                logger.info("Triggering PDF generation for transport ID: $transportId")
                supabaseClient.functions.invoke(
                    function = "pdf-generator",
                    body = buildJsonObject {
                        put("partyType", partyType)
                        put("transportId", transportId.toString())
                    },
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
                
                logger.info("PDF generation triggered successfully for transport ID: $transportId and party type: $partyType")
            } catch (e: Exception) {
                logger.error("Failed to trigger PDF generation for transport ID: $transportId", e)
            }
        }
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
