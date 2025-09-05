package nl.eazysoftware.eazyrecyclingservice.controller.user

data class UpdateUserRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: Array<String>,
) {
}
