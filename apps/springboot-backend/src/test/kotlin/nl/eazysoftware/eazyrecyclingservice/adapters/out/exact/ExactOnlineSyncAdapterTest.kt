package nl.eazysoftware.eazyrecyclingservice.adapters.out.exact

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.config.exact.ExactApiClient
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.City
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.service.ExactOAuthService
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncDto
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncRepository
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncCursorRepository
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class ExactOnlineSyncAdapterTest {

  @Mock
  private lateinit var exactApiClient: ExactApiClient

  @Mock(lenient = true)
  private lateinit var companySyncRepository: CompanySyncRepository

  @Mock
  private lateinit var syncCursorRepository: SyncCursorRepository

  @Mock
  private lateinit var companies: Companies

  @Mock
  private lateinit var exactOAuthService: ExactOAuthService

  @Mock
  private lateinit var restTemplate: RestTemplate

  private lateinit var adapter: ExactOnlineSyncAdapter
  private val objectMapper = ObjectMapper()

  private val companyId = UUID.randomUUID()
  private val exactCode = "12345"
  private val metadataUri = "https://start.exactonline.nl/api/v1/4002380/crm/Accounts(guid'$companyId')"

  @BeforeEach
  fun setUp() {
    val conflictHandler = ExactConflictHandler(companySyncRepository)
    adapter = ExactOnlineSyncAdapter(
      exactApiClient = exactApiClient,
      companySyncRepository = companySyncRepository,
      syncCursorRepository = syncCursorRepository,
      companies = companies,
      exactOAuthService = exactOAuthService,
      objectMapper = objectMapper,
      accountProcessor = ExactAccountProcessor(
        companySyncRepository = companySyncRepository,
        companies = companies,
        conflictHandler = conflictHandler
      ),
      conflictHandler = conflictHandler
    )
  }

  @Nested
  inner class SyncCompanyTests {

    @Test
    fun `syncCompany should throw ExactSyncException when no valid tokens`() {
      // Given
      val company = createTestCompany()
      whenever(exactOAuthService.hasValidTokens()).thenReturn(false)

      // When & Then
      val exception = assertThrows<ExactSyncException> {
        adapter.syncCompany(company)
      }
      assertEquals("No valid tokens", exception.message)

      verify(exactApiClient, never()).getRestTemplate()
      verify(companySyncRepository, never()).save(any())
    }

    @Test
    fun `syncCompany should create account and save sync record on success`() {
      // Given
      val company = createTestCompany()
      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)

      val createResponse = ExactOnlineSyncAdapter.ExactAccountCreateResponse(
        d = ExactOnlineSyncAdapter.ExactAccountData(
          __metadata = ExactOnlineSyncAdapter.ExactMetadata(
            uri = metadataUri,
            type = "Exact.Web.Api.Models.CRM.Account"
          )
        )
      )
      whenever(restTemplate.postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )).thenReturn(ResponseEntity.ok(createResponse))

      val detailsResponse = ExactOnlineSyncAdapter.ExactAccountDetailsResponse(
        d = ExactOnlineSyncAdapter.ExactAccountDetails(
          Code = exactCode,
          Name = company.name
        )
      )
      whenever(restTemplate.exchange(
        eq(metadataUri),
        eq(HttpMethod.GET),
        isNull(),
        eq(ExactOnlineSyncAdapter.ExactAccountDetailsResponse::class.java)
      )).thenReturn(ResponseEntity.ok(detailsResponse))

      // When
      adapter.syncCompany(company)

      // Then
      val syncCaptor = argumentCaptor<CompanySyncDto>()
      verify(companySyncRepository).save(syncCaptor.capture())

      val savedSync = syncCaptor.firstValue
      assertEquals(companyId, savedSync.companyId)
      assertEquals(exactCode, savedSync.externalId)
      assertEquals(SyncStatus.OK, savedSync.syncStatus)
      assertNull(savedSync.syncErrorMessage)
    }

    @Test
    fun `syncCompany should throw exception when POST returns empty body`() {
      // Given
      val company = createTestCompany()
      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)

      whenever(restTemplate.postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )).thenReturn(ResponseEntity.ok(null))

      // When & Then
      val exception = assertThrows<IllegalStateException> {
        adapter.syncCompany(company)
      }
      assertEquals("Empty response from Exact Online", exception.message)

      verify(companySyncRepository, never()).save(any())
    }

    @Test
    fun `syncCompany should throw exception when GET details returns empty body`() {
      // Given
      val company = createTestCompany()
      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)

      val createResponse = ExactOnlineSyncAdapter.ExactAccountCreateResponse(
        d = ExactOnlineSyncAdapter.ExactAccountData(
          __metadata = ExactOnlineSyncAdapter.ExactMetadata(
            uri = metadataUri,
            type = "Exact.Web.Api.Models.CRM.Account"
          )
        )
      )
      whenever(restTemplate.postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )).thenReturn(ResponseEntity.ok(createResponse))

      whenever(restTemplate.exchange(
        eq(metadataUri),
        eq(HttpMethod.GET),
        isNull(),
        eq(ExactOnlineSyncAdapter.ExactAccountDetailsResponse::class.java)
      )).thenReturn(ResponseEntity.ok(null))

      // When & Then
      val exception = assertThrows<IllegalStateException> {
        adapter.syncCompany(company)
      }
      assertEquals("Empty response when fetching account details", exception.message)

      verify(companySyncRepository, never()).save(any())
    }

    @Test
    fun `syncCompany should use correct customer status C when company is customer`() {
      // Given
      val company = createTestCompany(isCustomer = true)
      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)

      val createResponse = ExactOnlineSyncAdapter.ExactAccountCreateResponse(
        d = ExactOnlineSyncAdapter.ExactAccountData(
          __metadata = ExactOnlineSyncAdapter.ExactMetadata(uri = metadataUri, type = "")
        )
      )
      whenever(restTemplate.postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )).thenReturn(ResponseEntity.ok(createResponse))

      val detailsResponse = ExactOnlineSyncAdapter.ExactAccountDetailsResponse(
        d = ExactOnlineSyncAdapter.ExactAccountDetails(Code = exactCode, Name = company.name)
      )
      whenever(restTemplate.exchange(
        any<String>(),
        eq(HttpMethod.GET),
        isNull(),
        eq(ExactOnlineSyncAdapter.ExactAccountDetailsResponse::class.java)
      )).thenReturn(ResponseEntity.ok(detailsResponse))

      // When
      adapter.syncCompany(company)

      // Then
      val requestCaptor = argumentCaptor<org.springframework.http.HttpEntity<*>>()
      verify(restTemplate).postForEntity(
        any<String>(),
        requestCaptor.capture(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )

      val requestBody = requestCaptor.firstValue.body as ExactOnlineSyncAdapter.ExactAccountCreateRequest
      assertEquals("C", requestBody.Status)
    }

    @Test
    fun `syncCompany should use correct customer status A when company is not customer`() {
      // Given
      val company = createTestCompany(isCustomer = false)
      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)

      val createResponse = ExactOnlineSyncAdapter.ExactAccountCreateResponse(
        d = ExactOnlineSyncAdapter.ExactAccountData(
          __metadata = ExactOnlineSyncAdapter.ExactMetadata(uri = metadataUri, type = "")
        )
      )
      whenever(restTemplate.postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )).thenReturn(ResponseEntity.ok(createResponse))

      val detailsResponse = ExactOnlineSyncAdapter.ExactAccountDetailsResponse(
        d = ExactOnlineSyncAdapter.ExactAccountDetails(Code = exactCode, Name = company.name)
      )
      whenever(restTemplate.exchange(
        any<String>(),
        eq(HttpMethod.GET),
        isNull(),
        eq(ExactOnlineSyncAdapter.ExactAccountDetailsResponse::class.java)
      )).thenReturn(ResponseEntity.ok(detailsResponse))

      // When
      adapter.syncCompany(company)

      // Then
      val requestCaptor = argumentCaptor<org.springframework.http.HttpEntity<*>>()
      verify(restTemplate).postForEntity(
        any<String>(),
        requestCaptor.capture(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )

      val requestBody = requestCaptor.firstValue.body as ExactOnlineSyncAdapter.ExactAccountCreateRequest
      assertEquals("A", requestBody.Status)
    }
  }

  @Nested
  inner class UpdateCompanyTests {

    @Test
    fun `updateCompany should throw ExactSyncException when no valid tokens`() {
      // Given
      val company = createTestCompany()
      whenever(exactOAuthService.hasValidTokens()).thenReturn(false)

      // When & Then
      val exception = assertThrows<ExactSyncException> {
        adapter.updateCompany(company)
      }
      assertEquals("No valid tokens", exception.message)

      verify(exactApiClient, never()).getRestTemplate()
      verify(companySyncRepository, never()).save(any())
    }

    @Test
    fun `updateCompany should create in Exact when no existing sync found`() {
      // Given
      val company = createTestCompany()

      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(companySyncRepository.findByCompanyId(companyId)).thenReturn(null)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)

      val createResponse = ExactOnlineSyncAdapter.ExactAccountCreateResponse(
        d = ExactOnlineSyncAdapter.ExactAccountData(
          __metadata = ExactOnlineSyncAdapter.ExactMetadata(
            uri = metadataUri,
            type = "Exact.Web.Api.Models.CRM.Account"
          )
        )
      )
      whenever(restTemplate.postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )).thenReturn(ResponseEntity.ok(createResponse))

      val detailsResponse = ExactOnlineSyncAdapter.ExactAccountDetailsResponse(
        d = ExactOnlineSyncAdapter.ExactAccountDetails(Code = exactCode, Name = company.name)
      )
      whenever(restTemplate.exchange(
        eq(metadataUri),
        eq(HttpMethod.GET),
        isNull(),
        eq(ExactOnlineSyncAdapter.ExactAccountDetailsResponse::class.java)
      )).thenReturn(ResponseEntity.ok(detailsResponse))

      // When
      adapter.updateCompany(company)

      // Then - should create via POST, not PUT
      verify(restTemplate).postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )
      verify(restTemplate, never()).exchange(
        any<String>(),
        eq(HttpMethod.PUT),
        any(),
        eq(Void::class.java)
      )
      verify(companySyncRepository).save(any())
    }

    @Test
    fun `updateCompany should create in Exact when sync record has no exactGuid`() {
      // Given
      val company = createTestCompany()
      val existingSync = CompanySyncDto(
        companyId = companyId,
        externalId = exactCode,
        exactGuid = null, // No exactGuid
        syncStatus = SyncStatus.OK,
        syncedFromSourceAt = Instant.now()
      )

      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(companySyncRepository.findByCompanyId(companyId)).thenReturn(existingSync)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)

      val createResponse = ExactOnlineSyncAdapter.ExactAccountCreateResponse(
        d = ExactOnlineSyncAdapter.ExactAccountData(
          __metadata = ExactOnlineSyncAdapter.ExactMetadata(
            uri = metadataUri,
            type = "Exact.Web.Api.Models.CRM.Account"
          )
        )
      )
      whenever(restTemplate.postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )).thenReturn(ResponseEntity.ok(createResponse))

      val detailsResponse = ExactOnlineSyncAdapter.ExactAccountDetailsResponse(
        d = ExactOnlineSyncAdapter.ExactAccountDetails(Code = exactCode, Name = company.name)
      )
      whenever(restTemplate.exchange(
        eq(metadataUri),
        eq(HttpMethod.GET),
        isNull(),
        eq(ExactOnlineSyncAdapter.ExactAccountDetailsResponse::class.java)
      )).thenReturn(ResponseEntity.ok(detailsResponse))

      // When
      adapter.updateCompany(company)

      // Then - should create via POST, not PUT
      verify(restTemplate).postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )
      verify(restTemplate, never()).exchange(
        any<String>(),
        eq(HttpMethod.PUT),
        any(),
        eq(Void::class.java)
      )
      verify(companySyncRepository).save(any())
    }

    @Test
    fun `updateCompany should send PUT request to correct URL using exactGuid`() {
      // Given
      val company = createTestCompany()
      val exactGuid = UUID.randomUUID()
      val existingSync = CompanySyncDto(
        companyId = companyId,
        externalId = exactCode,
        exactGuid = exactGuid,
        syncStatus = SyncStatus.OK,
        syncedFromSourceAt = Instant.now()
      )

      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)
      whenever(companySyncRepository.findByCompanyId(companyId)).thenReturn(existingSync)

      val expectedUrl = "https://start.exactonline.nl/api/v1/4002380/crm/Accounts(guid'$exactGuid')"
      whenever(restTemplate.exchange(
        eq(expectedUrl),
        eq(HttpMethod.PUT),
        any(),
        eq(Void::class.java)
      )).thenReturn(ResponseEntity<Void>(HttpStatus.OK))

      // When
      adapter.updateCompany(company)

      // Then
      val urlCaptor = argumentCaptor<String>()
      verify(restTemplate).exchange(
        urlCaptor.capture(),
        eq(HttpMethod.PUT),
        any(),
        eq(Void::class.java)
      )

      assertEquals(expectedUrl, urlCaptor.firstValue)
    }

    @Test
    fun `updateCompany should use correct customer status C when company is customer`() {
      // Given
      val company = createTestCompany(isCustomer = true)
      val exactGuid = UUID.randomUUID()
      val existingSync = CompanySyncDto(
        companyId = companyId,
        externalId = exactCode,
        exactGuid = exactGuid,
        syncStatus = SyncStatus.OK,
        syncedFromSourceAt = Instant.now()
      )

      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)
      whenever(companySyncRepository.findByCompanyId(companyId)).thenReturn(existingSync)

      whenever(restTemplate.exchange(
        any<String>(),
        eq(HttpMethod.PUT),
        any(),
        eq(Void::class.java)
      )).thenReturn(ResponseEntity<Void>(HttpStatus.OK))

      // When
      adapter.updateCompany(company)

      // Then
      val requestCaptor = argumentCaptor<org.springframework.http.HttpEntity<*>>()
      verify(restTemplate).exchange(
        any<String>(),
        eq(HttpMethod.PUT),
        requestCaptor.capture(),
        eq(Void::class.java)
      )

      val requestBody = requestCaptor.firstValue.body as ExactOnlineSyncAdapter.ExactAccountUpdateRequest
      assertEquals("C", requestBody.Status)
    }

    @Test
    fun `updateCompany should use correct customer status A when company is not customer`() {
      // Given
      val company = createTestCompany(isCustomer = false)
      val exactGuid = UUID.randomUUID()
      val existingSync = CompanySyncDto(
        companyId = companyId,
        externalId = exactCode,
        exactGuid = exactGuid,
        syncStatus = SyncStatus.OK,
        syncedFromSourceAt = Instant.now()
      )

      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)
      whenever(companySyncRepository.findByCompanyId(companyId)).thenReturn(existingSync)

      whenever(restTemplate.exchange(
        any<String>(),
        eq(HttpMethod.PUT),
        any(),
        eq(Void::class.java)
      )).thenReturn(ResponseEntity<Void>(HttpStatus.OK))

      // When
      adapter.updateCompany(company)

      // Then
      val requestCaptor = argumentCaptor<org.springframework.http.HttpEntity<*>>()
      verify(restTemplate).exchange(
        any<String>(),
        eq(HttpMethod.PUT),
        requestCaptor.capture(),
        eq(Void::class.java)
      )

      val requestBody = requestCaptor.firstValue.body as ExactOnlineSyncAdapter.ExactAccountUpdateRequest
      assertEquals("A", requestBody.Status)
    }
  }

  @Nested
  inner class AddressFormattingTests {

    @Test
    fun `syncCompany should format address correctly with building number addition`() {
      // Given
      val company = createTestCompany(buildingNumberAddition = "A")
      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)

      val createResponse = ExactOnlineSyncAdapter.ExactAccountCreateResponse(
        d = ExactOnlineSyncAdapter.ExactAccountData(
          __metadata = ExactOnlineSyncAdapter.ExactMetadata(uri = metadataUri, type = "")
        )
      )
      whenever(restTemplate.postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )).thenReturn(ResponseEntity.ok(createResponse))

      val detailsResponse = ExactOnlineSyncAdapter.ExactAccountDetailsResponse(
        d = ExactOnlineSyncAdapter.ExactAccountDetails(Code = exactCode, Name = company.name)
      )
      whenever(restTemplate.exchange(
        any<String>(),
        eq(HttpMethod.GET),
        isNull(),
        eq(ExactOnlineSyncAdapter.ExactAccountDetailsResponse::class.java)
      )).thenReturn(ResponseEntity.ok(detailsResponse))

      // When
      adapter.syncCompany(company)

      // Then
      val requestCaptor = argumentCaptor<org.springframework.http.HttpEntity<*>>()
      verify(restTemplate).postForEntity(
        any<String>(),
        requestCaptor.capture(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )

      val requestBody = requestCaptor.firstValue.body as ExactOnlineSyncAdapter.ExactAccountCreateRequest
      assertEquals("Teststraat 123A", requestBody.AddressLine1)
    }

    @Test
    fun `syncCompany should format address correctly without building number addition`() {
      // Given
      val company = createTestCompany(buildingNumberAddition = null)
      whenever(exactOAuthService.hasValidTokens()).thenReturn(true)
      whenever(exactApiClient.getRestTemplate()).thenReturn(restTemplate)

      val createResponse = ExactOnlineSyncAdapter.ExactAccountCreateResponse(
        d = ExactOnlineSyncAdapter.ExactAccountData(
          __metadata = ExactOnlineSyncAdapter.ExactMetadata(uri = metadataUri, type = "")
        )
      )
      whenever(restTemplate.postForEntity(
        any<String>(),
        any(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )).thenReturn(ResponseEntity.ok(createResponse))

      val detailsResponse = ExactOnlineSyncAdapter.ExactAccountDetailsResponse(
        d = ExactOnlineSyncAdapter.ExactAccountDetails(Code = exactCode, Name = company.name)
      )
      whenever(restTemplate.exchange(
        any<String>(),
        eq(HttpMethod.GET),
        isNull(),
        eq(ExactOnlineSyncAdapter.ExactAccountDetailsResponse::class.java)
      )).thenReturn(ResponseEntity.ok(detailsResponse))

      // When
      adapter.syncCompany(company)

      // Then
      val requestCaptor = argumentCaptor<org.springframework.http.HttpEntity<*>>()
      verify(restTemplate).postForEntity(
        any<String>(),
        requestCaptor.capture(),
        eq(ExactOnlineSyncAdapter.ExactAccountCreateResponse::class.java)
      )

      val requestBody = requestCaptor.firstValue.body as ExactOnlineSyncAdapter.ExactAccountCreateRequest
      assertEquals("Teststraat 123", requestBody.AddressLine1)
    }
  }

  // Helper methods

  private fun createTestCompany(
    isCustomer: Boolean = true,
    isSupplier: Boolean = true,
    buildingNumberAddition: String? = null
  ) = Company(
    companyId = CompanyId(companyId),
    name = "Test Company BV",
    chamberOfCommerceId = "12345678",
    vihbNumber = null,
    processorId = null,
    address = Address(
      streetName = StreetName("Teststraat"),
      buildingNumber = "123",
      buildingNumberAddition = buildingNumberAddition,
      postalCode = DutchPostalCode("1234AB"),
      city = City("Amsterdam")
    ),
    roles = emptyList(),
    phone = "+31612345678",
    email = "test@company.nl",
    isSupplier = isSupplier,
    isCustomer = isCustomer
  )
}
