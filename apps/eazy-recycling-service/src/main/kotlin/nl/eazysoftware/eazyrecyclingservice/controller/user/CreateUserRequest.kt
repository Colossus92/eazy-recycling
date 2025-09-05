package nl.eazysoftware.eazyrecyclingservice.controller.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

data class CreateUserRequest(
    @field:Email(message = "Ongeldig emailadres")
    val email: String,
    @field:Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$", message = "Wachtwoord moet een kleine letter, hoofdletter, cijfer en speciaal karakter bevatten")
    val password: String,
    @field:NotBlank(message = "Voornaam mag niet leeg zijn")
    val firstName: String,
    @field:NotBlank(message = "Achternaam mag niet leeg zijn")
    val lastName: String,
    @field:NotEmpty(message = "Rol mag niet leeg zijn")
    val roles: Array<String>,
) {
}
