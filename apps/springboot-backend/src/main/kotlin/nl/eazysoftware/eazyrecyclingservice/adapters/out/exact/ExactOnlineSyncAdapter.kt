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
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.SyncFromExactResult
import nl.eazysoftware.eazyrecyclingservice.domain.service.ExactOAuthService
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncDto
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncRepository
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncCursorDto
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncCursorRepository
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncStatus
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

    // Create account in Exact Online
    val createResponse = createExactAccount(company)

    // Fetch full account details including Code
    val accountDetails = fetchAccountDetails(createResponse.d.__metadata.uri)

    // Save sync record with OK status
    val syncDto = CompanySyncDto(
      companyId = company.companyId.uuid,
      externalId = accountDetails.d.Code,
      syncStatus = SyncStatus.OK,
      syncedFromSourceAt = Instant.now()
    )
    companySyncRepository.save(syncDto)

    logger.info("Successfully synced company ${company.companyId.uuid} to Exact Online with code ${accountDetails.d.Code}")
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

    // Update account in Exact Online
    updateExactAccount(company)

    // Update sync record with OK status
    val existingSync = companySyncRepository.findByCompanyId(company.companyId.uuid)
    if (existingSync != null) {
      val updatedSync = existingSync.copy(
        syncStatus = SyncStatus.OK,
        syncedFromSourceAt = Instant.now(),
        syncErrorMessage = null,
        updatedAt = Instant.now()
      )
      companySyncRepository.save(updatedSync)
    } else {
      // No existing sync record - this shouldn't happen for updates, but handle gracefully
      logger.warn("No existing sync record found for company ${company.companyId.uuid} during update")
    }

    logger.info("Successfully updated company ${company.companyId.uuid} in Exact Online")
  }

  /**
   * Create account in Exact Online
   */
  private fun createExactAccount(company: Company): ExactAccountCreateResponse {
    val restTemplate = exactApiClient.getRestTemplate()
    val url = "https://start.exactonline.nl/api/v1/$EXACT_DIVISION/crm/Accounts"
    val customerStatus = if (company.isCustomer) "C" else "A"

    val accountRequest = ExactAccountRequest(
      ID = company.companyId.uuid,
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
   * Update account in Exact Online
   */
  private fun updateExactAccount(company: Company) {
    val restTemplate = exactApiClient.getRestTemplate()
    val url = "https://start.exactonline.nl/api/v1/$EXACT_DIVISION/crm/Accounts(guid'${company.companyId.uuid}')"
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
    val cursor = syncCursorRepository.findByEntity(SYNC_ENTITY_ACCOUNTS)
    val lastTimestamp = cursor?.lastTimestamp ?: 0L

    var totalSynced = 0
    var totalCreated = 0
    var totalUpdated = 0
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
          val (created, updated) = processExactAccount(account)
          if (created) totalCreated++
          if (updated) totalUpdated++
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
      lastTimestamp = currentTimestamp,
      updatedAt = Instant.now()
    )
    syncCursorRepository.save(newCursor)

    logger.info("Sync from Exact Online completed: $totalSynced records synced ($totalCreated created, $totalUpdated updated)")

    return SyncFromExactResult(
      recordsSynced = totalSynced,
      recordsCreated = totalCreated,
      recordsUpdated = totalUpdated,
      newTimestamp = currentTimestamp
    )
  }

  /**
   * Fetch accounts from Exact Online Sync API
   */
  private fun fetchAccountsFromExact(timestamp: Long): ExactSyncAccountsResponse {
    val restTemplate = exactApiClient.getRestTemplate()
    val url = "https://start.exactonline.nl/api/v1/$EXACT_DIVISION/sync/CRM/Accounts?\$select=ID,Name,Code,AddressLine1,Postcode,City,Country,Email,Phone,ChamberOfCommerce&\$filter=Timestamp gt $timestamp"

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
   * Returns a pair of (created, updated) booleans.
   * 
   * Matching strategy:
   * 1. First try to match by chamber of commerce ID
   * 2. Then try to match by external ID (Code) in companies_sync table
   * 3. If no match found, create a new company
   */
  private fun processExactAccount(account: ExactSyncAccount): Pair<Boolean, Boolean> {
    // Parse address from AddressLine1 (format: "Street Number Addition")
    val (streetName, buildingNumber, buildingNumberAddition) = parseAddressLine(account.AddressLine1)

    // Try to find existing company by chamber of commerce ID first
    val existingCompany = account.ChamberOfCommerce?.let { kvk ->
      companies.findByChamberOfCommerceId(kvk)
    } ?: run {
      // If no match by KvK, try to find by external ID (Code) in sync table
      account.Code?.let { code ->
        companySyncRepository.findByExternalId(code)?.let { syncRecord ->
          companies.findById(CompanyId(syncRecord.companyId))
        }
      }
    }

    return if (existingCompany == null) {
      // Create new company with a new UUID (not the Exact ID)
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

      // Create sync record linking our company ID to the Exact Code
      val syncDto = CompanySyncDto(
        companyId = newCompanyId.uuid,
        externalId = account.Code,
        syncStatus = SyncStatus.OK,
        syncedFromSourceAt = Instant.now()
      )
      companySyncRepository.save(syncDto)

      logger.debug("Created company ${company.name} (${newCompanyId.uuid}) from Exact Code ${account.Code}")
      Pair(true, false)
    } else {
      // Update existing company - overwrite fields from Exact
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

      // Update or create sync record
      val existingSync = companySyncRepository.findByCompanyId(existingCompany.companyId.uuid)
      if (existingSync != null) {
        val updatedSync = existingSync.copy(
          externalId = account.Code,
          syncStatus = SyncStatus.OK,
          syncedFromSourceAt = Instant.now(),
          updatedAt = Instant.now()
        )
        companySyncRepository.save(updatedSync)
      } else {
        // Create sync record if it doesn't exist
        val syncDto = CompanySyncDto(
          companyId = existingCompany.companyId.uuid,
          externalId = account.Code,
          syncStatus = SyncStatus.OK,
          syncedFromSourceAt = Instant.now()
        )
        companySyncRepository.save(syncDto)
      }

      logger.debug("Updated company ${updatedCompany.name} (${existingCompany.companyId.uuid}) from Exact Code ${account.Code}")
      Pair(false, true)
    }
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
  }

  // Data classes for Exact Online API

  data class ExactAccountRequest(
    @get:JsonProperty("ID")
    val ID: UUID,
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
}
