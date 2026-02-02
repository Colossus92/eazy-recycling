package nl.eazysoftware.eazyrecyclingservice.application.usecase.lmaimport

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyProjectLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProjectLocationId
import nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportError
import nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportErrorCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.lmaimport.LmaImportResult
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.*
import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.Instant
import java.util.*

/**
 * Service for importing waste streams from LMA CSV export files.
 */
@Service
class ImportLmaWasteStreamsService(
  private val csvParser: LmaCsvParser,
  private val wasteStreams: WasteStreams,
  private val companies: Companies,
  private val fuzzyMatcher: FuzzyMatcher,
  private val catalogItemMapper: WasteStreamCatalogItemMapper,
  private val catalogItems: CatalogItems,
  private val lmaImportErrors: LmaImportErrors,
  private val euralRepository: EuralRepository,
  private val processingMethodRepository: ProcessingMethodRepository,
  private val lmaDeclarations: LmaDeclarations,
  private val idGenerator: ReceivalDeclarationIdGenerator,
  private val projectLocations: ProjectLocations
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

      if (record.locatieLand?.isNetherlands() != true) {
        logger.debug("Skipping row ${record.rowNumber}: foreign address not supported")
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

    // Use smart matching to handle potential duplicate KvK numbers
    val candidates = companies.findAllByChamberOfCommerceId(kvkNumber)
    val pickupCompany = fuzzyMatcher.findBestMatchingCompany(candidates, record.naamOntdoener)
      ?: return createError(record, importBatchId, LmaImportErrorCode.COMPANY_NOT_FOUND,
        "Geen bedrijf gevonden met KvK nummer: $kvkNumber (${record.naamOntdoener ?: "onbekend"})")

    // Parse and validate eural code
    val euralCode = try {
      formatEuralCode(record.euralcode
        ?: return createError(record, importBatchId, LmaImportErrorCode.MISSING_REQUIRED_FIELD, "Euralcode is verplicht"))
    } catch (@Suppress("unused") e: Exception) {
      return createError(record, importBatchId, LmaImportErrorCode.INVALID_EURAL_CODE,
        "Ongeldige euralcode: ${record.euralcode}")
    }

    // Ensure eural code exists in database, create if missing
    ensureEuralCodeExists(euralCode, record.euralcodeOmschrijving)

    // Parse and validate processing method
    val processingMethodCode = try {
      formatProcessingMethod(record.verwerkingsmethodeCode
        ?: return createError(record, importBatchId, LmaImportErrorCode.MISSING_REQUIRED_FIELD, "VerwerkingsMethode Code is verplicht"))
    } catch (@Suppress("unused") e: Exception) {
      return createError(record, importBatchId, LmaImportErrorCode.INVALID_PROCESSING_METHOD,
        "Ongeldige verwerkingsmethode: ${record.verwerkingsmethodeCode}")
    }

    // Ensure processing method exists in database, create if missing
    ensureProcessingMethodExists(processingMethodCode, record.verwerkingsmethodeOmschrijving)

    try {

      // Determine collection type
      val collectionType = determineCollectionType(record)

      // Build pickup location
      val pickupLocation = buildPickupLocation(record, pickupCompany)

      // Resolve catalog item using hard-coded mappings
      val wasteStreamName = record.gebruikelijkeNaam ?: record.euralcodeOmschrijving ?: "Onbekend"
      val catalogItemId = resolveCatalogItemId(wasteStreamName)

      val wasteType = WasteType(
        name = wasteStreamName,
        euralCode = EuralCode(euralCode),
        processingMethod = ProcessingMethod(processingMethodCode)
      )

      // Create waste stream
      val wasteStream = WasteStream(
        wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
        wasteType = wasteType,
        collectionType = collectionType,
        pickupLocation = pickupLocation,
        deliveryLocation = WasteDeliveryLocation(ProcessorPartyId(processorPartyId)),
        consignorParty = Consignor.Company(pickupCompany.companyId),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
        pickupParty = pickupCompany.companyId,
        catalogItemId = catalogItemId,
        status = WasteStreamStatus.ACTIVE
      )

      wasteStreams.save(wasteStream)
      lmaDeclarations.save(
        LmaDeclarationDto(
          wasteStreamNumber = wasteStream.wasteStreamNumber.number,
          period = "012024",
          status = LmaDeclarationDto.Status.COMPLETED,
          totalWeight = 0,
          id = idGenerator.nextId(),
          amiceUUID = UUID.randomUUID(),
          transporters = emptyList(),
          totalShipments = 0,
          createdAt = Instant.now(),
          type = LmaDeclaration.Type.LEGACY,
          errors = emptyList(),
        )
      )
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
   * Uses ProximityDescription if nabijheidsbeschrijving is present,
   * creates/finds ProjectLocation for addresses different from company address,
   * otherwise uses Company location.
   */
  private fun buildPickupLocation(record: LmaCsvRecord, pickupCompany: Company): Location {
    // If there's a proximity description, use that
    if (!record.locatieNabijheid.isNullOrBlank()) {
      return Location.ProximityDescription(
        description = record.locatieNabijheid,
        postalCodeDigits = extractPostalCodeDigits(record.locatiePostcode),
        city = City(sanitizeStreetOrCity(record.locatiePlaats)),
        country = record.locatieLand ?: "Nederland"
      )
    }

    // If there's an address, check if it matches company address or create project location
    if (!record.locatieStraatnaam.isNullOrBlank()) {
      val address = Address(
        streetName = StreetName(sanitizeStreetOrCity(record.locatieStraatnaam)),
        buildingNumber = record.locatieHuisnummer ?: "",
        buildingNumberAddition = record.locatieHuisnummerToevoeging,
        postalCode = DutchPostalCode(sanitizePostalCode(record.locatiePostcode)),
        city = City(sanitizeStreetOrCity(record.locatiePlaats)),
        country = record.locatieLand ?: "Nederland"
      )

      // Check if address matches company address
      if (addressMatchesCompany(address, pickupCompany)) {
        return Location.Company(
          companyId = pickupCompany.companyId,
          name = pickupCompany.name,
          address = pickupCompany.address
        )
      }

      // Create or find project location
      return getOrCreateProjectLocation(pickupCompany, address)
    }

    // No location specified
    return Location.NoLocation
  }

  /**
   * Checks if the given address matches the company's address (postal code and building number).
   */
  private fun addressMatchesCompany(address: Address, company: Company): Boolean {
    return address.postalCode.value == company.address.postalCode.value &&
           address.buildingNumber == company.address.buildingNumber
  }

  /**
   * Gets existing project location or creates a new one if it doesn't exist.
   */
  private fun getOrCreateProjectLocation(company: Company, address: Address): Location.ProjectLocationSnapshot {
    // Try to find existing project location
    val existingLocation = projectLocations.findByCompanyIdAndPostalCodeAndBuildingNumber(
      companyId = company.companyId,
      postalCode = address.postalCode,
      buildingNumber = address.buildingNumber
    )

    if (existingLocation != null) {
      return existingLocation.toSnapshot()
    }

    // Create new project location if not found
    val projectLocation = CompanyProjectLocation(
      id = ProjectLocationId(UUID.randomUUID()),
      companyId = company.companyId,
      address = address
    )
    projectLocations.create(projectLocation)
    logger.info("Created project location for company ${company.name} at ${address.toAddressLine()}")
    return projectLocation.toSnapshot()
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
   * Sanitizes street name or city: first letter uppercase, rest lowercase.
   * Example: "HOOFDSTRAAT" -> "Hoofdstraat"
   */
  private fun sanitizeStreetOrCity(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val trimmed = value.trim()
    return trimmed.lowercase().replaceFirstChar { it.uppercase() }
  }

  /**
   * Sanitizes postal code to format "1234 AB".
   * Example: "2691HA" -> "2691 HA", "2691 ha" -> "2691 HA"
   */
  private fun sanitizePostalCode(postalCode: String?): String {
    if (postalCode.isNullOrBlank()) return ""
    val cleaned = postalCode.replace(" ", "").trim()
    if (cleaned.length != 6) return postalCode?.trim() ?: ""
    
    val digits = cleaned.substring(0, 4)
    val letters = cleaned.substring(4, 6).uppercase()
    return "$digits $letters"
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

  /**
   * Resolves catalog item ID based on waste stream name using hard-coded mappings.
   * Returns null if no mapping exists.
   */
  private fun resolveCatalogItemId(wasteStreamName: String): UUID? {
    val catalogItemName = catalogItemMapper.mapToCatalogItemName(wasteStreamName)
      ?: return null

    // Find catalog item by name (case-insensitive)
    val allCatalogItems = catalogItems.findAll(consignorPartyId = null, query = null)
    val matchingItem = allCatalogItems.firstOrNull {
      it.name.equals(catalogItemName, ignoreCase = true)
    }

    if (matchingItem != null) {
      logger.debug(
        "Resolved catalog item '{}' (ID: {}) for waste stream '{}'",
        matchingItem.name,
        matchingItem.id,
        wasteStreamName
      )
      return matchingItem.id
    } else {
      logger.warn("Catalog item '$catalogItemName' not found for waste stream '$wasteStreamName'")
      return null
    }
  }
}
