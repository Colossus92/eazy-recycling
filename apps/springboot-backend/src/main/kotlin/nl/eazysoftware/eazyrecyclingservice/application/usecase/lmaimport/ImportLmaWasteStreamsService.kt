package nl.eazysoftware.eazyrecyclingservice.application.usecase.lmaimport

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportError
import nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportErrorCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportResult
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaImportErrors
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.*

/**
 * Service for importing waste streams from LMA CSV export files.
 */
@Service
class ImportLmaWasteStreamsService(
  private val csvParser: LmaCsvParser,
  private val wasteStreams: WasteStreams,
  private val companies: Companies,
  private val lmaImportErrors: LmaImportErrors,
  private val euralRepository: EuralRepository,
  private val processingMethodRepository: ProcessingMethodRepository
) : ImportLmaWasteStreams {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Transactional
  override fun import(csvInputStream: InputStream): LmaImportResult {
    val importBatchId = UUID.randomUUID()
    logger.info("Starting LMA import with batch ID: $importBatchId")

    val records = try {
      csvParser.parse(csvInputStream)
    } catch (e: Exception) {
      logger.error("Failed to parse CSV file", e)
      val error = LmaImportError(
        importBatchId = importBatchId,
        rowNumber = 0,
        wasteStreamNumber = null,
        errorCode = LmaImportErrorCode.INVALID_CSV_FORMAT,
        errorMessage = "Kon CSV bestand niet lezen: ${e.message}"
      )
      lmaImportErrors.save(error)
      return LmaImportResult(
        importBatchId = importBatchId,
        totalRows = 0,
        successfulImports = 0,
        skippedRows = 0,
        errorCount = 1,
        errors = listOf(error)
      )
    }

    // Deduplicate records by waste stream number (LMA export may contain duplicates)
    val uniqueRecords = records
      .filter { it.afvalstroomnummer != null }
      .distinctBy { it.afvalstroomnummer }

    logger.info("Parsed ${records.size} records, ${uniqueRecords.size} unique waste stream numbers")

    var successfulImports = 0
    var skippedRows = 0
    val errors = mutableListOf<LmaImportError>()

    for (record in uniqueRecords) {
      // Skip private consignors (not supported yet)
      if (record.isPrivateConsignor) {
        logger.debug("Skipping row ${record.rowNumber}: private consignor")
        skippedRows++
        continue
      }

      when (val result = processRecord(record, importBatchId)) {
        is ProcessResult.Success -> successfulImports++
        is ProcessResult.Skipped -> skippedRows++
        is ProcessResult.Error -> errors.add(result.error)
      }
    }

    // Save all errors at once
    if (errors.isNotEmpty()) {
      lmaImportErrors.saveAll(errors)
    }

    logger.info("LMA import completed: $successfulImports successful, $skippedRows skipped, ${errors.size} errors")

    return LmaImportResult(
      importBatchId = importBatchId,
      totalRows = uniqueRecords.size,
      successfulImports = successfulImports,
      skippedRows = skippedRows,
      errorCount = errors.size,
      errors = errors
    )
  }

  private sealed class ProcessResult {
    data object Success : ProcessResult()
    data object Skipped : ProcessResult()
    data class Error(val error: LmaImportError) : ProcessResult()
  }

  private fun processRecord(record: LmaCsvRecord, importBatchId: UUID): ProcessResult {
    val wasteStreamNumber = record.afvalstroomnummer
      ?: return createError(record, importBatchId, LmaImportErrorCode.MISSING_REQUIRED_FIELD, "Afvalstroomnummer is verplicht")

    // Check if waste stream already exists
    if (wasteStreams.existsById(WasteStreamNumber(wasteStreamNumber))) {
      logger.debug("Skipping row ${record.rowNumber}: waste stream $wasteStreamNumber already exists")
      return ProcessResult.Skipped
    }

    // Validate and get processor party
    val processorPartyId = record.verwerkersnummer
      ?: return createError(record, importBatchId, LmaImportErrorCode.MISSING_REQUIRED_FIELD, "Verwerkersnummer is verplicht")

    companies.findByProcessorId(processorPartyId)
      ?: return createError(record, importBatchId, LmaImportErrorCode.PROCESSOR_NOT_FOUND,
        "Geen verwerker gevonden met nummer: $processorPartyId")

    // Validate and get pickup party (ontdoener) by KvK number
    val kvkNumber = record.handelsregisternummerOntdoener
      ?: return createError(record, importBatchId, LmaImportErrorCode.MISSING_REQUIRED_FIELD,
        "Handelsregisternummer Ontdoener is verplicht")

    val pickupCompany = companies.findByChamberOfCommerceId(kvkNumber)
      ?: return createError(record, importBatchId, LmaImportErrorCode.COMPANY_NOT_FOUND,
        "Geen bedrijf gevonden met KvK nummer: $kvkNumber (${record.naamOntdoener ?: "onbekend"})")

    // Parse and validate eural code
    val euralCode = try {
      formatEuralCode(record.euralcode
        ?: return createError(record, importBatchId, LmaImportErrorCode.MISSING_REQUIRED_FIELD, "Euralcode is verplicht"))
    } catch (e: Exception) {
      return createError(record, importBatchId, LmaImportErrorCode.INVALID_EURAL_CODE,
        "Ongeldige euralcode: ${record.euralcode}")
    }

    // Ensure eural code exists in database, create if missing
    ensureEuralCodeExists(euralCode, record.euralcodeOmschrijving)

    // Parse and validate processing method
    val processingMethodCode = try {
      formatProcessingMethod(record.verwerkingsmethodeCode
        ?: return createError(record, importBatchId, LmaImportErrorCode.MISSING_REQUIRED_FIELD, "VerwerkingsMethode Code is verplicht"))
    } catch (e: Exception) {
      return createError(record, importBatchId, LmaImportErrorCode.INVALID_PROCESSING_METHOD,
        "Ongeldige verwerkingsmethode: ${record.verwerkingsmethodeCode}")
    }

    // Ensure processing method exists in database, create if missing
    ensureProcessingMethodExists(processingMethodCode, record.verwerkingsmethodeOmschrijving)

    // Determine collection type
    val collectionType = determineCollectionType(record)

    // Build pickup location
    val pickupLocation = buildPickupLocation(record)

    // Create waste stream
    try {
      val wasteType = WasteType(
        name = record.gebruikelijkeNaam ?: record.euralcodeOmschrijving ?: "Onbekend",
        euralCode = EuralCode(euralCode),
        processingMethod = ProcessingMethod(processingMethodCode)
      )

      val wasteStream = WasteStream(
        wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
        wasteType = wasteType,
        collectionType = collectionType,
        pickupLocation = pickupLocation,
        deliveryLocation = WasteDeliveryLocation(ProcessorPartyId(processorPartyId)),
        consignorParty = Consignor.Company(pickupCompany.companyId),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
        pickupParty = pickupCompany.companyId,
        status = WasteStreamStatus.ACTIVE
      )

      wasteStreams.save(wasteStream)
      logger.debug("Successfully imported waste stream: $wasteStreamNumber")
      return ProcessResult.Success

    } catch (e: Exception) {
      logger.warn("Failed to create waste stream ${record.afvalstroomnummer}: ${e.message}")
      return createError(record, importBatchId, LmaImportErrorCode.VALIDATION_ERROR,
        "Kon afvalstroom niet aanmaken: ${e.message}")
    }
  }

  private fun createError(
    record: LmaCsvRecord,
    importBatchId: UUID,
    errorCode: LmaImportErrorCode,
    message: String
  ): ProcessResult.Error {
    val error = LmaImportError(
      importBatchId = importBatchId,
      rowNumber = record.rowNumber,
      wasteStreamNumber = record.afvalstroomnummer,
      errorCode = errorCode,
      errorMessage = message,
      rawData = record.toRawDataMap()
    )
    return ProcessResult.Error(error)
  }

  /**
   * Formats eural code from LMA format (170405) to our format (17 04 05).
   * Handles asterisk suffix (170405* -> 17 04 05*)
   */
  private fun formatEuralCode(code: String): String {
    val cleanCode = code.replace(" ", "")
    val hasAsterisk = cleanCode.endsWith("*")
    val digits = if (hasAsterisk) cleanCode.dropLast(1) else cleanCode

    if (digits.length != 6 || !digits.all { it.isDigit() }) {
      throw IllegalArgumentException("Invalid eural code format: $code")
    }

    val formatted = "${digits.substring(0, 2)} ${digits.substring(2, 4)} ${digits.substring(4, 6)}"
    return if (hasAsterisk) "$formatted*" else formatted
  }

  /**
   * Formats processing method from LMA format (A01) to our format (A.01).
   */
  private fun formatProcessingMethod(code: String): String {
    val cleanCode = code.replace(".", "").trim()
    if (cleanCode.length != 3) {
      throw IllegalArgumentException("Invalid processing method format: $code")
    }
    return "${cleanCode[0]}.${cleanCode.substring(1)}"
  }

  /**
   * Determines collection type based on CSV flags.
   */
  private fun determineCollectionType(record: LmaCsvRecord): WasteCollectionType {
    return when {
      record.routeinzameling?.uppercase() == "J" -> WasteCollectionType.ROUTE
      record.inzamelaarsregeling?.uppercase() == "J" -> WasteCollectionType.COLLECTORS_SCHEME
      else -> WasteCollectionType.DEFAULT
    }
  }

  /**
   * Builds pickup location from CSV record.
   * Uses ProximityDescription if nabijheidsbeschrijving is present, otherwise DutchAddress.
   */
  private fun buildPickupLocation(record: LmaCsvRecord): nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location {
    // If there's a proximity description, use that
    if (!record.locatieNabijheid.isNullOrBlank()) {
      return nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.ProximityDescription(
        description = record.locatieNabijheid,
        postalCodeDigits = extractPostalCodeDigits(record.locatiePostcode),
        city = nl.eazysoftware.eazyrecyclingservice.domain.model.address.City(record.locatiePlaats ?: ""),
        country = record.locatieLand ?: "Nederland"
      )
    }

    // If there's an address, use DutchAddress
    if (!record.locatieStraatnaam.isNullOrBlank()) {
      val address = nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address(
        streetName = nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName(record.locatieStraatnaam),
        buildingNumber = record.locatieHuisnummer ?: "",
        buildingNumberAddition = record.locatieHuisnummerToevoeging,
        postalCode = nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode(record.locatiePostcode ?: ""),
        city = nl.eazysoftware.eazyrecyclingservice.domain.model.address.City(record.locatiePlaats ?: ""),
        country = record.locatieLand ?: "Nederland"
      )
      return nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.DutchAddress(address)
    }

    // No location specified
    return nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.NoLocation
  }

  /**
   * Extracts 4-digit postal code from full postal code (e.g., "2691HA" -> "2691").
   */
  private fun extractPostalCodeDigits(postalCode: String?): String {
    if (postalCode.isNullOrBlank()) return ""
    val digits = postalCode.replace(" ", "").take(4)
    return if (digits.all { it.isDigit() }) digits else ""
  }

  /**
   * Ensures the Eural code exists in the database. Creates it if missing.
   */
  private fun ensureEuralCodeExists(code: String, description: String?) {
    if (!euralRepository.existsById(code)) {
      val eural = Eural(
        code = code,
        description = description ?: "Geïmporteerd via LMA - $code"
      )
      euralRepository.save(eural)
      logger.info("Created missing Eural code: $code")
    }
  }

  /**
   * Ensures the processing method exists in the database. Creates it if missing.
   */
  private fun ensureProcessingMethodExists(code: String, description: String?) {
    if (!processingMethodRepository.existsById(code)) {
      val processingMethod = ProcessingMethodDto(
        code = code,
        description = description ?: "Geïmporteerd via LMA - $code"
      )
      processingMethodRepository.save(processingMethod)
      logger.info("Created missing processing method: $code")
    }
  }
}
