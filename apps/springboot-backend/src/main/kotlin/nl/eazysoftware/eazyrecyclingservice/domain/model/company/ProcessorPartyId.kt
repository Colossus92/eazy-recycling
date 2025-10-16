package nl.eazysoftware.eazyrecyclingservice.domain.model.company

data class ProcessorPartyId(
  val number: String
) {
  init {
    require(number.length == 5) {
      "Het verwerkersnummer moet exact 5 tekens lang zijn, maar is: $number"
    }
  }
}
