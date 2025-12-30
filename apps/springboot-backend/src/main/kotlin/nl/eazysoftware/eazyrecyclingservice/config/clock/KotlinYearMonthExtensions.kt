package nl.eazysoftware.eazyrecyclingservice.config.clock

import kotlinx.datetime.YearMonth
import kotlinx.datetime.number


fun String.toYearMonth(): YearMonth {
  require(this.length == 6) { "Period moet 6 cijfers bevatten (MMyyyy formaat), maar was: $this" }

  val month = this.substring(0, 2).toIntOrNull()
    ?: throw IllegalArgumentException("Ongeldige maand in periode: $this")
  val year = this.substring(2, 6).toIntOrNull()
    ?: throw IllegalArgumentException("Ongeldig jaar in periode: $this")

  require(month in 1..12) { "Maand moet tussen 1 en 12 zijn, maar was: $month" }
  require(year >= 2005) { "Jaar moet in of na 2005 zijn: $year" }

  return YearMonth(year, month)
}

fun YearMonth.toLmaPeriod(): String {
  return "${this.month.number.toString().padStart(2, '0')}${this.year}"
}
