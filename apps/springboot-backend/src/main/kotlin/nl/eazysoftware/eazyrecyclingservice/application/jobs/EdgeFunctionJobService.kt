package nl.eazysoftware.eazyrecyclingservice.application.jobs

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import org.jobrunr.jobs.annotations.Job
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * JobRunr-based service for executing Supabase Edge Function calls.
 * This service is designed to be extensible for different edge functions.
 */
@Service
class EdgeFunctionJobService(
    private val supabaseClient: SupabaseClient,
    private val invoices: Invoices,
) {
    private val logger = LoggerFactory.getLogger(EdgeFunctionJobService::class.java)

    /**
     * Execute a send-email edge function call.
     * This method is enqueued by JobRunr and executed asynchronously.
     *
     * @param invoiceId The ID of the invoice to mark as sent after successful email delivery
     * @param emailPayload JSON string containing email parameters (to, bcc, subject, body, etc.)
     */
    @Job(name = "Send Invoice Email", retries = 3)
    @Transactional
    fun executeSendEmailJob(invoiceId: String, emailPayload: String) {
        logger.info("Executing send-email job for invoice $invoiceId")

        try {
            val responseBody = invokeSendEmailFunction(emailPayload)
            logger.info("Email sent successfully for invoice $invoiceId: $responseBody")

            // Mark invoice as sent
            markInvoiceAsSent(invoiceId)
        } catch (e: Exception) {
            logger.error("Failed to send email for invoice $invoiceId: ${e.message}", e)
            throw e // JobRunr will retry based on @Job annotation
        }
    }

    /**
     * Generic method to invoke any Supabase edge function.
     * Can be extended for other edge function types in the future.
     *
     * @param functionName The name of the edge function to invoke
     * @param payload JSON string payload to send to the function
     * @return Response body as string
     */
    private fun invokeEdgeFunction(functionName: String, payload: String): String {
        return runBlocking {
            val body = Json.parseToJsonElement(payload)
            val response = supabaseClient.functions.invoke(
                function = functionName,
                body = body,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                }
            )
            response.bodyAsText()
        }
    }

    private fun invokeSendEmailFunction(payload: String): String {
        return invokeEdgeFunction("send-email", payload)
    }

    private fun markInvoiceAsSent(invoiceId: String) {
        try {
            val id = InvoiceId(UUID.fromString(invoiceId))
            val invoice = invoices.findById(id)
            if (invoice != null) {
                invoice.markAsSent()
                invoices.save(invoice)
                logger.info("Marked invoice ${invoice.invoiceNumber?.value} (id: $invoiceId) as SENT")
            } else {
                logger.warn("Invoice $invoiceId not found when trying to mark as sent")
            }
        } catch (e: Exception) {
            logger.error("Failed to mark invoice $invoiceId as sent: ${e.message}", e)
        }
    }

    /**
     * Execute an invoice PDF generation edge function call.
     * This method is enqueued by JobRunr and executed asynchronously.
     *
     * @param invoiceId The ID of the invoice to update with the PDF URL after successful generation
     * @param pdfPayload JSON string containing PDF generation parameters
     */
    @Job(name = "Generate Invoice PDF", retries = 3)
    @Transactional
    fun executeGeneratePdfJob(invoiceId: String, pdfPayload: String) {
        logger.info("Executing invoice PDF generation job for invoice $invoiceId")

        try {
            val responseBody = invokeInvoicePdfGeneratorFunction(pdfPayload)
            val response = parseResponse(responseBody)

            if (response?.success == true && response.storagePath != null) {
                logger.info("Invoice PDF generated successfully for invoice $invoiceId. Storage path: ${response.storagePath}")
                updateInvoicePdfUrl(invoiceId, response.storagePath)
            } else {
                val errorMessage = response?.error ?: response?.message ?: "Unknown error"
                logger.error("Invoice PDF generation failed for invoice $invoiceId: $errorMessage")
                throw RuntimeException("PDF generation failed: $errorMessage")
            }
        } catch (e: Exception) {
            logger.error("Failed to generate PDF for invoice $invoiceId: ${e.message}", e)
            throw e
        }
    }

    private fun invokeInvoicePdfGeneratorFunction(payload: String): String {
        return invokeEdgeFunction("invoice-pdf-generator", payload)
    }

    private fun updateInvoicePdfUrl(invoiceId: String, storagePath: String) {
        try {
            val id = InvoiceId(UUID.fromString(invoiceId))
            val invoice = invoices.findById(id)
            if (invoice != null) {
                invoice.pdfUrl = storagePath
                invoices.save(invoice)
                logger.info("Updated invoice $invoiceId with PDF URL: $storagePath")
            } else {
                logger.warn("Invoice $invoiceId not found when trying to update PDF URL")
            }
        } catch (e: Exception) {
            logger.error("Failed to update invoice $invoiceId with PDF URL: ${e.message}", e)
            throw e
        }
    }

    private fun parseResponse(responseBody: String): EdgeFunctionResponse? {
        return try {
            val response = Json.decodeFromString<EdgeFunctionResponse>(responseBody)
            response
        } catch (e: Exception) {
            logger.warn("Failed to parse edge function response: ${e.message}")
            null
        }
    }

    /**
     * Execute a waybill PDF generation edge function call.
     * This method is enqueued by JobRunr and executed asynchronously.
     *
     * @param transportId The ID of the transport to generate the waybill PDF for
     * @param partyType The party type (e.g., "empty", "consignor", "carrier", "pickup")
     */
    @Job(name = "Generate Waybill PDF: %0 - %1", retries = 3)
    fun executeGenerateWaybillPdfJob(transportId: String, partyType: String) {
        logger.info("Executing waybill PDF generation job for transport $transportId, party type: $partyType")

        try {
            val payload = buildJsonObject {
                put("transportId", transportId)
                put("partyType", partyType)
            }

            val responseBody = runBlocking {
                val response = supabaseClient.functions.invoke(
                    function = "pdf-generator",
                    body = payload,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
                response.bodyAsText()
            }

            logger.info("Waybill PDF generation completed successfully for transport $transportId, party type: $partyType. Response: $responseBody")
        } catch (e: Exception) {
            logger.error("Failed to generate waybill PDF for transport $transportId, party type: $partyType: ${e.message}", e)
            throw e
        }
    }
}

@Serializable
data class EdgeFunctionResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val storagePath: String? = null,
    val error: String? = null,
)
