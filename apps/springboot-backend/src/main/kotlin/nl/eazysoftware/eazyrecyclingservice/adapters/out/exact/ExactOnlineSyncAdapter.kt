package nl.eazysoftware.eazyrecyclingservice.adapters.out.exact

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.config.exact.ExactApiClient
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.City
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ExactOnlineSync
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.SyncDeletedResult
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
      externalId = accountDetails.d.Code,
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
      Phone = company.phone,
      Email = company.email,
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
      Phone = company.phone,
      Email = company.email,
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

      if (accounts.isEmpty()) {
        hasMoreRecords = false
        continue
      }

      logger.info("Fetched ${accounts.size} accounts from Exact Online (timestamp > $currentTimestamp)")

      for (account in accounts) {
        try {
          val result = processExactAccount(account)
          when (result) {
            is ProcessResult.Created -> totalCreated++
            is ProcessResult.Updated -> totalUpdated++
            is ProcessResult.Conflict -> totalConflicted++
            is ProcessResult.PendingReview -> totalPendingReview++
          }
          totalSynced++
        } catch (e: Exception) {
          logger.error("Failed to process account ${account.ID}: ${e.message}", e)
          // Continue with other accounts
        }
      }

      // Update timestamp to the highest value in this batch
      val maxTimestamp = accounts.maxOfOrNull { it.Timestamp } ?: currentTimestamp
      currentTimestamp = maxTimestamp

      // If we got less than 1000 records, we've reached the end
      hasMoreRecords = accounts.size >= 1000
    }

    // Save the new cursor
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

    logger.info("Sync from Exact Online completed: $totalSynced records synced ($totalCreated created, $totalUpdated updated, $totalConflicted conflicts, $totalPendingReview pending review)")

    return SyncFromExactResult(
      recordsSynced = totalSynced,
      recordsCreated = totalCreated,
      recordsUpdated = totalUpdated,
      recordsConflicted = totalConflicted,
      recordsPendingReview = totalPendingReview,
      newTimestamp = currentTimestamp
    )
  }

  /**
   * Get all sync records that have conflicts requiring manual resolution.
   */
  override fun getConflicts(): List<CompanySyncDto> {
    return companySyncRepository.findAllBySyncStatus(SyncStatus.CONFLICT)
  }

  /**
   * Get all sync records that are pending manual review.
   */
  override fun getPendingReviews(): List<CompanySyncDto> {
    return companySyncRepository.findAllByRequiresManualReviewTrue()
  }

  /**
   * Sync deleted records from Exact Online.
   * Uses the Exact Online Deleted API with timestamp-based pagination.
   * Soft-deletes companies locally that were deleted in Exact.
   */
  @Transactional
  override fun syncDeletedFromExact(): SyncDeletedResult {
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
        hasMoreRecords = false
        continue
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
          // Continue with other records
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
      recordsNotFound = totalNotFound,
      newTimestamp = currentTimestamp
    )
  }

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

    // Find the company
    val company = companies.findById(CompanyId(syncRecord.companyId))
    if (company == null) {
      logger.warn("Company ${syncRecord.companyId} not found for deleted Exact account ${deleted.EntityKey}")
      return DeleteResult.NotFound
    }

    // Soft-delete the company
    companies.deleteById(company.companyId)

    // Update sync record with deletion info
    val updatedSync = syncRecord.copy(
      syncStatus = SyncStatus.DELETED,
      deletedInExactAt = deleted.DeletedDate?.let { Instant.parse(it) },
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
      "https://start.exactonline.nl/api/v1/$EXACT_DIVISION/sync/Deleted?\$filter=Timestamp gt $timestamp&\$select=Timestamp,DeletedBy,DeletedDate,Division,EntityKey,EntityType,ID"

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
   * Result of processing a single Exact account
   */
  private sealed class ProcessResult {
    object Created : ProcessResult()
    object Updated : ProcessResult()
    object Conflict : ProcessResult()
    object PendingReview : ProcessResult()
  }

  /**
   * Fetch accounts from Exact Online Sync API
   */
  private fun fetchAccountsFromExact(timestamp: Long): ExactSyncAccountsResponse {
    val restTemplate = exactApiClient.getRestTemplate()
    val url =
      "https://start.exactonline.nl/api/v1/$EXACT_DIVISION/sync/CRM/Accounts?\$select=ID,Name,Code,AddressLine1,Postcode,City,Country,Email,Phone,ChamberOfCommerce&\$filter=Timestamp gt $timestamp"

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
   * Process a single account from Exact Online.
   * Returns a ProcessResult indicating what happened.
   *
   * Matching strategy (in priority order):
   * 1. exact_guid     → Direct link (highest confidence)
   * 2. external_id    → Exact Code link
   * 3. chamber_of_commerce_id → Business key match (KVK)
   * 4. postal_code + building_number + building_number_addition → Address match (requires manual review)
   * 5. No match       → Create new company
   *
   * Conflict detection:
   * - If KVK match found but company is not linked to this Exact account → CONFLICT
   * - If address match found → PENDING_REVIEW (fuzzy match needs confirmation)
   */
  private fun processExactAccount(account: ExactSyncAccount): ProcessResult {
    // Parse address from AddressLine1 (format: "Street Number Addition")
    val (streetName, buildingNumber, buildingNumberAddition) = parseAddressLine(account.AddressLine1)

    // Step 1: Try to find by exact_guid (direct link - highest confidence)
    val syncByGuid = companySyncRepository.findByExactGuid(account.ID)
    if (syncByGuid != null) {
      val existingCompany = companies.findById(CompanyId(syncByGuid.companyId))
      if (existingCompany != null) {
        return updateExistingCompany(
          existingCompany,
          account,
          syncByGuid,
          streetName,
          buildingNumber,
          buildingNumberAddition
        )
      }
    }

    // Step 2: Try to find by external_id (Exact Code)
    val syncByCode = account.Code?.let { companySyncRepository.findByExternalId(it) }
    if (syncByCode != null) {
      val existingCompany = companies.findById(CompanyId(syncByCode.companyId))
      if (existingCompany != null) {
        // Update the sync record with the exact_guid if not already set
        val updatedSync = if (syncByCode.exactGuid == null) {
          syncByCode.copy(exactGuid = account.ID, updatedAt = Instant.now())
        } else syncByCode
        return updateExistingCompany(
          existingCompany,
          account,
          updatedSync,
          streetName,
          buildingNumber,
          buildingNumberAddition
        )
      }
    }

    // Step 3: Try to find by chamber of commerce ID (KVK)
    val companyByKvk = account.ChamberOfCommerce?.let { companies.findByChamberOfCommerceId(it) }
    if (companyByKvk != null) {
      // Check if this company is already linked to a DIFFERENT Exact account
      val existingSync = companySyncRepository.findByCompanyId(companyByKvk.companyId.uuid)
      if (existingSync != null && existingSync.exactGuid != null && existingSync.exactGuid != account.ID) {
        // CONFLICT: Company with this KVK is already linked to a different Exact account
        logger.warn("CONFLICT: Company ${companyByKvk.name} (${companyByKvk.companyId.uuid}) with KVK ${account.ChamberOfCommerce} is already linked to Exact GUID ${existingSync.exactGuid}, but received update from Exact GUID ${account.ID}")

        // Create a conflict sync record for the new Exact account
        val conflictSync = CompanySyncDto(
          companyId = companyByKvk.companyId.uuid,
          externalId = account.Code,
          exactGuid = account.ID,
          syncStatus = SyncStatus.CONFLICT,
          syncedFromSourceAt = Instant.now(),
          conflictDetails = mapOf(
            "conflictType" to "KVK_COLLISION",
            "field" to "chamber_of_commerce_id",
            "value" to account.ChamberOfCommerce,
            "existingExactGuid" to existingSync.exactGuid.toString(),
            "newExactGuid" to account.ID.toString(),
            "existingCompanyId" to companyByKvk.companyId.uuid.toString()
          ),
          requiresManualReview = true
        )
        companySyncRepository.save(conflictSync)
        return ProcessResult.Conflict
      }

      // No conflict - link and update
      val syncRecord = existingSync ?: CompanySyncDto(
        companyId = companyByKvk.companyId.uuid,
        syncStatus = SyncStatus.OK,
        syncedFromSourceAt = Instant.now()
      )
      val updatedSync = syncRecord.copy(
        externalId = account.Code,
        exactGuid = account.ID,
        syncStatus = SyncStatus.OK,
        syncedFromSourceAt = Instant.now(),
        updatedAt = Instant.now()
      )
      return updateExistingCompany(
        companyByKvk,
        account,
        updatedSync,
        streetName,
        buildingNumber,
        buildingNumberAddition
      )
    }

    // Step 4: Try to find by exact address match (postal_code + building_number + building_number_addition)
    val postalCode = account.Postcode
    if (!postalCode.isNullOrBlank() && buildingNumber.isNotBlank()) {
      val companyByAddress = companies.findByAddress(postalCode, buildingNumber, buildingNumberAddition)
      if (companyByAddress != null) {
        // Check if this company is already linked to a DIFFERENT Exact account
        val existingSync = companySyncRepository.findByCompanyId(companyByAddress.companyId.uuid)
        if (existingSync != null && existingSync.exactGuid != null && existingSync.exactGuid != account.ID) {
          // CONFLICT: Company at this address is already linked to a different Exact account
          logger.warn("CONFLICT: Company ${companyByAddress.name} (${companyByAddress.companyId.uuid}) at address $postalCode $buildingNumber$buildingNumberAddition is already linked to Exact GUID ${existingSync.exactGuid}, but received update from Exact GUID ${account.ID}")

          val conflictSync = CompanySyncDto(
            companyId = companyByAddress.companyId.uuid,
            externalId = account.Code,
            exactGuid = account.ID,
            syncStatus = SyncStatus.CONFLICT,
            syncedFromSourceAt = Instant.now(),
            conflictDetails = mapOf(
              "conflictType" to "ADDRESS_COLLISION",
              "postalCode" to postalCode,
              "buildingNumber" to buildingNumber,
              "buildingNumberAddition" to (buildingNumberAddition ?: ""),
              "existingExactGuid" to existingSync.exactGuid.toString(),
              "newExactGuid" to account.ID.toString(),
              "existingCompanyId" to companyByAddress.companyId.uuid.toString()
            ),
            requiresManualReview = true
          )
          companySyncRepository.save(conflictSync)
          return ProcessResult.Conflict
        }

        // Address match found but not linked yet - mark for manual review
        logger.info("PENDING_REVIEW: Found potential address match for Exact account ${account.ID} (${account.Name}) with company ${companyByAddress.name} (${companyByAddress.companyId.uuid}) at $postalCode $buildingNumber$buildingNumberAddition")

        val pendingSync = CompanySyncDto(
          companyId = companyByAddress.companyId.uuid,
          externalId = account.Code,
          exactGuid = account.ID,
          syncStatus = SyncStatus.PENDING_REVIEW,
          syncedFromSourceAt = Instant.now(),
          conflictDetails = mapOf(
            "matchType" to "ADDRESS_MATCH",
            "postalCode" to postalCode,
            "buildingNumber" to buildingNumber,
            "buildingNumberAddition" to (buildingNumberAddition ?: ""),
            "exactAccountName" to account.Name,
            "localCompanyName" to companyByAddress.name
          ),
          requiresManualReview = true
        )
        companySyncRepository.save(pendingSync)
        return ProcessResult.PendingReview
      }
    }

    // Step 5: No match found - create new company
    return createNewCompany(account, streetName, buildingNumber, buildingNumberAddition)
  }

  /**
   * Update an existing company with data from Exact Online.
   */
  private fun updateExistingCompany(
    existingCompany: Company,
    account: ExactSyncAccount,
    syncRecord: CompanySyncDto,
    streetName: String,
    buildingNumber: String,
    buildingNumberAddition: String?
  ): ProcessResult {
    val updatedCompany = existingCompany.copy(
      name = account.Name,
      chamberOfCommerceId = account.ChamberOfCommerce,
      address = Address(
        streetName = StreetName(streetName),
        buildingNumber = buildingNumber,
        buildingNumberAddition = buildingNumberAddition,
        postalCode = DutchPostalCode(account.Postcode ?: "0000AA"),
        city = City(account.City ?: ""),
        country = account.Country ?: "NL"
      ),
      phone = account.Phone,
      email = account.Email,
      isSupplier = account.IsSupplier ?: false,
      isCustomer = account.Status == "C"
    )
    companies.update(updatedCompany)

    // Update sync record
    val updatedSync = syncRecord.copy(
      externalId = account.Code,
      exactGuid = account.ID,
      syncStatus = SyncStatus.OK,
      syncedFromSourceAt = Instant.now(),
      syncErrorMessage = null,
      conflictDetails = null,
      requiresManualReview = false,
      updatedAt = Instant.now()
    )
    companySyncRepository.save(updatedSync)

    logger.debug(
      "Updated company {} ({}) from Exact GUID {}",
      updatedCompany.name,
      existingCompany.companyId.uuid,
      account.ID
    )
    return ProcessResult.Updated
  }

  /**
   * Create a new company from Exact Online data.
   */
  private fun createNewCompany(
    account: ExactSyncAccount,
    streetName: String,
    buildingNumber: String,
    buildingNumberAddition: String?
  ): ProcessResult {
    val newCompanyId = CompanyId(UUID.randomUUID())
    val company = Company(
      companyId = newCompanyId,
      name = account.Name,
      chamberOfCommerceId = account.ChamberOfCommerce,
      vihbNumber = null, // Not available from Exact
      processorId = null, // Not available from Exact
      address = Address(
        streetName = StreetName(streetName),
        buildingNumber = buildingNumber,
        buildingNumberAddition = buildingNumberAddition,
        postalCode = DutchPostalCode(account.Postcode ?: "0000AA"),
        city = City(account.City ?: ""),
        country = account.Country ?: "NL"
      ),
      roles = emptyList(), // Roles are managed locally
      phone = account.Phone,
      email = account.Email,
      isSupplier = account.IsSupplier ?: false,
      isCustomer = account.Status == "C"
    )

    companies.create(company)

    // Create sync record linking our company ID to the Exact GUID and Code
    val syncDto = CompanySyncDto(
      companyId = newCompanyId.uuid,
      externalId = account.Code,
      exactGuid = account.ID,
      syncStatus = SyncStatus.OK,
      syncedFromSourceAt = Instant.now()
    )
    companySyncRepository.save(syncDto)

    logger.debug("Created company {} ({}) from Exact GUID {}", company.name, newCompanyId.uuid, account.ID)
    return ProcessResult.Created
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
