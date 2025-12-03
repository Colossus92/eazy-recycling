package nl.eazysoftware.eazyrecyclingservice.adapters.out.exact

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AddressLineParserTest {

  @Test
  @DisplayName("Parse address with street, number and addition without space")
  fun `parse street with number and addition`() {
    val result = AddressLineParser.parse("Kerkstraat 123A")

    assertEquals("Kerkstraat", result.first)
    assertEquals("123", result.second)
    assertEquals("A", result.third)
  }

  @Test
  @DisplayName("Parse address with street, number and addition with space")
  fun `parse street with number and addition with space`() {
    val result = AddressLineParser.parse("Kerkstraat 123 A")

    assertEquals("Kerkstraat", result.first)
    assertEquals("123", result.second)
    assertEquals("A", result.third)
  }

  @Test
  @DisplayName("Parse address with street and number only")
  fun `parse street with number only`() {
    val result = AddressLineParser.parse("Kerkstraat 123")

    assertEquals("Kerkstraat", result.first)
    assertEquals("123", result.second)
    assertNull(result.third)
  }

  @Test
  @DisplayName("Parse address with multi-word street name")
  fun `parse multi-word street name`() {
    val result = AddressLineParser.parse("Jan van Galenstraat 45B")

    assertEquals("Jan van Galenstraat", result.first)
    assertEquals("45", result.second)
    assertEquals("B", result.third)
  }

  @Test
  @DisplayName("Parse address with multi-word street name and space before addition")
  fun `parse multi-word street name with space before addition`() {
    val result = AddressLineParser.parse("Jan van Galenstraat 45 B")

    assertEquals("Jan van Galenstraat", result.first)
    assertEquals("45", result.second)
    assertEquals("B", result.third)
  }

  @Test
  @DisplayName("Parse address with only street name (no number)")
  fun `parse street name only`() {
    val result = AddressLineParser.parse("Kerkstraat")

    assertEquals("Kerkstraat", result.first)
    assertEquals("", result.second)
    assertNull(result.third)
  }

  @Test
  @DisplayName("Parse null address line")
  fun `parse null address`() {
    val result = AddressLineParser.parse(null)

    assertEquals("", result.first)
    assertEquals("", result.second)
    assertNull(result.third)
  }

  @Test
  @DisplayName("Parse empty address line")
  fun `parse empty address`() {
    val result = AddressLineParser.parse("")

    assertEquals("", result.first)
    assertEquals("", result.second)
    assertNull(result.third)
  }

  @Test
  @DisplayName("Parse blank address line")
  fun `parse blank address`() {
    val result = AddressLineParser.parse("   ")

    assertEquals("", result.first)
    assertEquals("", result.second)
    assertNull(result.third)
  }

  @Test
  @DisplayName("Parse address with lowercase addition")
  fun `parse lowercase addition`() {
    val result = AddressLineParser.parse("Hoofdstraat 10a")

    assertEquals("Hoofdstraat", result.first)
    assertEquals("10", result.second)
    assertEquals("a", result.third)
  }

  @Test
  @DisplayName("Parse address with lowercase addition and space")
  fun `parse lowercase addition with space`() {
    val result = AddressLineParser.parse("Hoofdstraat 10 a")

    assertEquals("Hoofdstraat", result.first)
    assertEquals("10", result.second)
    assertEquals("a", result.third)
  }

  @Test
  @DisplayName("Parse address with leading/trailing whitespace")
  fun `parse address with whitespace`() {
    val result = AddressLineParser.parse("  Kerkstraat 123A  ")

    assertEquals("Kerkstraat", result.first)
    assertEquals("123", result.second)
    assertEquals("A", result.third)
  }

  @Test
  @DisplayName("Parse address with multi-letter addition")
  fun `parse multi-letter addition`() {
    val result = AddressLineParser.parse("Dorpsweg 5bis")

    assertEquals("Dorpsweg", result.first)
    assertEquals("5", result.second)
    assertEquals("bis", result.third)
  }

  @Test
  @DisplayName("Parse address with multi-letter addition and space")
  fun `parse multi-letter addition with space`() {
    val result = AddressLineParser.parse("Dorpsweg 5 bis")

    assertEquals("Dorpsweg", result.first)
    assertEquals("5", result.second)
    assertEquals("bis", result.third)
  }

  @Test
  @DisplayName("Parse ridiculously long but real street name")
  fun `parse ridiculously long but real street name`() {
    val result = AddressLineParser.parse("Ir. Mr. Dr. van Waterschoot van der Grachtstraat 67 bis")

    assertEquals("Ir. Mr. Dr. van Waterschoot van der Grachtstraat", result.first)
    assertEquals("67", result.second)
    assertEquals("bis", result.third)
  }
}
