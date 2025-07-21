package nl.eazysoftware.eazyrecyclingservice.domain.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

private const val bucket = "waybills"


@Component
class StorageClient(
    private val supabaseClient: SupabaseClient,
) {

    private val logger = LoggerFactory.getLogger(StorageClient::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun saveSignature(id: UUID, signature: String, party: String) {
        coroutineScope.launch {
            try {
                val signaturePath = "signatures/${id}"
                val signatureBytes = Base64.getDecoder().decode(signature.substringAfter("base64,"))

                logger.info("Storing signature for transport ID: $id, party: $party at path: $signaturePath")
                supabaseClient.storage.from(bucket).upload(
                    path = "$signaturePath/${party}.png",
                    data = signatureBytes,
                ) {
                    upsert = true
                }

                logger.info("Signature stored successfully at $signaturePath/$party.png")
            } catch (e: Exception) {
                logger.error("Error storing signature for transport ID: $id, party: $party", e)
                // Continue with PDF generation even if signature storage fails
            }
        }
    }

    fun listSignatures(id: UUID): List<String> {
        return runBlocking {
            supabaseClient.storage.from(bucket).list(prefix = "signatures/$id")
                .map { it.name }
        }
    }
}