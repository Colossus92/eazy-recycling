package nl.eazysoftware.eazyrecyclingservice.domain.waste

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import kotlin.test.assertFailsWith

class WeightTest {

    @Test
    fun `should create weight with valid positive value`() {
        // Given/When
        val weight = Weight(BigDecimal("100.50"), Weight.WeightUnit.KILOGRAM)

        // Then
        assertThat(weight.value).isEqualByComparingTo(BigDecimal("100.50"))
        assertThat(weight.unit).isEqualTo(Weight.WeightUnit.KILOGRAM)
    }

    @Test
    fun `should create weight with zero value`() {
        // Given/When
        val weight = Weight(BigDecimal.ZERO, Weight.WeightUnit.KILOGRAM)

        // Then
        assertThat(weight.value).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `should fail when creating weight with negative value`() {
        // When/Then
        val exception = assertFailsWith<IllegalArgumentException> {
            Weight(BigDecimal("-10.00"), Weight.WeightUnit.KILOGRAM)
        }

        assertThat(exception).hasMessageContaining("Gewicht dient een positief getal te zijn")
    }

    @ParameterizedTest
    @CsvSource(
        "100.00, 50, 50.00",
        "100.00, 25, 25.00",
        "100.00, 75, 75.00",
        "100.00, 100, 100.00",
        "100.00, 1, 1.00",
        "200.50, 50, 100.25",
        "150.00, 33, 49.50",
        "150.00, 67, 100.50"
    )
    fun `should multiply weight by percentage correctly`(
        originalValue: String,
        percentage: Int,
        expectedValue: String
    ) {
        // Given
        val weight = Weight(BigDecimal(originalValue), Weight.WeightUnit.KILOGRAM)

        // When
        val result = weight.multiplyByPercentage(percentage)

        // Then
        assertThat(result.value).isEqualByComparingTo(BigDecimal(expectedValue))
        assertThat(result.unit).isEqualTo(Weight.WeightUnit.KILOGRAM)
    }

    @Test
    fun `should round to 2 decimal places when multiplying by percentage`() {
        // Given
        val weight = Weight(BigDecimal("100.33"), Weight.WeightUnit.KILOGRAM)

        // When
        val result = weight.multiplyByPercentage(33)

        // Then - 100.33 * 0.33 = 33.1089, rounded to 33.11
        assertThat(result.value).isEqualByComparingTo(BigDecimal("33.11"))
        assertThat(result.value.scale()).isEqualTo(2)
    }

    @Test
    fun `should round up when multiplying results in small fractions`() {
        // Given
        val weight = Weight(BigDecimal("10.00"), Weight.WeightUnit.KILOGRAM)

        // When
        val result = weight.multiplyByPercentage(5)

        // Then - 10.00 * 0.05 = 0.50
        assertThat(result.value).isEqualByComparingTo(BigDecimal("0.50"))
    }

    @Test
    fun `should handle very small percentages`() {
        // Given
        val weight = Weight(BigDecimal("1000.00"), Weight.WeightUnit.KILOGRAM)

        // When
        val result = weight.multiplyByPercentage(1)

        // Then - 1000.00 * 0.01 = 10.00
        assertThat(result.value).isEqualByComparingTo(BigDecimal("10.00"))
    }

    @Test
    fun `should handle large weights`() {
        // Given
        val weight = Weight(BigDecimal("999999.99"), Weight.WeightUnit.KILOGRAM)

        // When
        val result = weight.multiplyByPercentage(50)

        // Then
        assertThat(result.value).isEqualByComparingTo(BigDecimal("500000.00"))
    }

    @Test
    fun `should fail when percentage is zero`() {
        // Given
        val weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)

        // When/Then
        val exception = assertFailsWith<IllegalArgumentException> {
            weight.multiplyByPercentage(0)
        }

        assertThat(exception).hasMessageContaining("Percentage moet tussen 1 en 100 zijn")
    }

    @Test
    fun `should fail when percentage is negative`() {
        // Given
        val weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)

        // When/Then
        val exception = assertFailsWith<IllegalArgumentException> {
            weight.multiplyByPercentage(-10)
        }

        assertThat(exception).hasMessageContaining("Percentage moet tussen 1 en 100 zijn")
    }

    @Test
    fun `should fail when percentage is greater than 100`() {
        // Given
        val weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)

        // When/Then
        val exception = assertFailsWith<IllegalArgumentException> {
            weight.multiplyByPercentage(101)
        }

        assertThat(exception).hasMessageContaining("Percentage moet tussen 1 en 100 zijn")
    }

    @Test
    fun `should preserve weight unit when multiplying`() {
        // Given
        val weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)

        // When
        val result = weight.multiplyByPercentage(50)

        // Then
        assertThat(result.unit).isEqualTo(Weight.WeightUnit.KILOGRAM)
    }

    @Test
    fun `should handle decimal percentage calculations with proper rounding`() {
        // Given - Test case that requires proper rounding
        val weight = Weight(BigDecimal("123.45"), Weight.WeightUnit.KILOGRAM)

        // When
        val result = weight.multiplyByPercentage(37)

        // Then - 123.45 * 0.37 = 45.6765, rounded to 45.68
        assertThat(result.value).isEqualByComparingTo(BigDecimal("45.68"))
    }

    @Test
    fun `should handle rounding down correctly`() {
        // Given
        val weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)

        // When
        val result = weight.multiplyByPercentage(33)

        // Then - 100.00 * 0.33 = 33.00
        assertThat(result.value).isEqualByComparingTo(BigDecimal("33.00"))
    }

    @Test
    fun `should handle edge case with 99 percent`() {
        // Given
        val weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)

        // When
        val result = weight.multiplyByPercentage(99)

        // Then
        assertThat(result.value).isEqualByComparingTo(BigDecimal("99.00"))
    }

    @Test
    fun `should multiply zero weight by percentage`() {
        // Given
        val weight = Weight(BigDecimal.ZERO, Weight.WeightUnit.KILOGRAM)

        // When
        val result = weight.multiplyByPercentage(50)

        // Then
        assertThat(result.value).isEqualByComparingTo(BigDecimal("0.00"))
    }
}
