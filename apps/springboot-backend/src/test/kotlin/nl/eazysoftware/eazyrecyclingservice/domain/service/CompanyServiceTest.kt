package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController
import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import java.sql.SQLException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class CompanyServiceTest {

    @Mock
    private lateinit var companyRepository: CompanyRepository

    @Mock
    private lateinit var companyBranchRepository: ProjectLocations

    private lateinit var companyService: CompanyService

    @BeforeEach
    fun setUp() {
        companyService = CompanyService(companyRepository, companyBranchRepository)
    }

    @Test
    fun `create company - duplicate chamberOfCommerceId throws DuplicateKeyException`() {
        // Given
        val request = CompanyController.CompanyRequest(
            chamberOfCommerceId = "12345678",
            vihbId = "VIHB123",
            name = "Test Company",
            address = AddressRequest(
                streetName = "Main St",
                buildingNumber = "1",
                postalCode = "1234AB",
                city = "Amsterdam"
            )
        )

        val constraintViolationException = ConstraintViolationException(
            "Unique constraint violation",
            SQLException("Unique constraint violation"),
            "companies_chamber_of_commerce_id_key"
        )
        val dataIntegrityViolationException = DataIntegrityViolationException(
            "Data integrity violation",
            constraintViolationException
        )

        whenever(companyRepository.save(any<CompanyDto>())).thenThrow(dataIntegrityViolationException)

        // When & Then
        val exception = assertFailsWith<DuplicateKeyException> {
            companyService.create(request)
        }
        assertEquals("Kvk nummer of VIHB nummer al in gebruik.", exception.message)
    }

    @Test
    fun `create company - duplicate vihbId throws DuplicateKeyException`() {
        // Given
        val request = CompanyController.CompanyRequest(
            chamberOfCommerceId = "12345678",
            vihbId = "VIHB123",
            name = "Test Company",
            address = AddressRequest(
                streetName = "Main St",
                buildingNumber = "1",
                postalCode = "1234AB",
                city = "Amsterdam"
            )
        )

        val constraintViolationException = ConstraintViolationException(
            "Unique constraint violation",
            SQLException("Unique constraint violation"),
            "companies_vihb_number_key"
        )
        val dataIntegrityViolationException = DataIntegrityViolationException(
            "Data integrity violation",
            constraintViolationException
        )

        whenever(companyRepository.save(any<CompanyDto>())).thenThrow(dataIntegrityViolationException)

        // When & Then
        val exception = assertFailsWith<DuplicateKeyException> {
            companyService.create(request)
        }
        assertEquals("Kvk nummer of VIHB nummer al in gebruik.", exception.message)
    }

    @Test
    fun `create company - successful creation returns CompanyDto`() {
        // Given
        val request = CompanyController.CompanyRequest(
            chamberOfCommerceId = "12345678",
            vihbId = "VIHB123",
            name = "Test Company",
            address = AddressRequest(
                streetName = "Main St",
                buildingNumber = "1",
                postalCode = "1234AB",
                city = "Amsterdam"
            )
        )

        val expectedCompanyDto = CompanyDto(
            id = UUID.randomUUID(),
            name = "Test Company",
            chamberOfCommerceId = "12345678",
            vihbId = "VIHB123",
            address = AddressDto(
                streetName = "Main St",
                buildingNumber = "1",
                postalCode = "1234AB",
                city = "Amsterdam",
                buildingName = null,
                country = null
            )
        )

        whenever(companyRepository.save(any<CompanyDto>())).thenReturn(expectedCompanyDto)

        // When
        val result = companyService.create(request)

        // Then
        assertEquals(expectedCompanyDto, result)
    }

    @Test
    fun `update company - duplicate chamberOfCommerceId throws DuplicateKeyException`() {
        // Given
        val companyId = UUID.randomUUID().toString()
        val existingCompany = CompanyDto(
            id = UUID.fromString(companyId),
            name = "Existing Company",
            chamberOfCommerceId = "87654321",
            vihbId = "VIHB456",
            address = AddressDto(
                streetName = "Old St",
                buildingNumber = "2",
                postalCode = "5678CD",
                city = "Rotterdam",
                buildingName = null,
                country = null
            )
        )

        val updatedCompany = existingCompany.copy(chamberOfCommerceId = "12345678") // Duplicate ID

        val constraintViolationException = ConstraintViolationException(
            "Unique constraint violation",
            SQLException("Unique constraint violation"),
            "companies_chamber_of_commerce_id_key"
        )
        val dataIntegrityViolationException = DataIntegrityViolationException(
            "Data integrity violation",
            constraintViolationException
        )

        whenever(companyRepository.findById(UUID.fromString(companyId))).thenReturn(Optional.of(existingCompany))
        whenever(companyRepository.save(updatedCompany)).thenThrow(dataIntegrityViolationException)

        // When & Then
        val exception = assertFailsWith<DuplicateKeyException> {
            companyService.update(companyId, updatedCompany)
        }
        assertEquals("Kvk nummer of VIHB nummer al in gebruik.", exception.message)
    }

    @Test
    fun `update company - duplicate vihbId throws DuplicateKeyException`() {
        // Given
        val companyId = UUID.randomUUID().toString()
        val existingCompany = CompanyDto(
            id = UUID.fromString(companyId),
            name = "Existing Company",
            chamberOfCommerceId = "87654321",
            vihbId = "VIHB456",
            address = AddressDto(
                streetName = "Old St",
                buildingNumber = "2",
                postalCode = "5678CD",
                city = "Rotterdam",
                buildingName = null,
                country = null
            )
        )

        val updatedCompany = existingCompany.copy(vihbId = "VIHB123") // Duplicate VIHB ID

        val constraintViolationException = ConstraintViolationException(
            "Unique constraint violation",
            SQLException("Unique constraint violation"),
            "companies_vihb_number_key"
        )
        val dataIntegrityViolationException = DataIntegrityViolationException(
            "Data integrity violation",
            constraintViolationException
        )

        whenever(companyRepository.findById(UUID.fromString(companyId))).thenReturn(Optional.of(existingCompany))
        whenever(companyRepository.save(updatedCompany)).thenThrow(dataIntegrityViolationException)

        // When & Then
        val exception = assertFailsWith<DuplicateKeyException> {
            companyService.update(companyId, updatedCompany)
        }
        assertEquals("Kvk nummer of VIHB nummer al in gebruik.", exception.message)
    }

    @Test
    fun `update company - successful update returns CompanyDto`() {
        // Given
        val companyId = UUID.randomUUID().toString()
        val existingCompany = CompanyDto(
            id = UUID.fromString(companyId),
            name = "Existing Company",
            chamberOfCommerceId = "87654321",
            vihbId = "VIHB456",
            address = AddressDto(
                streetName = "Old St",
                buildingNumber = "2",
                postalCode = "5678CD",
                city = "Rotterdam",
                buildingName = null,
                country = null
            )
        )

        val updatedCompany = existingCompany.copy(name = "Updated Company")

        whenever(companyRepository.findById(UUID.fromString(companyId))).thenReturn(Optional.of(existingCompany))
        whenever(companyRepository.save(updatedCompany)).thenReturn(updatedCompany)

        // When
        val result = companyService.update(companyId, updatedCompany)

        // Then
        assertEquals(updatedCompany, result)
    }

    @Test
    fun `create company - other DataIntegrityViolationException is rethrown`() {
        // Given
        val request = CompanyController.CompanyRequest(
            chamberOfCommerceId = "12345678",
            vihbId = "VIHB123",
            name = "Test Company",
            address = AddressRequest(
                streetName = "Main St",
                buildingNumber = "1",
                postalCode = "1234AB",
                city = "Amsterdam"
            )
        )

        val constraintViolationException = ConstraintViolationException(
            "Some other constraint violation",
            SQLException("Some other constraint violation"),
            "some_other_constraint"
        )
        val dataIntegrityViolationException = DataIntegrityViolationException(
            "Data integrity violation",
            constraintViolationException
        )

        whenever(companyRepository.save(any<CompanyDto>())).thenThrow(dataIntegrityViolationException)

        // When & Then
        assertFailsWith<DataIntegrityViolationException> {
            companyService.create(request)
        }
    }
}
