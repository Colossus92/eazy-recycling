package nl.eazysoftware.eazyrecyclingservice.controller.company

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ImportAutoConfiguration(ValidationAutoConfiguration::class)
class CompanyControllerValidationTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setup() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @ParameterizedTest
    @MethodSource("validChamberOfCommerceIds")
    fun `valid champer of commerce id should not have any violations`(validChamberOfCommerceId: String?) {
        val company = CompanyController.CompanyRequest(
            validChamberOfCommerceId,
            "123456VIHB",
            "Company",
            CompanyController.AddressRequest(
                "Main St",
                "",
                "1",
                "1234AB",
                "Amsterdam"
            )
        )
        val violations = validator.validate(company)
        assertThat(violations).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidChamberOfCommerceIds")
    fun `invalid chamber of commerce id should have a violation`(invalidChamberOfCommerceId: String) {
        val company = CompanyController.CompanyRequest(
            invalidChamberOfCommerceId,
            "123456VIHB",
            "Company",
            CompanyController.AddressRequest(
                "Main St",
                "",
                "1",
                "1234AB",
                "Amsterdam"
            )
        )
        val violations = validator.validate(company)
        assertThat(violations).hasSize(1)
        assertThat(violations).extracting(ConstraintViolation<*>::getMessage).containsExactly(
            tuple("KVK nummer moet bestaan uit 8 cijfers, of leeg zijn")
        )
    }

    @ParameterizedTest
    @MethodSource("validVihbIds")
    fun `valid vihb id should not have any violations`(validVihbId: String?) {
        val company = CompanyController.CompanyRequest(
            "12345678",
            validVihbId,
            "Company",
            addressRequest,
        )
        val violations = validator.validate(company)
        assertThat(violations).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidVihbIds")
    fun `invalid vihb id should have a violation`(invalidVihbId: String) {
        val company = CompanyController.CompanyRequest(
            "12345678",
            invalidVihbId,
            "Company",
            addressRequest,
        )
        val violations = validator.validate(company)
        assertThat(violations).hasSize(1)
        assertThat(violations).extracting(ConstraintViolation<*>::getMessage).containsExactly(
            tuple("VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn")
        )
    }

    companion object {

        private val addressRequest = CompanyController.AddressRequest(
            "Main St",
            "",
            "1",
            "1234AB",
            "Amsterdam"
        )

        @JvmStatic
        fun invalidChamberOfCommerceIds() = listOf(
            "1234567",
            "123456789",
            "1234567A",
            "1234-567",
        )

        @JvmStatic
        fun validChamberOfCommerceIds() = listOf(
            "12345678",
            "",
            null,
        )

        @JvmStatic
        fun validVihbIds() = listOf(
            "123456VIHB",
            "999999XXXX",
            "888888VXXB",
            "777777XXXB",
            null,
            ""
        )

        @JvmStatic
        fun invalidVihbIds() = listOf(
            "1234567VIHB",
            "12345VIHB",
            "123456VIHBA",
            "1234-5VIHB",
            "VIHB123456",
            "123456VIHBX",
            "123456VIH",
        )

        
    }
//    @Test
//    fun `create company - invalid vihbId with 5 digits - validation error`() {
//        val req = companyRequest(vihbId = "12345VIHB")
//        securedMockMvc.post(
//            "/companies",
//            objectMapper.writeValueAsString(req)
//        )
//            .andExpect(status().isBadRequest)
//            .andExpect(jsonPath("$.message").value("VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn"))
//    }
//
//    @Test
//    fun `create company - invalid vihbId with 7 digits - validation error`() {
//        val req = companyRequest(vihbId = "1234567VIHB")
//        securedMockMvc.post(
//            "/companies",
//            objectMapper.writeValueAsString(req)
//        )
//            .andExpect(status().isBadRequest)
//            .andExpect(jsonPath("$.message").value("VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn"))
//    }
//
//    @Test
//    fun `create company - invalid vihbId with 3 letters - validation error`() {
//        val req = companyRequest(vihbId = "123456VIH")
//        securedMockMvc.post(
//            "/companies",
//            objectMapper.writeValueAsString(req)
//        )
//            .andExpect(status().isBadRequest)
//            .andExpect(jsonPath("$.message").value("VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn"))
//    }
//
//    @Test
//    fun `create company - invalid vihbId with 5 letters - validation error`() {
//        val req = companyRequest(vihbId = "123456VIHBX")
//        securedMockMvc.post(
//            "/companies",
//            objectMapper.writeValueAsString(req)
//        )
//            .andExpect(status().isBadRequest)
//            .andExpect(jsonPath("$.message").value("VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn"))
//    }
//
//    @Test
//    fun `create company - invalid vihbId with invalid letters - validation error`() {
//        val req = companyRequest(vihbId = "123456ABCD")
//        securedMockMvc.post(
//            "/companies",
//            objectMapper.writeValueAsString(req)
//        )
//            .andExpect(status().isBadRequest)
//            .andExpect(jsonPath("$.message").value("VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn"))
//    }
//
//    @Test
//    fun `create company - invalid vihbId with lowercase letters - validation error`() {
//        val req = companyRequest(vihbId = "123456vihb")
//        securedMockMvc.post(
//            "/companies",
//            objectMapper.writeValueAsString(req)
//        )
//            .andExpect(status().isBadRequest)
//            .andExpect(jsonPath("$.message").value("VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn"))
//    }
//
//    @Test
//    fun `create company - invalid vihbId with letters in digits section - validation error`() {
//        val req = companyRequest(vihbId = "12345AVIHB")
//        securedMockMvc.post(
//            "/companies",
//            objectMapper.writeValueAsString(req)
//        )
//            .andExpect(status().isBadRequest)
//            .andExpect(jsonPath("$.message").value("VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn"))
//    }
//
//    @Test
//    fun `create company - invalid vihbId with special characters - validation error`() {
//        val req = companyRequest(vihbId = "123456-VIH")
//        securedMockMvc.post(
//            "/companies",
//            objectMapper.writeValueAsString(req)
//        )
//            .andExpect(status().isBadRequest)
//            .andExpect(jsonPath("$.message").value("VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn"))
//    }
}