package nl.eazysoftware.eazyrecyclingservice.domain.transport

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class VihbNumberTest {

  @ParameterizedTest
  @CsvSource(value = ["000100VXXX", "000200VXXX", "123456VIHB", "000100XVIH", "000100XXVI", "000100XXXV", "999999VIHB"])
  fun `valid VIHB numbers should be accepted`(value: String) {
    val vihb = VihbNumber(value)

    assertEquals(value, vihb.value)
  }

  @ParameterizedTest
  @CsvSource(value = ["12345VXXX", "1234567VXXX", "123456VXX", "123456VXXXX", ])
  fun `VIHB number should have exactly 6 digits and 4 letters`(value: String) {
    val exception = assertThrows<IllegalArgumentException> {
      VihbNumber(value)
    }
    assert(exception.message!!.contains("exact 6 cijfers"))
  }

  @Test
  fun `VIHB number with invalid letter should be rejected`() {
    val exception = assertThrows<IllegalArgumentException> {
      VihbNumber("123456VAAA")
    }
    assert(exception.message!!.contains("X, V, I, H of B"))
  }

  @Test
  fun `VIHB number with lowercase letters should be rejected`() {
    val exception = assertThrows<IllegalArgumentException> {
      VihbNumber("123456vxxx")
    }
    assert(exception.message!!.contains("X, V, I, H of B"))
  }

  @Test
  fun `VIHB number with only X letters should be rejected`() {
    val exception = assertThrows<IllegalArgumentException> {
      VihbNumber("123456XXXX")
    }
    assert(exception.message!!.contains("maximaal 3"))
  }

  @Test
  fun `VIHB number with letters before digits should be rejected`() {
    val exception = assertThrows<IllegalArgumentException> {
      VihbNumber("VXXX123456")
    }
    assert(exception.message!!.contains("exact 6 cijfers"))
  }

  @Test
  fun `VIHB number with mixed digits and letters should be rejected`() {
    val exception = assertThrows<IllegalArgumentException> {
      VihbNumber("12V34X56HB")
    }
    assert(exception.message!!.contains("exact 6 cijfers"))
  }
}
