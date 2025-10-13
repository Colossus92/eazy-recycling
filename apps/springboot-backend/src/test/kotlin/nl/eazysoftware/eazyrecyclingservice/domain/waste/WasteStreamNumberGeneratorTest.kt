package nl.eazysoftware.eazyrecyclingservice.domain.waste

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WasteStreamNumberGeneratorTest {

    private val generator = WasteStreamNumberGenerator()

    @Test
    fun `should generate first number when no existing numbers`() {
        // Given
        val processorId = ProcessorPartyId("12345")
        val highestExisting: WasteStreamNumber? = null

        // When
        val result = generator.generateNext(processorId, highestExisting)

        // Then
        assertEquals("123450000001", result.number)
    }

    @Test
    fun `should generate next sequential number`() {
        // Given
        val processorId = ProcessorPartyId("12345")
        val highestExisting = WasteStreamNumber("123450000012")

        // When
        val result = generator.generateNext(processorId, highestExisting)

        // Then
        assertEquals("123450000013", result.number)
    }

    @Test
    fun `should handle large sequential numbers with correct padding`() {
        // Given
        val processorId = ProcessorPartyId("98765")
        val highestExisting = WasteStreamNumber("987650009999")

        // When
        val result = generator.generateNext(processorId, highestExisting)

        // Then
        assertEquals("987650010000", result.number)
    }

    @Test
    fun `should maintain zero padding for small numbers`() {
        // Given
        val processorId = ProcessorPartyId("00001")
        val highestExisting = WasteStreamNumber("000010000003")

        // When
        val result = generator.generateNext(processorId, highestExisting)

        // Then
        assertEquals("000010000004", result.number)
    }

    @Test
    fun `should handle maximum sequential number`() {
        // Given
        val processorId = ProcessorPartyId("55555")
        val highestExisting = WasteStreamNumber("555559999998")

        // When
        val result = generator.generateNext(processorId, highestExisting)

        // Then
        assertEquals("555559999999", result.number)
    }

    @Test
    fun `should throw exception when maximum sequential number exceeded`() {
        // Given
        val processorId = ProcessorPartyId("55555")
        val highestExisting = WasteStreamNumber("555559999999")

        // When / Then
        val exception = assertThrows<IllegalArgumentException> {
            generator.generateNext(processorId, highestExisting)
        }

        assertEquals("Maximum aantal afvalstroomnummers bereikt voor verwerker 55555", exception.message)
    }

    @Test
    fun `should generate correct format with 12 digits total`() {
        // Given
        val processorId = ProcessorPartyId("67890")
        val highestExisting = WasteStreamNumber("678900000100")

        // When
        val result = generator.generateNext(processorId, highestExisting)

        // Then
        assertEquals(12, result.number.length)
        assertEquals("678900000101", result.number)
    }

    @Test
    fun `should handle transition from 999999 to 1000000`() {
        // Given
        val processorId = ProcessorPartyId("11111")
        val highestExisting = WasteStreamNumber("111110999999")

        // When
        val result = generator.generateNext(processorId, highestExisting)

        // Then
        assertEquals("111111000000", result.number)
    }

    @Test
    fun `generated number should start with processor ID`() {
        // Given
        val processorId = ProcessorPartyId("24680")
        val highestExisting = WasteStreamNumber("246800000042")

        // When
        val result = generator.generateNext(processorId, highestExisting)

        // Then
        assertEquals("24680", result.number.take(5))
        assertEquals("246800000043", result.number)
    }
}
