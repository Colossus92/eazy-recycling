package nl.eazysoftware.eazyrecyclingservice.domain.waste

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.EuralCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertFailsWith

class EuralCodeTest {

  @ParameterizedTest
  @ValueSource(strings = ["123456", "170101", "200301"])
  fun `valid 6-digit eural codes are accepted`(code: String) {
    // When
    val euralCode = assertDoesNotThrow {
      EuralCode(code)
    }

    // Then
    assertThat(euralCode.code).isEqualTo(code)
  }

  @ParameterizedTest
  @ValueSource(strings = ["123456*", "170101*", "200301*"])
  fun `valid 6-digit eural codes with asterisk are accepted`(code: String) {
    // When
    val euralCode = assertDoesNotThrow {
      EuralCode(code)
    }

    // Then
    assertThat(euralCode.code).isEqualTo(code)
  }

  @ParameterizedTest
  @ValueSource(strings = ["12 34 56", "17 01 01", "20 03 01"])
  fun `valid 6-digit eural codes with spaces are accepted`(code: String) {
    // When
    val euralCode = assertDoesNotThrow {
      EuralCode(code)
    }

    // Then
    assertThat(euralCode.code).isEqualTo(code)
  }

  @ParameterizedTest
  @ValueSource(strings = ["12 34 56*", "17 01 01*", "20 03 01*"])
  fun `valid 6-digit eural codes with spaces and asterisk are accepted`(code: String) {
    // When
    val euralCode = assertDoesNotThrow {
      EuralCode(code)
    }

    // Then
    assertThat(euralCode.code).isEqualTo(code)
  }

  @Test
  fun `eural code with mixed spacing is accepted`() {
    // When
    val euralCode = assertDoesNotThrow {
      EuralCode("1 23456")
    }

    // Then
    assertThat(euralCode.code).isEqualTo("1 23456")
  }

  @ParameterizedTest
  @ValueSource(strings = ["", "12345", "1234567"])
  fun `invalid eural codes are rejected`(code: String) {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      EuralCode(code)
    }

    assertThat(exception.message).contains("Eural code moet 6 cijfers bevatten")
  }

  @Test
  fun `invalid eural codes are rejected for too few digits`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      EuralCode("12345**")
    }

    assertThat(exception.message).contains("De eerste 6 tekens van de eural code moeten cijfers zijn")
  }

  @Test
  fun `eural code with too few digits is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      EuralCode("12345")
    }

    assertThat(exception.message).contains("Eural code moet 6 cijfers bevatten")
  }

  @Test
  fun `eural code with too many digits without asterisk is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      EuralCode("1234567")
    }

    assertThat(exception.message).contains("Eural code moet 6 cijfers bevatten")
  }

  @Test
  fun `eural code with asterisk in wrong position is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      EuralCode("12*456")
    }

    assertThat(exception.message).contains("De eerste 6 tekens van de eural code moeten cijfers zijn")
  }

  @Test
  fun `eural code with multiple asterisks is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      EuralCode("123456**")
    }

    assertThat(exception.message).contains("Eural code moet 6 cijfers bevatten")
  }

  @Test
  fun `blank eural code is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      EuralCode("")
    }

    assertThat(exception.message).contains("Eural code moet 6 cijfers bevatten")
  }

  @Test
  fun `eural code with only spaces is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      EuralCode("      ")
    }

    assertThat(exception.message).contains("Eural code moet 6 cijfers bevatten")
  }
}
