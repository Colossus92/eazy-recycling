package nl.eazysoftware.eazyrecyclingservice.config.database

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import kotlinx.datetime.YearMonth

/**
 * JPA converter for kotlinx.datetime.YearMonth to/from database string representation.
 * Stores YearMonth as ISO-8601 format (e.g., "2025-11")
 */
@Converter(autoApply = true)
class YearMonthConverter : AttributeConverter<YearMonth, String> {

  override fun convertToDatabaseColumn(attribute: YearMonth?): String? {
    return attribute?.toString()
  }

  override fun convertToEntityAttribute(dbData: String?): YearMonth? {
    return dbData?.let { YearMonth.parse(it) }
  }
}
