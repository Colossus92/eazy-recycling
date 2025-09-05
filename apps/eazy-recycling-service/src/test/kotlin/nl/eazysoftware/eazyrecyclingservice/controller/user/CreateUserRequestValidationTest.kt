package nl.eazysoftware.eazyrecyclingservice.controller.user

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.Test

@ExtendWith(SpringExtension::class)
@ImportAutoConfiguration(ValidationAutoConfiguration::class)
class CreateUserRequestValidationTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setup() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `should have no validation errors for valid CreateUserRequest`() {
        val validRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            password = "V@l1dP@ssw0rd",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(validRequest)

        assertThat(violations).isEmpty()
    }

    // Email validation tests
    @Test
    fun `should reject invalid email format`() {
        val invalidRequest = CreateUserRequest(
            email = "invalid-email",
            firstName = "John",
            lastName = "Doe",
            password = "V@l1dP@ssw0rd",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Ongeldig emailadres")
    }

    @Test
    fun `should reject email without domain`() {
        val invalidRequest = CreateUserRequest(
            email = "test@",
            firstName = "John",
            lastName = "Doe",
            password = "V@l1dP@ssw0rd",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Ongeldig emailadres")
    }

    // Password validation tests
    @Test
    fun `should reject password without uppercase letter`() {
        val invalidRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            password = "v@l1dp@ssw0rd",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Wachtwoord moet een kleine letter, hoofdletter, cijfer en speciaal karakter bevatten")
    }

    @Test
    fun `should reject password without lowercase letter`() {
        val invalidRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            password = "V@L1DP@SSW0RD",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Wachtwoord moet een kleine letter, hoofdletter, cijfer en speciaal karakter bevatten")
    }

    @Test
    fun `should reject password without digit`() {
        val invalidRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            password = "V@lidP@ssword",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Wachtwoord moet een kleine letter, hoofdletter, cijfer en speciaal karakter bevatten")
    }

    @Test
    fun `should reject password without special character`() {
        val invalidRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            password = "Val1dPassword",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Wachtwoord moet een kleine letter, hoofdletter, cijfer en speciaal karakter bevatten")
    }

    @Test
    fun `should reject password shorter than 8 characters`() {
        val invalidRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            password = "V@l1d12",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Wachtwoord moet een kleine letter, hoofdletter, cijfer en speciaal karakter bevatten")
    }

    // FirstName validation tests
    @Test
    fun `should reject blank firstName`() {
        val invalidRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "",
            lastName = "Doe",
            password = "V@l1dP@ssw0rd",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Voornaam mag niet leeg zijn")
    }

    @Test
    fun `should reject whitespace-only firstName`() {
        val invalidRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "   ",
            lastName = "Doe",
            password = "V@l1dP@ssw0rd",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Voornaam mag niet leeg zijn")
    }

    // LastName validation tests
    @Test
    fun `should reject blank lastName`() {
        val invalidRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "John",
            lastName = "",
            password = "V@l1dP@ssw0rd",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Achternaam mag niet leeg zijn")
    }

    @Test
    fun `should reject whitespace-only lastName`() {
        val invalidRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "John",
            lastName = "   ",
            password = "V@l1dP@ssw0rd",
            roles = arrayOf("planner")
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Achternaam mag niet leeg zijn")
    }

    // Roles validation tests
    @Test
    fun `should reject empty roles array`() {
        val invalidRequest = CreateUserRequest(
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            password = "V@l1dP@ssw0rd",
            roles = arrayOf()
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().message).isEqualTo("Rol mag niet leeg zijn")
    }

    // Multiple validation errors test
    @Test
    fun `should return multiple validation errors for multiple invalid fields`() {
        val invalidRequest = CreateUserRequest(
            email = "invalid-email",
            firstName = "",
            lastName = "",
            password = "weak",
            roles = arrayOf()
        )

        val violations = validator.validate(invalidRequest)

        assertThat(violations).hasSize(5)
        val messages = violations.map { it.message }.toSet()
        assertThat(messages).containsExactlyInAnyOrder(
            "Ongeldig emailadres",
            "Voornaam mag niet leeg zijn",
            "Achternaam mag niet leeg zijn",
            "Wachtwoord moet een kleine letter, hoofdletter, cijfer en speciaal karakter bevatten",
            "Rol mag niet leeg zijn"
        )
    }
}