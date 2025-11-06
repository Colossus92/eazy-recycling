package nl.eazysoftware.eazyrecyclingservice.domain.model.waste

data class EuralCode(
  val code: String
) {

  init {
    val codeWithoutSpaces = code.replace(" ", "")
    require(codeWithoutSpaces.take(6).all { it.isDigit() }) {
      "De eerste 6 tekens van de eural code moeten cijfers zijn"
    }
    require(codeWithoutSpaces.length == 6 || (codeWithoutSpaces.length == 7 && codeWithoutSpaces.endsWith("*"))) {
      "Eural code moet 6 cijfers bevatten, eventueel gevolgd door *, spaties zijn toegestaan"
    }
  }
}
