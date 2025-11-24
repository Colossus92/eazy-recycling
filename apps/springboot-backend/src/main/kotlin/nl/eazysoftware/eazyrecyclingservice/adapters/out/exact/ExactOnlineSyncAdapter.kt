package nl.eazysoftware.eazyrecyclingservice.adapters.out.exact

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.config.exact.ExactApiClient
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ExactOnlineSync
import nl.eazysoftware.eazyrecyclingservice.domain.service.ExactOAuthService
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncDto
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncRepository
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncStatus
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
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
}
