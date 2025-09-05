package nl.eazysoftware.eazyrecyclingservice.controller.company

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
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
            AddressRequest(
                "Main St",
                "",
                "1",
                "1234 AB",
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
            AddressRequest(
                "Main St",
                "",
                "1",
                "1234 AB",
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

    @ParameterizedTest
    @MethodSource("validPostalCodes")
    fun `valid postal code should not have any violations`(validPostalCode: String) {
        val company = CompanyController.CompanyRequest(
            "12345678",
            "123456VIHB",
            "Company",
            AddressRequest(
                "Main St",
                "",
                "1",
                validPostalCode,
                "Amsterdam"
            )
        )
        val violations = validator.validate(company)
        assertThat(violations).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidPostalCodes")
    fun `invalid postal code should have a violation`(invalidPostalCode: String) {
        val company = CompanyController.CompanyRequest(
            "12345678",
            "123456VIHB",
            "Company",
            AddressRequest(
                "Main St",
                "",
                "1",
                invalidPostalCode,
                "Amsterdam"
            )
        )
        val violations = validator.validate(company)

        assertThat(violations).hasSize(1)
        assertThat(violations).extracting(ConstraintViolation<*>::getMessage).containsExactly(
            tuple("Postcode moet bestaan uit 4 cijfers gevolgd door een spatie en 2 hoofdletters")
        )
    }

    companion object {

        private val addressRequest = AddressRequest(
            "Main St",
            "",
            "1",
            "1234 AB",
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
            "1234567VIHB",    // 7 digits (too many digits)
            "12345VIHB",      // 5 digits (too few digits)
            "123456VIHBA",    // 5 letters (too many letters)
            "1234-5VIHB",     // special characters in digits
            "VIHB123456",     // letters first, digits last
            "123456VIHBX",    // invalid letter combination (not from VIHBX)
            "123456VIH",      // 3 letters (too few letters)
            "123456ABCD",     // invalid letters (A, C, D not allowed)
            "123456vihb",     // lowercase letters
            "123456Vihb",     // mixed case letters
            "123456VIHB1",    // digit at end instead of letter
            "12345AVIHB",     // letter in digit section
            "123456VI-B",     // special character in letter section
            "123 456VIHB",    // space in digit section
        )

        @JvmStatic
        fun validPostalCodes() = listOf(
            "1234 AB",        // standard Dutch postal code
            "9999 ZZ",        // edge case with highest values
            "0000 AA",        // edge case with lowest values
            "5678 XY",        // mixed letters
            "1111 BB",        // repeated digits and letters
        )

        @JvmStatic
        fun invalidPostalCodes() = listOf(
            "123 AB",         // 3 digits (too few)
            "12345 AB",       // 5 digits (too many)
            "1234AB",         // no space
            "1234  AB",       // double space
            "1234 ab",        // lowercase letters
            "1234 Ab",        // mixed case letters
            "1234 A",         // 1 letter (too few)
            "1234 ABC",       // 3 letters (too many)
            "1234 A1",        // digit in letter section
            "123A AB",        // letter in digit section
            "1234-AB",        // dash instead of space
            "1234.AB",        // dot instead of space
            " 1234 AB",       // leading space
            "1234 AB ",       // trailing space
            "12 34 AB",       // space in digit section
            "1234 A B",       // space in letter section
            "",               // empty string
            "ABCD EF",        // all letters in digit section
            "1234 12",        // all digits in letter section
        )

    }
}