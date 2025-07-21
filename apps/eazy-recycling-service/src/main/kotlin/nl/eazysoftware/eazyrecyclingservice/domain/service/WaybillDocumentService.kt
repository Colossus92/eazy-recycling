package nl.eazysoftware.eazyrecyclingservice.domain.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import nl.eazysoftware.eazyrecyclingservice.controller.transport.signature.LatestPdfResponse
import nl.eazysoftware.eazyrecyclingservice.repository.SignaturesRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.SignaturesDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.imageio.ImageIO

private const val bucket = "waybills"

@Service
class WaybillDocumentService(
    private val signaturesRepository: SignaturesRepository,
    private val supabaseClient: SupabaseClient,
    private val pdfGenerationClient: PdfGenerationClient,
    private val transportService: TransportService,
    private val storageClient: StorageClient,
) {
    private val logger = LoggerFactory.getLogger(WaybillDocumentService::class.java)

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
                signatures.consignorSignedAt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"))
            }

            "consignee" -> {
                signatures.consigneeEmail = request.email
                signatures.consigneeSignedAt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"))
            }

            "carrier" -> {
                signatures.carrierEmail = request.email
                signatures.carrierSignedAt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"))
            }

            "pickup" -> {
                signatures.pickupEmail = request.email
                signatures.pickupSignedAt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"))
            }

            else -> throw IllegalArgumentException("Ongeldige partij: ${request.party}")
        }

        signaturesRepository.save(signatures)

        storageClient.saveSignature(id, request.signature, request.party)
        pdfGenerationClient.triggerPdfGeneration(id, request.party)

        return getSignatureStatuses(id)
    }

    suspend fun getLatestPdf(id: UUID): LatestPdfResponse {
        try {
            val bucket = supabaseClient.storage.from(bucket)
            // The path should be the folder structure within the bucket
            val path = "waybills/$id" // This will look for files under waybills/{uuid}/

            logger.info("Fetching PDF files for transport ID: $id from path: $path")
            val files = bucket.list(path)
            logger.info("Found ${files.size} files in path $path")

            if (files.isEmpty()) {
                logger.info("No PDF files found for transport ID: $id")
                return LatestPdfResponse(
                    url = null,
                    thumbnail = null
                )
            }

            // Get the most recently updated file
            val latestFile = files
                .sortedByDescending { it.updatedAt }
                .first()

            logger.info("Latest PDF file found: ${latestFile.name}, updated at: ${latestFile.updatedAt}")

            // Generate a public URL for the file
            val fileUrl = bucket.authenticatedUrl(path + "/" + latestFile.name)

            // Generate thumbnail
            val thumbnail = createThumbnail(path, latestFile.name)

            return LatestPdfResponse(
                url = fileUrl,
                thumbnail = thumbnail
            )
        } catch (e: Exception) {
            logger.error("Error retrieving PDF for transport ID: $id", e)
            return LatestPdfResponse(
                url = null,
                thumbnail = null
            )
        }
    }

    /**
     * Creates a thumbnail from a PDF file and returns it as a Base64 encoded string
     */
    suspend fun createThumbnail(path: String, name: String): String? {
        try {
            val bytes = supabaseClient.storage.from(bucket).downloadAuthenticated("$path/$name")
            val inputPdf = File.createTempFile("waybill", ".pdf")
            val outputImage = File.createTempFile("thumbnail", ".jpg")

            try {
                // Write the downloaded bytes to a temporary file
                inputPdf.writeBytes(bytes)

                // Create ByteArrayOutputStream to store the image data
                val baos = ByteArrayOutputStream()

                PDDocument.load(inputPdf).use { document ->
                    val renderer = PDFRenderer(document)

                    // Render the first page at 100 DPI
                    val image: BufferedImage = renderer.renderImageWithDPI(0, 100f)

                    // Write to ByteArrayOutputStream instead of file
                    ImageIO.write(image, "jpg", baos)
                    ImageIO.write(image, "jpg", outputImage) // Also save to file for logging

                    logger.info("✅ Thumbnail created for $path/$name")
                }

                // Convert to Base64
                val thumbnailBase64 = Base64.getEncoder().encodeToString(baos.toByteArray())
                return "data:image/jpeg;base64,$thumbnailBase64"
            } finally {
                // Clean up temporary files
                inputPdf.delete()
                outputImage.delete()
            }
        } catch (e: Exception) {
            logger.error("Error creating thumbnail for $path/$name", e)
            return null
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
