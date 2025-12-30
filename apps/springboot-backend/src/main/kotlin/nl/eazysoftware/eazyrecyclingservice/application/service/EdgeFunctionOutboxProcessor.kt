package nl.eazysoftware.eazyrecyclingservice.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId
import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.EdgeFunctionName
import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.EdgeFunctionOutbox
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.EdgeFunctionOutboxRepository
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Invoices
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class EdgeFunctionOutboxProcessor(
    private val outboxRepository: EdgeFunctionOutboxRepository,
    private val supabaseClient: SupabaseClient,
    private val objectMapper: ObjectMapper,
    private val invoices: Invoices,
) {
    private val logger = LoggerFactory.getLogger(EdgeFunctionOutboxProcessor::class.java)

    @Scheduled(fixedDelayString = "\${outbox.processor.delay:5000}")
    @Transactional
    fun processOutbox() {
        val pendingEntries = outboxRepository.findPendingEntries(limit = 10)

        if (pendingEntries.isEmpty()) {
            return
        }

        logger.info("Processing ${pendingEntries.size} outbox entries")

        for (entry in pendingEntries) {
            try {
                processEntry(entry)
            } catch (e: Exception) {
                logger.error("Failed to process outbox entry ${entry.id.value}: ${e.message}", e)
                entry.markAsFailed(e.message ?: "Unknown error")
                outboxRepository.save(entry)
            }
        }
    }

    private fun processEntry(entry: EdgeFunctionOutbox) {
        logger.info("Processing outbox entry ${entry.id.value} for function ${entry.functionName}")

        entry.markAsProcessing()
        outboxRepository.save(entry)

        val functionName = getFunctionName(entry.functionName)

        try {
            val responseBody = runBlocking {
                val body = entry.payload?.let { Json.parseToJsonElement(it) } ?: kotlinx.serialization.json.buildJsonObject {}
                val response = supabaseClient.functions.invoke(
                    function = functionName,
                    body = body,
                    headers = io.ktor.http.Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
                response.bodyAsText()
            }

            handleSuccessfulResponse(entry, responseBody)
        } catch (e: Exception) {
            val errorMessage = extractErrorMessage(e.message)
            logger.error("Error calling edge function $functionName: $errorMessage", e)
            entry.markAsFailed(errorMessage)
            outboxRepository.save(entry)
        }
    }

    private fun handleSuccessfulResponse(entry: EdgeFunctionOutbox, responseBody: String?) {
        val response = responseBody?.let { parseResponse(it) }

        if (response?.success == true) {
            logger.info("Edge function ${entry.functionName} completed successfully for entry ${entry.id.value}. Storage path: ${response.storagePath}")
            entry.markAsCompleted()

            if (entry.functionName == EdgeFunctionName.INVOICE_PDF_GENERATOR && entry.aggregateId != null && response.storagePath != null) {
                updateInvoicePdfUrl(entry.aggregateId!!, response.storagePath!!)
            }
        } else {
            val errorMessage = response?.error ?: response?.message ?: "Unknown error"
            logger.error("Edge function ${entry.functionName} returned error for entry ${entry.id.value}: $errorMessage")
            entry.markAsFailed(errorMessage)
        }

        outboxRepository.save(entry)
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
        }
    }

    private fun extractErrorMessage(responseBody: String?): String {
        if (responseBody.isNullOrBlank()) {
            return "No error message"
        }

        return try {
            val response = parseResponse(responseBody)
            response?.error ?: response?.message ?: responseBody
        } catch (e: Exception) {
            responseBody
        }
    }

    private fun parseResponse(responseBody: String): EdgeFunctionResponse? {
        return try {
            objectMapper.readValue(responseBody, EdgeFunctionResponse::class.java)
        } catch (e: Exception) {
            logger.warn("Failed to parse edge function response: ${e.message}")
            null
        }
    }

    private fun getFunctionName(functionName: EdgeFunctionName): String {
        return when (functionName) {
            EdgeFunctionName.INVOICE_PDF_GENERATOR -> "invoice-pdf-generator"
        }
    }
}

data class EdgeFunctionResponse(
    val success: Boolean?,
    val message: String?,
    val storagePath: String?,
    val error: String?,
)
