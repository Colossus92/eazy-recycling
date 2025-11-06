package nl.eazysoftware.eazyrecyclingservice.domain.model.waste

data class ProcessingMethod(
  val code: String,
) {
  init {
      require(code.length == 3 || (code.length == 4 && code.contains("."))) {
        "De verwerkingsmethode code moet 3 karakters (A01) of 4 karakters met daarin een punt bevatten (A.01)"
      }
  }
}
