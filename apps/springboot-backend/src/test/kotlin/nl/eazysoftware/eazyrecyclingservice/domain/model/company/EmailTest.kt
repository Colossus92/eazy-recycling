package nl.eazysoftware.eazyrecyclingservice.domain.model.company

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class EmailTest {

  @ParameterizedTest
  @ValueSource(
    strings = [
      "test@example.com",
      "user.name@example.com",
      "user+tag@example.co.uk",
      "user_name@example.com",
      "user-name@example.com",
      "user123@example.com",
      "123user@example.com",
      "a@example.com",
      "test@sub.domain.example.com",
      "user%test@example.com",
      "first.last@example.co",
      "email@example-domain.com",
      "email@example.museum",
      "email@example.travel",
      "test+filter@example.com",
      "user_123@test-domain.co.uk",
      "user..name@example.com",      // Consecutive dots (regex allows)
      ".user@example.com",            // Leading dot (regex allows)
      "user.@example.com",            // Trailing dot (regex allows)
      "user@.example.com",            // Leading dot in domain (regex allows)
      "user@example..com"             // Consecutive dots in domain (regex allows)
    ]
  )
  fun `valid email addresses should be accepted`(value: String) {
    // When
    val email = Email(value)

    // Then
    assertEquals(value, email.value)
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "",                           // Empty string
      "plaintext",                  // No @ symbol
      "@example.com",               // Missing local part
      "user@",                      // Missing domain
      "user@domain",                // Missing TLD
      "user @example.com",          // Space in local part
      "user@exam ple.com",          // Space in domain
      "user@example.com.",          // Trailing dot in domain
      "user name@example.com",      // Space in email
      "user@example",               // No TLD
      "user@.com",                  // Missing domain name
      "user@@example.com",          // Double @
      "user@example@com",           // Multiple @ symbols
    ]
  )
  fun `invalid email addresses should be rejected`(value: String) {
    // When & Then
    val exception = assertThrows<IllegalArgumentException> {
      Email(value)
    }
    assert(exception.message!!.contains("geldig emailadres"))
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "user@example.c",             // TLD too short (1 char)
      "user@",                      // No domain at all
      "user",                       // No @ symbol
      "@",                          // Only @ symbol
      "user@domain.",               // TLD missing
    ]
  )
  fun `email with invalid TLD should be rejected`(value: String) {
    // When & Then
    val exception = assertThrows<IllegalArgumentException> {
      Email(value)
    }
    assert(exception.message!!.contains("geldig emailadres"))
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "user!name@example.com",      // Contains !
      "user#name@example.com",      // Contains #
      "user\$name@example.com",     // Contains $
      "user&name@example.com",      // Contains &
      "user*name@example.com",      // Contains *
      "user(name@example.com",      // Contains (
      "user)name@example.com",      // Contains )
      "user=name@example.com",      // Contains =
      "user[name@example.com",      // Contains [
      "user]name@example.com",      // Contains ]
      "user{name@example.com",      // Contains {
      "user}name@example.com",      // Contains }
      "user|name@example.com",      // Contains |
      "user\\name@example.com",     // Contains \
      "user/name@example.com",      // Contains /
      "user<name@example.com",      // Contains <
      "user>name@example.com",      // Contains >
      "user?name@example.com",      // Contains ?
      "user,name@example.com",      // Contains ,
      "user;name@example.com",      // Contains ;
      "user:name@example.com",      // Contains :
      "user\"name@example.com",     // Contains "
    ]
  )
  fun `email with special characters should be rejected`(value: String) {
    // When & Then
    val exception = assertThrows<IllegalArgumentException> {
      Email(value)
    }
    assert(exception.message!!.contains("geldig emailadres"))
  }

  @Test
  fun `email with uppercase letters should be accepted`() {
    // When
    val email = Email("User.Name@Example.COM")

    // Then
    assertEquals("User.Name@Example.COM", email.value)
  }

  @Test
  fun `email with plus sign for filtering should be accepted`() {
    // When
    val email = Email("user+newsletter@example.com")

    // Then
    assertEquals("user+newsletter@example.com", email.value)
  }

  @Test
  fun `email with subdomain should be accepted`() {
    // When
    val email = Email("user@mail.example.com")

    // Then
    assertEquals("user@mail.example.com", email.value)
  }

  @Test
  fun `email with hyphenated domain should be accepted`() {
    // When
    val email = Email("user@my-domain.com")

    // Then
    assertEquals("user@my-domain.com", email.value)
  }

  @Test
  fun `email with numeric domain should be accepted`() {
    // When
    val email = Email("user@123domain.com")

    // Then
    assertEquals("user@123domain.com", email.value)
  }

  @Test
  fun `email with long TLD should be accepted`() {
    // When
    val email = Email("user@example.international")

    // Then
    assertEquals("user@example.international", email.value)
  }

  @Test
  fun `email with minimum valid TLD length should be accepted`() {
    // When
    val email = Email("user@example.co")

    // Then
    assertEquals("user@example.co", email.value)
  }
}
