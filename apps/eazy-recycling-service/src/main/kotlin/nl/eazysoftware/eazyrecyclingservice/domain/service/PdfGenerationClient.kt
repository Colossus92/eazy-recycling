package nl.eazysoftware.eazyrecyclingservice.domain.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class PdfGenerationClient(
    private val supabaseClient: SupabaseClient,
) {

    private val logger = LoggerFactory.getLogger(PdfGenerationClient::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Triggers the Supabase edge function to generate and send a PDF asynchronously.
     * The transaction does not wait for this function to complete.
     */
    fun triggerPdfGeneration(transportId: UUID, partyType: String) {
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