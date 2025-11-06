package nl.eazysoftware.eazyrecyclingservice.domain.waste

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.ProcessingMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertFailsWith

class ProcessingMethodTest {

  @ParameterizedTest
  @ValueSource(strings = ["A01", "B02", "R13", "D10", "XYZ"])
  fun `valid 3-character processing method codes are accepted`(code: String) {
    // When
    val processingMethod = assertDoesNotThrow {
      ProcessingMethod(code)
    }

    // Then
    assertThat(processingMethod.code).isEqualTo(code)
  }

  @ParameterizedTest
  @ValueSource(strings = ["A.01", "B.02", "R.13", "D.10"])
  fun `valid 4-character processing method codes with dot are accepted`(code: String) {
    // When
    val processingMethod = assertDoesNotThrow {
      ProcessingMethod(code)
    }

    // Then
    assertThat(processingMethod.code).isEqualTo(code)
  }

  @Test
  fun `processing method with dot in second position is accepted`() {
    // When
    val processingMethod = assertDoesNotThrow {
      ProcessingMethod("A.01")
    }

    // Then
    assertThat(processingMethod.code).isEqualTo("A.01")
  }

  @ParameterizedTest
  @ValueSource(strings = ["", "A", "AB", "ABCD", "AB01", "A-01", "A 01"])
  fun `invalid processing method codes are rejected`(code: String) {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      ProcessingMethod(code)
    }

    assertThat(exception.message).contains("De verwerkingsmethode code moet 3 karakters")
  }

  @Test
  fun `processing method with too few characters is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      ProcessingMethod("AB")
    }

    assertThat(exception.message).contains("De verwerkingsmethode code moet 3 karakters")
  }

  @Test
  fun `processing method with too many characters is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      ProcessingMethod("ABCDE")
    }

    assertThat(exception.message).contains("De verwerkingsmethode code moet 3 karakters")
  }

  @Test
  fun `processing method with 4 characters but no dot is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      ProcessingMethod("AB01")
    }

    assertThat(exception.message).contains("De verwerkingsmethode code moet 3 karakters")
  }

  @Test
  fun `processing method with dash instead of dot is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      ProcessingMethod("A-01")
    }

    assertThat(exception.message).contains("De verwerkingsmethode code moet 3 karakters")
  }

  @Test
  fun `processing method with space instead of dot is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      ProcessingMethod("A 01")
    }

    assertThat(exception.message).contains("De verwerkingsmethode code moet 3 karakters")
  }

  @Test
  fun `blank processing method code is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      ProcessingMethod("")
    }

    assertThat(exception.message).contains("De verwerkingsmethode code moet 3 karakters")
  }

  @Test
  fun `processing method with multiple dots is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      ProcessingMethod("A.0.1")
    }

    assertThat(exception.message).contains("De verwerkingsmethode code moet 3 karakters")
  }

  @Test
  fun `processing method with 5 characters including dot is rejected`() {
    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> {
      ProcessingMethod("A.012")
    }

    assertThat(exception.message).contains("De verwerkingsmethode code moet 3 karakters")
  }
}
