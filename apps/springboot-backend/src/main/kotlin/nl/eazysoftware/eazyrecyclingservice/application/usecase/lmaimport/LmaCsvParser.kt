package nl.eazysoftware.eazyrecyclingservice.application.usecase.lmaimport

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Parser for LMA CSV export files.
 * Handles the specific format exported from the LMA portal.
 */
@Component
class LmaCsvParser {

  companion object {
    // CSV column headers from LMA export
    const val COL_AFVALSTROOMNUMMER = "Afvalstroomnummer"
    const val COL_VERWERKERSNUMMER = "Verwerkersnummer Locatie van Bestemming (LvB)"
    const val COL_LVB_KVK = "LvB KvK nummer"
    const val COL_LVB_BEDRIJFSNAAM = "LvB Bedrijfsnaam"
    const val COL_LVB_ADRES = "LvB Adres"
    const val COL_LVB_POSTCODE_PLAATS = "LvB Postcode/Plaats"
    const val COL_HANDELSREGISTERNUMMER_ONTDOENER = "Handelsregisternummer Ontdoener"
    const val COL_NAAM_ONTDOENER = "Naam Ontdoener"
    const val COL_LAND_ONTDOENER = "Land Ontdoener"
    const val COL_LOCATIE_STRAATNAAM = "LocatieHerkomst Straatnaam"
    const val COL_LOCATIE_HUISNUMMER = "LocatieHerkomst Huisnummer"
    const val COL_LOCATIE_HUISNUMMER_TOEVOEGING = "LocatieHerkomst HuisnummerToevoeging"
    const val COL_LOCATIE_POSTCODE = "LocatieHerkomst Postcode"
    const val COL_LOCATIE_PLAATS = "LocatieHerkomst Plaats"
    const val COL_LOCATIE_NABIJHEID = "LocatieHerkomst Nabijheidsbeschrijving"
    const val COL_LOCATIE_LAND = "LocatieHerkomst Land"
    const val COL_EURALCODE = "Euralcode"
    const val COL_EURALCODE_OMSCHRIJVING = "Euralcode Omschrijving"
    const val COL_GEBRUIKELIJKE_NAAM = "Gebruikelijke Naam Afvalstof"
    const val COL_VERWERKINGSMETHODE_CODE = "VerwerkingsMethode Code"
    const val COL_VERWERKINGSMETHODE_OMSCHRIJVING = "VerwerkingsMethode Omschrijving"
    const val COL_ROUTEINZAMELING = "Routeinzameling"
    const val COL_INZAMELAARSREGELING = "Inzamelaarsregeling"
    const val COL_PARTICULIERE_ONTDOENER = "Particuliere Ontdoener"
  }

  /**
   * Parses an LMA CSV export file.
   *
   * @param inputStream The CSV file input stream
   * @return List of parsed CSV records
   */
  fun parse(inputStream: InputStream): List<LmaCsvRecord> {
    val reader = InputStreamReader(inputStream)
    val csvFormat = CSVFormat.DEFAULT.builder()
      .setHeader()
      .setSkipHeaderRecord(true)
      .setIgnoreEmptyLines(true)
      .setTrim(true)
      .build()

    return CSVParser(reader, csvFormat).use { parser ->
      parser.records.mapIndexed { index, record ->
        parseSingleRecord(record, index + 2) // +2 because row 1 is header, index is 0-based
      }
    }
  }

  private fun parseSingleRecord(record: CSVRecord, rowNumber: Int): LmaCsvRecord {
    return LmaCsvRecord(
      rowNumber = rowNumber,
      afvalstroomnummer = record.getOrNull(COL_AFVALSTROOMNUMMER),
      verwerkersnummer = record.getOrNull(COL_VERWERKERSNUMMER),
      lvbKvkNummer = record.getOrNull(COL_LVB_KVK),
      lvbBedrijfsnaam = record.getOrNull(COL_LVB_BEDRIJFSNAAM),
      lvbAdres = record.getOrNull(COL_LVB_ADRES),
      lvbPostcodePlaats = record.getOrNull(COL_LVB_POSTCODE_PLAATS),
      handelsregisternummerOntdoener = record.getOrNull(COL_HANDELSREGISTERNUMMER_ONTDOENER),
      naamOntdoener = record.getOrNull(COL_NAAM_ONTDOENER),
      landOntdoener = record.getOrNull(COL_LAND_ONTDOENER),
      locatieStraatnaam = record.getOrNull(COL_LOCATIE_STRAATNAAM),
      locatieHuisnummer = record.getOrNull(COL_LOCATIE_HUISNUMMER),
      locatieHuisnummerToevoeging = record.getOrNull(COL_LOCATIE_HUISNUMMER_TOEVOEGING),
      locatiePostcode = record.getOrNull(COL_LOCATIE_POSTCODE),
      locatiePlaats = record.getOrNull(COL_LOCATIE_PLAATS),
      locatieNabijheid = record.getOrNull(COL_LOCATIE_NABIJHEID),
      locatieLand = record.getOrNull(COL_LOCATIE_LAND),
      euralcode = record.getOrNull(COL_EURALCODE),
      euralcodeOmschrijving = record.getOrNull(COL_EURALCODE_OMSCHRIJVING),
      gebruikelijkeNaam = record.getOrNull(COL_GEBRUIKELIJKE_NAAM),
      verwerkingsmethodeCode = record.getOrNull(COL_VERWERKINGSMETHODE_CODE),
      verwerkingsmethodeOmschrijving = record.getOrNull(COL_VERWERKINGSMETHODE_OMSCHRIJVING),
      routeinzameling = record.getOrNull(COL_ROUTEINZAMELING),
      inzamelaarsregeling = record.getOrNull(COL_INZAMELAARSREGELING),
      particuliereOntdoener = record.getOrNull(COL_PARTICULIERE_ONTDOENER)
    )
  }

  private fun CSVRecord.getOrNull(column: String): String? {
    return try {
      val value = this.get(column)
      if (value.isNullOrBlank()) null else value.trim()
    } catch (e: IllegalArgumentException) {
      null
    }
  }
}

/**
 * Parsed record from LMA CSV export.
 */
data class LmaCsvRecord(
  val rowNumber: Int,
  val afvalstroomnummer: String?,
  val verwerkersnummer: String?,
  val lvbKvkNummer: String?,
  val lvbBedrijfsnaam: String?,
  val lvbAdres: String?,
  val lvbPostcodePlaats: String?,
  val handelsregisternummerOntdoener: String?,
  val naamOntdoener: String?,
  val landOntdoener: String?,
  val locatieStraatnaam: String?,
  val locatieHuisnummer: String?,
  val locatieHuisnummerToevoeging: String?,
  val locatiePostcode: String?,
  val locatiePlaats: String?,
  val locatieNabijheid: String?,
  val locatieLand: String?,
  val euralcode: String?,
  val euralcodeOmschrijving: String?,
  val gebruikelijkeNaam: String?,
  val verwerkingsmethodeCode: String?,
  val verwerkingsmethodeOmschrijving: String?,
  val routeinzameling: String?,
  val inzamelaarsregeling: String?,
  val particuliereOntdoener: String?
) {
  /**
   * Returns true if this record is for a private consignor (Particuliere Ontdoener = "J").
   * Private consignors are not supported yet and should be skipped.
   */
  val isPrivateConsignor: Boolean
    get() = particuliereOntdoener?.uppercase() == "J"

  /**
   * Converts the record to a map for storing as raw data in error records.
   */
  fun toRawDataMap(): Map<String, String> {
    return mapOf(
      "afvalstroomnummer" to (afvalstroomnummer ?: ""),
      "verwerkersnummer" to (verwerkersnummer ?: ""),
      "handelsregisternummerOntdoener" to (handelsregisternummerOntdoener ?: ""),
      "naamOntdoener" to (naamOntdoener ?: ""),
      "euralcode" to (euralcode ?: ""),
      "gebruikelijkeNaam" to (gebruikelijkeNaam ?: ""),
      "verwerkingsmethodeCode" to (verwerkingsmethodeCode ?: "")
    ).filterValues { it.isNotEmpty() }
  }
}
