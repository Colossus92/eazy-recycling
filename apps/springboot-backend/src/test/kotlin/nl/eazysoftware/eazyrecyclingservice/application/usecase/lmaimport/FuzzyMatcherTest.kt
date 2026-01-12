package nl.eazysoftware.eazyrecyclingservice.application.usecase.lmaimport

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.City
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class FuzzyMatcherTest {

  private lateinit var fuzzyMatcher: FuzzyMatcher

  private val companyId1 = CompanyId(UUID.randomUUID())
  private val companyId2 = CompanyId(UUID.randomUUID())
  private val companyId3 = CompanyId(UUID.randomUUID())

  @BeforeEach
  fun setUp() {
    fuzzyMatcher = FuzzyMatcher()
  }

  @Nested
  inner class FindBestMatchingCompanyTests {

    @Test
    fun `should return null when no candidates provided`() {
      val result = fuzzyMatcher.findBestMatchingCompany(emptyList(), "Test Company")
      assertNull(result)
    }

    @Test
    fun `should return single company when only one candidate`() {
      val company = createCompany(companyId1, "Test Company BV")
      
      val result = fuzzyMatcher.findBestMatchingCompany(listOf(company), "Any Name")
      
      assertEquals(company, result)
    }

    @Test
    fun `should select company with exact name match from multiple candidates`() {
      val company1 = createCompany(companyId1, "Jansen Transport BV")
      val company2 = createCompany(companyId2, "Jansen Logistics BV")
      val company3 = createCompany(companyId3, "Pietersen Transport BV")
      
      val result = fuzzyMatcher.findBestMatchingCompany(
        listOf(company1, company2, company3),
        "Jansen Transport BV"
      )
      
      assertEquals(company1, result)
    }

    @Test
    fun `should select company with closest name using Levenshtein distance`() {
      val company1 = createCompany(companyId1, "Jansen Transport BV") // distance = 1 from "Jansen Transprt BV"
      val company2 = createCompany(companyId2, "Pietersen Transport BV") // distance > 1
      
      val result = fuzzyMatcher.findBestMatchingCompany(
        listOf(company1, company2),
        "Jansen Transprt BV" // typo: missing 'o'
      )
      
      assertEquals(company1, result)
    }

    @Test
    fun `should handle case-insensitive name matching`() {
      val company = createCompany(companyId1, "Jansen Transport BV")
      
      val result = fuzzyMatcher.findBestMatchingCompany(
        listOf(company),
        "JANSEN TRANSPORT BV"
      )
      
      assertEquals(company, result)
    }

    @Test
    fun `should use first company when no company name provided for disambiguation`() {
      val company1 = createCompany(companyId1, "Company A")
      val company2 = createCompany(companyId2, "Company B")
      
      val result = fuzzyMatcher.findBestMatchingCompany(
        listOf(company1, company2),
        null
      )
      
      assertEquals(company1, result)
    }

    @Test
    fun `should use first company when company name is blank`() {
      val company1 = createCompany(companyId1, "Company A")
      val company2 = createCompany(companyId2, "Company B")
      
      val result = fuzzyMatcher.findBestMatchingCompany(
        listOf(company1, company2),
        "   "
      )
      
      assertEquals(company1, result)
    }

    @Test
    fun `should prefer closer match even with multiple similar options`() {
      val company1 = createCompany(companyId1, "ABC Transport") // exact match
      val company2 = createCompany(companyId2, "ABC Transprt")  // distance 1
      val company3 = createCompany(companyId3, "ABC Transpr")   // distance 2
      
      val result = fuzzyMatcher.findBestMatchingCompany(
        listOf(company2, company3, company1), // different order
        "ABC Transport"
      )
      
      assertEquals(company1, result)
    }
  }

  // Helper methods

  private fun createCompany(companyId: CompanyId, name: String): Company {
    return Company(
      companyId = companyId,
      code = "C001",
      name = name,
      address = Address(
        streetName = StreetName("Teststraat"),
        buildingNumber = "1",
        buildingNumberAddition = null,
        postalCode = DutchPostalCode("1234AB"),
        city = City("Amsterdam")
      ),
      chamberOfCommerceId = "12345678",
      vihbNumber = null,
      processorId = ProcessorPartyId("12345"),
      roles = emptyList()
    )
  }
}
