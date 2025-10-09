package nl.eazysoftware.eazyrecyclingservice.domain.model.company

class Person(
  val firstName: String,
  val lastName: String,
  val email: String?,
  val phoneNumber: String?,
  val roles: List<String>,
) {

  init {
    require(firstName.isNotBlank()) { "De voornaam is verplicht" }
    require(lastName.isNotBlank()) { "De achternaam is verplicht" }
  }
}
