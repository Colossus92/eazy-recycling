package nl.eazysoftware.eazyrecyclingservice.domain.waste

import nl.eazysoftware.eazyrecyclingservice.domain.model.Tenant
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumberGenerator
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamSequences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WasteStreamNumberGeneratorTest {

    private val tenantProcessorId = Tenant.processorPartyId // "08797"

    @Test
    fun `should generate number with correct format using sequence value`() {
        // Given
        val mockSequences = object : WasteStreamSequences {
            override fun nextValue(processorId: ProcessorPartyId) = 1L
        }
        val generator = WasteStreamNumberGenerator(mockSequences)

        // When
        val result = generator.generateNext()

        // Then
        assertEquals("087970000001", result.number)
    }

    @Test
    fun `should pad sequence value to 7 digits`() {
        // Given
        val mockSequences = object : WasteStreamSequences {
            override fun nextValue(processorId: ProcessorPartyId) = 42L
        }
        val generator = WasteStreamNumberGenerator(mockSequences)

        // When
        val result = generator.generateNext()

        // Then
        assertEquals("087970000042", result.number)
    }

    @Test
    fun `should handle large sequential numbers with correct padding`() {
        // Given
        val mockSequences = object : WasteStreamSequences {
            override fun nextValue(processorId: ProcessorPartyId) = 10000L
        }
        val generator = WasteStreamNumberGenerator(mockSequences)

        // When
        val result = generator.generateNext()

        // Then
        assertEquals("087970010000", result.number)
    }

    @Test
    fun `should handle maximum sequential number`() {
        // Given
        val mockSequences = object : WasteStreamSequences {
            override fun nextValue(processorId: ProcessorPartyId) = 9999999L
        }
        val generator = WasteStreamNumberGenerator(mockSequences)

        // When
        val result = generator.generateNext()

        // Then
        assertEquals("087979999999", result.number)
    }

    @Test
    fun `should throw exception when maximum sequential number exceeded`() {
        // Given
        val mockSequences = object : WasteStreamSequences {
            override fun nextValue(processorId: ProcessorPartyId) = 10000000L
        }
        val generator = WasteStreamNumberGenerator(mockSequences)

        // When / Then
        val exception = assertThrows<IllegalArgumentException> {
            generator.generateNext()
        }

        assertEquals("Maximum aantal afvalstroomnummers bereikt voor verwerker ${tenantProcessorId.number}", exception.message)
    }

    @Test
    fun `should generate correct format with 12 digits total`() {
        // Given
        val mockSequences = object : WasteStreamSequences {
            override fun nextValue(processorId: ProcessorPartyId) = 101L
        }
        val generator = WasteStreamNumberGenerator(mockSequences)

        // When
        val result = generator.generateNext()

        // Then
        assertEquals(12, result.number.length)
        assertEquals("087970000101", result.number)
    }

    @Test
    fun `should handle 7-digit sequence number without padding`() {
        // Given
        val mockSequences = object : WasteStreamSequences {
            override fun nextValue(processorId: ProcessorPartyId) = 1000000L
        }
        val generator = WasteStreamNumberGenerator(mockSequences)

        // When
        val result = generator.generateNext()

        // Then
        assertEquals("087971000000", result.number)
    }

    @Test
    fun `generated number should start with tenant processor ID`() {
        // Given
        val mockSequences = object : WasteStreamSequences {
            override fun nextValue(processorId: ProcessorPartyId) = 43L
        }
        val generator = WasteStreamNumberGenerator(mockSequences)

        // When
        val result = generator.generateNext()

        // Then
        assertEquals(tenantProcessorId.number, result.number.take(5))
        assertEquals("087970000043", result.number)
    }

    @Test
    fun `should pass tenant processor ID to sequence`() {
        // Given
        var capturedProcessorId: ProcessorPartyId? = null
        val mockSequences = object : WasteStreamSequences {
            override fun nextValue(processorId: ProcessorPartyId): Long {
                capturedProcessorId = processorId
                return 1000000L
            }
        }
        val generator = WasteStreamNumberGenerator(mockSequences)

        // When
        generator.generateNext()

        // Then
        assertEquals(tenantProcessorId, capturedProcessorId)
    }
}
