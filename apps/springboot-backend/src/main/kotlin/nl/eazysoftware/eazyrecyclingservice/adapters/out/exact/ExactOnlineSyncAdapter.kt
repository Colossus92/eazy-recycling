package nl.eazysoftware.eazyrecyclingservice.adapters.out.exact

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.config.exact.ExactApiClient
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ExactOnlineSync
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.SyncFromExactResult
import nl.eazysoftware.eazyrecyclingservice.domain.service.ExactOAuthService
import nl.eazysoftware.eazyrecyclingservice.repository.exact.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Exception thrown when Exact Online sync fails
 */
class ExactSyncException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)


/**
 * Adapter for syncing companies to Exact Online
 */
@Component
class ExactOnlineSyncAdapter(
  private val exactApiClient: ExactApiClient,
  private val companySyncRepository: CompanySyncRepository,
  private val syncCursorRepository: SyncCursorRepository,
  private val companies: Companies,
  private val exactOAuthService: ExactOAuthService,
  private val objectMapper: ObjectMapper,
  private val accountProcessor: ExactAccountProcessor,
  private val conflictHandler: ExactConflictHandler,
) : ExactOnlineSync {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val EXACT_DIVISION = "4002380" // Division ID from the API endpoint

  /**
   * Sync a newly created company to Exact Online
   * Runs in the same transaction as company creation.
   * Caller must handle exceptions to prevent rollback of company creation.
   */
  override fun syncCompany(company: Company) {
    // Skip sync if no valid tokens
    if (!exactOAuthService.hasValidTokens()) {
      logger.warn("Skipping Exact Online sync for company ${company.companyId.uuid} - no valid tokens")
      throw ExactSyncException("No valid tokens")
    }

    logger.info("Syncing company ${company.companyId.uuid} to Exact Online")

    // Create account in Exact Online (without specifying ID - let Exact generate it)
    val createResponse = createExactAccount(company)

    // Extract the Exact GUID from the metadata URI
    val exactGuid = extractGuidFromMetadataUri(createResponse.d.__metadata.uri)

    // Fetch full account details including Code
    val accountDetails = fetchAccountDetails(createResponse.d.__metadata.uri)

    // Save sync record with OK status, storing both Exact GUID and Code
    val syncDto = CompanySyncDto(
      companyId = company.companyId.uuid,
      exactGuid = exactGuid,
      syncStatus = SyncStatus.OK,
      syncedFromSourceAt = Instant.now()
    )
    companySyncRepository.save(syncDto)

    logger.info("Successfully synced company ${company.companyId.uuid} to Exact Online with GUID $exactGuid and code ${accountDetails.d.Code}")
  }

  /**
   * Extract the GUID from Exact Online metadata URI.
   * URI format: https://start.exactonline.nl/api/v1/{division}/crm/Accounts(guid'{guid}')
   */
  private fun extractGuidFromMetadataUri(uri: String): UUID? {
    val regex = Regex("""guid'([0-9a-fA-F-]+)'""")
    val match = regex.find(uri)
    return match?.groupValues?.get(1)?.let { UUID.fromString(it) }
  }

  /**
   * Update an existing company in Exact Online
   * Caller must handle exceptions to prevent rollback of company update.
   */
  override fun updateCompany(company: Company) {
    // Skip sync if no valid tokens
    if (!exactOAuthService.hasValidTokens()) {
      logger.warn("Skipping Exact Online update for company ${company.companyId.uuid} - no valid tokens")
      throw ExactSyncException("No valid tokens")
    }

    logger.info("Updating company ${company.companyId.uuid} in Exact Online")

    // Find existing sync record to get the Exact GUID
    val existingSync = companySyncRepository.findByCompanyId(company.companyId.uuid)
    val exactGuid = existingSync?.exactGuid
    if (existingSync == null || exactGuid == null) {
      // No sync record or no GUID - create in Exact
      logger.info("No existing Exact link for company ${company.companyId.uuid} - creating in Exact Online")
      syncCompany(company)
      return
    }

    // Update account in Exact Online using the Exact GUID
    updateExactAccount(company, exactGuid)

    // Update sync record with OK status
    val updatedSync = existingSync.copy(
      syncStatus = SyncStatus.OK,
      syncedFromSourceAt = Instant.now(),
      syncErrorMessage = null,
      updatedAt = Instant.now()
    )
    companySyncRepository.save(updatedSync)

    logger.info("Successfully updated company ${company.companyId.uuid} in Exact Online (GUID: $exactGuid)")
  }

  /**
   * Create account in Exact Online.
   */
  private fun createExactAccount(company: Company): ExactAccountCreateResponse {
    val restTemplate = exactApiClient.getRestTemplate()
    val url = "https://start.exactonline.nl/api/v1/$EXACT_DIVISION/crm/Accounts"
    val customerStatus = if (company.isCustomer) "C" else "A"

    // Note: We do NOT send ID - let Exact auto-generate it
    val accountRequest = ExactAccountCreateRequest(
      Name = company.name,
      AddressLine1 = "${company.address.streetName.value} ${company.address.buildingNumber}${company.address.buildingNumberAddition ?: ""}",
      Postcode = company.address.postalCode.value,
      City = company.address.city.value,
      Country = "NL",
      Status = customerStatus,
      ChamberOfCommerce = company.chamberOfCommerceId,
      Phone = company.phone?.value,
      Email = company.email?.value,
      IsSupplier = company.isSupplier,
    )

    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON

    // Log the request payload
    val requestJson = objectMapper.writeValueAsString(accountRequest)
    logger.info("Sending request to Exact Online: POST $url")
    logger.info("Request payload: $requestJson")

    val request = HttpEntity(accountRequest, headers)
    val response = restTemplate.postForEntity(url, request, ExactAccountCreateResponse::class.java)

    logger.info("Received response from Exact Online: ${response.statusCode}")

    return response.body ?: throw IllegalStateException("Empty response from Exact Online")
  }

  /**
   * Update account in Exact Online.
   */
  private fun updateExactAccount(company: Company, exactGuid: UUID) {
    val restTemplate = exactApiClient.getRestTemplate()
    val url = "https://start.exactonline.nl/api/v1/$EXACT_DIVISION/crm/Accounts(guid'$exactGuid')"
    val customerStatus = if (company.isCustomer) "C" else "A"

    val accountRequest = ExactAccountUpdateRequest(
      Name = company.name,
      AddressLine1 = "${company.address.streetName.value} ${company.address.buildingNumber}${company.address.buildingNumberAddition ?: ""}",
      Postcode = company.address.postalCode.value,
      City = company.address.city.value,
      Country = "NL",
      Status = customerStatus,
      ChamberOfCommerce = company.chamberOfCommerceId,
      Phone = company.phone?.value,
      Email = company.email?.value,
      IsSupplier = company.isSupplier,
    )

    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON

    // Log the request payload
    val requestJson = objectMapper.writeValueAsString(accountRequest)
    logger.info("Sending request to Exact Online: PUT $url")
    logger.info("Request payload: $requestJson")

    val request = HttpEntity(accountRequest, headers)
    val response = restTemplate.exchange(url, HttpMethod.PUT, request, Void::class.java)

    logger.info("Received response from Exact Online: ${response.statusCode}")
  }

  /**
   * Fetch full account details including Code field
   */
  private fun fetchAccountDetails(metadataUri: String): ExactAccountDetailsResponse {
    val restTemplate = exactApiClient.getRestTemplate()

    logger.debug("Fetching account details from: $metadataUri")

    val response = restTemplate.exchange(
      metadataUri,
      HttpMethod.GET,
      null,
      ExactAccountDetailsResponse::class.java
    )

    return response.body ?: throw IllegalStateException("Empty response when fetching account details")
  }

  /**
   * Sync companies from Exact Online to our database using the Sync API.
   * Uses timestamp-based pagination to get only new and changed records.
   */
  @Transactional
  override fun syncFromExact(): SyncFromExactResult {
    // Skip sync if no valid tokens
    if (!exactOAuthService.hasValidTokens()) {
      logger.warn("Skipping Exact Online sync - no valid tokens")
      throw ExactSyncException("No valid tokens")
    }

    logger.info("Starting sync from Exact Online")

    // Get the last timestamp from cursor, or start from 1 (first sync)
    val cursor = syncCursorRepository.findByEntityAndCursorType(SYNC_ENTITY_ACCOUNTS, SyncCursorDto.CURSOR_TYPE_SYNC)
    val lastTimestamp = cursor?.lastTimestamp ?: 0L

    var totalSynced = 0
    var totalCreated = 0
    var totalUpdated = 0
    var totalConflicted = 0
    var totalPendingReview = 0
    var currentTimestamp = lastTimestamp
    var hasMoreRecords = true

    // Paginate through all records using timestamp
    while (hasMoreRecords) {
      val response = fetchAccountsFromExact(currentTimestamp)
      val accounts = response.d.results
        .filter { it.Status == "C" || it.IsSupplier == true } // Sync endpoint does not support filtering on fields

      if (accounts.isEmpty()) {
        break  // Exit loop - no need to update timestamp when no records
      }

      logger.info("Fetched ${accounts.size} accounts from Exact Online (timestamp > $currentTimestamp)")

      for (account in accounts) {
        try {
          // Parse address from AddressLine1 (format: "Street Number Addition")
          val (streetName, buildingNumber, buildingNumberAddition) = parseAddressLine(account.AddressLine1)

          // Process in separate transaction to isolate constraint violations
          val result = accountProcessor.processAccount(account, streetName, buildingNumber, buildingNumberAddition)
          when (result) {
            is ProcessResult.Created -> totalCreated++
            is ProcessResult.Updated -> totalUpdated++
            is ProcessResult.Conflict -> totalConflicted++
            is ProcessResult.PendingReview -> totalPendingReview++
          }
          totalSynced++
        } catch (e: IllegalArgumentException) {
          // Domain invariant failure (e.g., missing street name, invalid postal code)
          logger.warn("Domain validation failed for account ${account.ID} (${account.Name}): ${e.message}")
          conflictHandler.createValidationConflict(account, e.message ?: "Validatiefout", "DOMAIN_VALIDATION_ERROR")
          totalConflicted++
          totalSynced++
        } catch (e: Exception) {
          // Other unexpected errors - log but continue with other accounts
          logger.error("Failed to process account ${account.ID}: ${e.message}", e)
        }
      }

      // Update timestamp to the highest value in this batch
      val maxTimestamp = accounts.maxOfOrNull { it.Timestamp } ?: currentTimestamp
      currentTimestamp = maxTimestamp

      // If we got less than 1000 records, we've reached the end
      hasMoreRecords = accounts.size >= 1000
    }

    logger.info("Sync from Exact Online completed: $totalSynced records synced ($totalCreated created, $totalUpdated updated, $totalConflicted conflicts, $totalPendingReview pending review)")

    // Also sync deleted records (before updating cursor to ensure atomicity)
    val deletedResult = syncDeletedFromExact()

    // Save the new cursor only after both syncs completed successfully
    val newCursor = cursor?.copy(
      lastTimestamp = currentTimestamp,
      updatedAt = Instant.now()
    ) ?: SyncCursorDto(
      entity = SYNC_ENTITY_ACCOUNTS,
      cursorType = SyncCursorDto.CURSOR_TYPE_SYNC,
      lastTimestamp = currentTimestamp,
      updatedAt = Instant.now()
    )
    syncCursorRepository.save(newCursor)

    return SyncFromExactResult(
      recordsSynced = totalSynced,
      recordsCreated = totalCreated,
      recordsUpdated = totalUpdated,
      recordsConflicted = totalConflicted,
      recordsPendingReview = totalPendingReview,
      newTimestamp = currentTimestamp,
      deletedRecordsProcessed = deletedResult.recordsProcessed,
      deletedRecordsDeleted = deletedResult.recordsDeleted,
      deletedRecordsNotFound = deletedResult.recordsNotFound
    )
  }

  /**
   * Get all sync records that have conflicts requiring manual resolution.
   */
  override fun getConflicts(): List<CompanySyncDto> {
    return companySyncRepository.findAllBySyncStatus(SyncStatus.CONFLICT)
  }

  /**
   * Parse Microsoft JSON date format: /Date(1234567890123)/
   * Returns null if the format is invalid or the string is null.
   */
  private fun parseMicrosoftJsonDate(dateString: String?): Instant? {
    if (dateString.isNullOrBlank()) return null

    val regex = Regex("""^/Date\((\d+)\)/$""")
    val match = regex.find(dateString)

    return match?.groupValues?.get(1)?.toLongOrNull()?.let { millis ->
      Instant.ofEpochMilli(millis)
    }
  }

  /**
   * Sync deleted records from Exact Online.
   * Uses the Exact Online Deleted API with timestamp-based pagination.
   * Soft-deletes companies locally that were deleted in Exact.
   */
  private fun syncDeletedFromExact(): SyncDeletedResult {
    // Skip sync if no valid tokens
    if (!exactOAuthService.hasValidTokens()) {
      logger.warn("Skipping Exact Online deletion sync - no valid tokens")
      throw ExactSyncException("No valid tokens")
    }

    logger.info("Starting deletion sync from Exact Online")

    // Get the last timestamp from deleted cursor, or start from 1 (first sync)
    val cursor = syncCursorRepository.findByEntityAndCursorType(SYNC_ENTITY_ACCOUNTS, SyncCursorDto.CURSOR_TYPE_DELETED)
    val lastTimestamp = cursor?.lastTimestamp ?: 0L

    var totalProcessed = 0
    var totalDeleted = 0
    var totalNotFound = 0
    var currentTimestamp = lastTimestamp
    var hasMoreRecords = true

    // Paginate through all deleted records using timestamp
    while (hasMoreRecords) {
      val response = fetchDeletedFromExact(currentTimestamp)
      val deletedRecords = response.d.results

      if (deletedRecords.isEmpty()) {
        break  // Exit loop - no need to update timestamp when no records
      }

      logger.info("Fetched ${deletedRecords.size} deleted records from Exact Online (timestamp > $currentTimestamp)")

      for (deleted in deletedRecords) {
        // Only process Account deletions (EntityType = 2)
        if (deleted.EntityType != ENTITY_TYPE_ACCOUNTS) {
          continue
        }

        try {
          val result = processDeletedAccount(deleted)
          when (result) {
            is DeleteResult.Deleted -> totalDeleted++
            is DeleteResult.NotFound -> totalNotFound++
          }
          totalProcessed++
        } catch (e: Exception) {
          logger.error("Failed to process deleted account ${deleted.EntityKey}: ${e.message}", e)
          // Don't update cursor - throw exception to prevent partial sync
          throw ExactSyncException("Failed to process deleted account ${deleted.EntityKey}", e)
        }
      }

      // Update timestamp to the highest value in this batch
      val maxTimestamp = deletedRecords.maxOfOrNull { it.Timestamp } ?: currentTimestamp
      currentTimestamp = maxTimestamp

      // If we got less than 1000 records, we've reached the end
      hasMoreRecords = deletedRecords.size >= 1000
    }

    // Save the new cursor
    val newCursor = cursor?.copy(
      lastTimestamp = currentTimestamp,
      updatedAt = Instant.now()
    ) ?: SyncCursorDto(
      entity = SYNC_ENTITY_ACCOUNTS,
      cursorType = SyncCursorDto.CURSOR_TYPE_DELETED,
      lastTimestamp = currentTimestamp,
      updatedAt = Instant.now()
    )
    syncCursorRepository.save(newCursor)

    logger.info("Deletion sync from Exact Online completed: $totalProcessed records processed ($totalDeleted deleted, $totalNotFound not found)")

    return SyncDeletedResult(
      recordsProcessed = totalProcessed,
      recordsDeleted = totalDeleted,
      recordsNotFound = totalNotFound
    )
  }

  /**
   * Internal result of syncing deleted records
   */
  private data class SyncDeletedResult(
    val recordsProcessed: Int,
    val recordsDeleted: Int,
    val recordsNotFound: Int
  )

  /**
   * Result of processing a deleted record
   */
  private sealed class DeleteResult {
    object Deleted : DeleteResult()
    object NotFound : DeleteResult()
  }

  /**
   * Process a single deleted account from Exact Online.
   */
  private fun processDeletedAccount(deleted: ExactDeletedRecord): DeleteResult {
    // Find the sync record by exact_guid (EntityKey is the account's GUID)
    val syncRecord = companySyncRepository.findByExactGuid(deleted.EntityKey)

    if (syncRecord == null) {
      logger.debug("No sync record found for deleted Exact account {} - may have never been synced", deleted.EntityKey)
      return DeleteResult.NotFound
    }

    // Find the company (companyId can be null for validation conflicts)
    val companyId = syncRecord.companyId
    if (companyId == null) {
      logger.warn("Sync record for Exact account ${deleted.EntityKey} has no company - skipping deletion")
      return DeleteResult.NotFound
    }
    val company = companies.findById(CompanyId(companyId))
    if (company == null) {
      logger.warn("Company $companyId not found for deleted Exact account ${deleted.EntityKey}")
      return DeleteResult.NotFound
    }

    // Soft-delete the company
    companies.deleteById(company.companyId)

    // Update sync record with deletion info
    val updatedSync = syncRecord.copy(
      syncStatus = SyncStatus.DELETED,
      deletedInExactAt = parseMicrosoftJsonDate(deleted.DeletedDate),
      deletedInExactBy = deleted.DeletedBy,
      updatedAt = Instant.now()
    )
    companySyncRepository.save(updatedSync)

    logger.info("Soft-deleted company ${company.name} (${company.companyId.uuid}) - deleted in Exact by ${deleted.DeletedBy} on ${deleted.DeletedDate}")
    return DeleteResult.Deleted
  }

  /**
   * Fetch deleted records from Exact Online Deleted API
   */
  private fun fetchDeletedFromExact(timestamp: Long): ExactDeletedResponse {
    val restTemplate = exactApiClient.getRestTemplate()
    val url =
      "https://start.exactonline.nl/api/v1/$EXACT_DIVISION/sync/Deleted?\$filter=Timestamp gt ${timestamp}L&\$select=Timestamp,DeletedBy,DeletedDate,Division,EntityKey,EntityType,ID"

    logger.debug("Fetching deleted records from Exact Online: $url")

    val response = restTemplate.exchange(
      url,
      HttpMethod.GET,
      null,
      ExactDeletedResponse::class.java
    )

    return response.body ?: throw IllegalStateException("Empty response from Exact Online Deleted API")
  }

  /**
   * Fetch accounts from Exact Online Sync API
   */
  private fun fetchAccountsFromExact(timestamp: Long): ExactSyncAccountsResponse {
    val restTemplate = exactApiClient.getRestTemplate()
    val url =
      "https://start.exactonline.nl/api/v1/$EXACT_DIVISION/sync/CRM/Accounts?\$select=ID,Name,Code,AddressLine1,Postcode,City,Country,Email,Phone,ChamberOfCommerce,Status,IsSupplier&\$filter=Timestamp gt ${timestamp}L"

    logger.debug("Fetching accounts from Exact Online: $url")

    val response = restTemplate.exchange(
      url,
      HttpMethod.GET,
      null,
      ExactSyncAccountsResponse::class.java
    )

    return response.body ?: throw IllegalStateException("Empty response from Exact Online Sync API")
  }

  /**
   * Parse address line into street name, building number, and addition.
   * Format: "Street Name 123A" -> ("Street Name", "123", "A")
   */
  private fun parseAddressLine(addressLine: String?): Triple<String, String, String?> {
    if (addressLine.isNullOrBlank()) {
      return Triple("", "", null)
    }

    // Regex to match: street name, building number, optional addition
    val regex = Regex("""^(.+?)\s+(\d+)([A-Za-z]*)$""")
    val match = regex.find(addressLine.trim())

    return if (match != null) {
      val (street, number, addition) = match.destructured
      Triple(street, number, addition.ifBlank { null })
    } else {
      // If no match, treat the whole thing as street name
      Triple(addressLine, "", null)
    }
  }

  companion object {
    private const val SYNC_ENTITY_ACCOUNTS = "accounts"
    private const val ENTITY_TYPE_ACCOUNTS = 2 // EntityType for Accounts in Exact Deleted API
  }

  // Data classes for Exact Online API

  /**
   * Request body for creating an account in Exact Online.
   * Note: ID is NOT included - we let Exact auto-generate it.
   */
  data class ExactAccountCreateRequest(
    @get:JsonProperty("Name")
    val Name: String,
    @get:JsonProperty("AddressLine1")
    val AddressLine1: String,
    @get:JsonProperty("Postcode")
    val Postcode: String,
    @get:JsonProperty("City")
    val City: String,
    @get:JsonProperty("Country")
    val Country: String,
    @get:JsonProperty("Status")
    val Status: String,
    @get:JsonProperty("ChamberOfCommerce")
    val ChamberOfCommerce: String? = null,
    @get:JsonProperty("Phone")
    val Phone: String? = null,
    @get:JsonProperty("Email")
    val Email: String? = null,
    @get:JsonProperty("IsSupplier")
    val IsSupplier: Boolean,
  )

  data class ExactAccountUpdateRequest(
    @get:JsonProperty("Name")
    val Name: String,
    @get:JsonProperty("AddressLine1")
    val AddressLine1: String,
    @get:JsonProperty("Postcode")
    val Postcode: String,
    @get:JsonProperty("City")
    val City: String,
    @get:JsonProperty("Country")
    val Country: String,
    @get:JsonProperty("Status")
    val Status: String,
    @get:JsonProperty("ChamberOfCommerce")
    val ChamberOfCommerce: String? = null,
    @get:JsonProperty("Phone")
    val Phone: String? = null,
    @get:JsonProperty("Email")
    val Email: String? = null,
    @get:JsonProperty("IsSupplier")
    val IsSupplier: Boolean,
  )

  data class ExactAccountCreateResponse(
    val d: ExactAccountData
  )

  data class ExactAccountData(
    @field:JsonProperty("__metadata")
    val __metadata: ExactMetadata
  )

  data class ExactMetadata(
    val uri: String,
    val type: String
  )

  data class ExactAccountDetailsResponse(
    val d: ExactAccountDetails
  )

  data class ExactAccountDetails(
    val Code: String,
    val Name: String,
    // Add other fields as needed
  )

  // Data classes for Exact Online Sync API

  data class ExactSyncAccountsResponse(
    val d: ExactSyncAccountsData
  )

  data class ExactSyncAccountsData(
    val results: List<ExactSyncAccount>
  )

  data class ExactSyncAccount(
    @field:JsonProperty("ID")
    val ID: UUID,
    @field:JsonProperty("Timestamp")
    val Timestamp: Long,
    @field:JsonProperty("Code")
    val Code: String?,
    @field:JsonProperty("Name")
    val Name: String,
    @field:JsonProperty("AddressLine1")
    val AddressLine1: String?,
    @field:JsonProperty("Postcode")
    val Postcode: String?,
    @field:JsonProperty("City")
    val City: String?,
    @field:JsonProperty("Country")
    val Country: String?,
    @field:JsonProperty("Status")
    val Status: String?,
    @field:JsonProperty("ChamberOfCommerce")
    val ChamberOfCommerce: String?,
    @field:JsonProperty("Phone")
    val Phone: String?,
    @field:JsonProperty("Email")
    val Email: String?,
    @field:JsonProperty("IsSupplier")
    val IsSupplier: Boolean?,
  )

  // Data classes for Exact Online Deleted API

  data class ExactDeletedResponse(
    val d: ExactDeletedData
  )

  data class ExactDeletedData(
    val results: List<ExactDeletedRecord>
  )

  data class ExactDeletedRecord(
    @field:JsonProperty("Timestamp")
    val Timestamp: Long,
    @field:JsonProperty("DeletedBy")
    val DeletedBy: UUID?,
    @field:JsonProperty("DeletedDate")
    val DeletedDate: String?, // ISO 8601 format
    @field:JsonProperty("Division")
    val Division: Int,
    @field:JsonProperty("EntityKey")
    val EntityKey: UUID,
    @field:JsonProperty("EntityType")
    val EntityType: Int,
    @field:JsonProperty("ID")
    val ID: UUID,
  )
}
