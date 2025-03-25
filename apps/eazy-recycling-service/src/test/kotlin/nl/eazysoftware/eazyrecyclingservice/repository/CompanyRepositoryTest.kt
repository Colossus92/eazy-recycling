package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.TestContainerBaseTest
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.AfterTest
import kotlin.test.Test

private const val CHAMBER_OF_COMMERCE_ID = "87068888"
private const val VIHB_ID = "VXXB324324"

class CompanyRepositoryTest(
    @Autowired
    private val companyRepository: CompanyRepository
): TestContainerBaseTest() {

    @AfterTest
    fun cleanUp() {
        companyRepository.deleteAll()
    }

    @Test
    fun `Company can be saved and retrieved by Chamber of Commerce ID`() {
        val company = CompanyDto(
            chamberOfCommerceId = CHAMBER_OF_COMMERCE_ID,
            name = "Eazy Software",
            address = AddressDto(
                streetName = "Abe Bonnemastraat 58",
                buildingNumber = "58",
                city = "Bergschenhoek",
                postalCode = "2662EJ",
                country = "Nederland",
            )
        )

        val savedCompany = companyRepository.save(company)
        val retrievedCompany = companyRepository.findByChamberOfCommerceIdAndVihbId(CHAMBER_OF_COMMERCE_ID, null)

        assertThat(retrievedCompany)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(savedCompany)
    }

    @Test
    fun `Company can be saved and retrieved by VIHB ID`() {
        val company = CompanyDto(
            vihbId = VIHB_ID,
            name = "Eazy Software",
            address = AddressDto(
                streetName = "Abe Bonnemastraat 58",
                buildingNumber = "58",
                city = "Bergschenhoek",
                postalCode = "2662EJ",
                country = "Nederland",
            )
        )

        val savedCompany = companyRepository.save(company)
        val retrievedCompany = companyRepository.findByChamberOfCommerceIdAndVihbId(null, VIHB_ID)

        assertThat(retrievedCompany)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(savedCompany)
    }

    @Test
    fun `Company can be saved and retrieved by both Chamber of Commerce and VIHB ID`() {
        val company = CompanyDto(
            chamberOfCommerceId = CHAMBER_OF_COMMERCE_ID,
            vihbId = VIHB_ID,
            name = "Eazy Software",
            address = AddressDto(
                streetName = "Abe Bonnemastraat 58",
                buildingNumber = "58",
                city = "Bergschenhoek",
                postalCode = "2662EJ",
                country = "Nederland",
            )
        )

        val savedCompany = companyRepository.save(company)
        val retrievedCompany = companyRepository.findByChamberOfCommerceIdAndVihbId(CHAMBER_OF_COMMERCE_ID, VIHB_ID)

        assertThat(retrievedCompany)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(savedCompany)
    }

    @Test
    fun `When both ID's are specified you can't retrieve by one ID`() {
        val company = CompanyDto(
            chamberOfCommerceId = CHAMBER_OF_COMMERCE_ID,
            vihbId = VIHB_ID,
            name = "Eazy Software",
            address = AddressDto(
                streetName = "Abe Bonnemastraat 58",
                buildingNumber = "58",
                city = "Bergschenhoek",
                postalCode = "2662EJ",
                country = "Nederland",
            )
        )

        val savedCompany = companyRepository.save(company)
        val retrievedCompany = companyRepository.findByChamberOfCommerceIdAndVihbId(CHAMBER_OF_COMMERCE_ID, null)

        assertThat(savedCompany).isNotNull
        assertThat(retrievedCompany).isNull()
    }
}