package nl.eazysoftware.eazyrecyclingservice.controller.exception

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.application.usecase.company.SoftDeletedCompanyConflictException
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.IncompatibleWasteStreamsException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@ExtendWith(SpringExtension::class)
class GlobalExceptionHandlerTest {

  private lateinit var handler: GlobalExceptionHandler

  @BeforeEach
  fun setup() {
    handler = GlobalExceptionHandler()
  }

  @Test
  fun `handleDuplicateKeyException should return CONFLICT with custom message`() {
    val exception = DuplicateKeyException("Duplicate key error")

    val response = handler.handleDuplicateKeyException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Duplicate key error")
  }

  @Test
  fun `handleDuplicateKeyException should return CONFLICT with default message when exception message is null`() {
    val exception = mock<DuplicateKeyException>()
    whenever(exception.message).thenReturn(null)

    val response = handler.handleDuplicateKeyException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Object met id bestaat al")
  }

  @Test
  fun `handleEntityNotFoundException should return NOT_FOUND with custom message`() {
    val exception = EntityNotFoundException("Entity not found")

    val response = handler.handleEntityNotFoundException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    assertThat(response.body?.message).isEqualTo("Entity not found")
  }

  @Test
  fun `handleEntityNotFoundException should return NOT_FOUND with default message when exception message is null`() {
    val exception = mock<EntityNotFoundException>()
    whenever(exception.message).thenReturn(null)

    val response = handler.handleEntityNotFoundException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    assertThat(response.body?.message).isEqualTo("Object niet gevonden")
  }

  @Test
  fun `handleIllegalArgumentException should return BAD_REQUEST with custom message`() {
    val exception = IllegalArgumentException("Invalid argument")

    val response = handler.handleDuplicateKeyException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body?.message).isEqualTo("Invalid argument")
  }

  @Test
  fun `handleIllegalArgumentException should return BAD_REQUEST with default message when exception message is null`() {
    val exception = mock<IllegalArgumentException>()
    whenever(exception.message).thenReturn(null)

    val response = handler.handleDuplicateKeyException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body?.message).isEqualTo("Ongeldige invoer")
  }

  // Note: MethodArgumentNotValidException tests are covered by integration tests
  // Unit testing this handler is complex due to the exception's internal getMessage()
  // implementation requiring a fully constructed MethodParameter with executable.
  // The handler logic for extracting field and global errors is straightforward
  // and is better tested through integration tests with actual validation failures.

  @Test
  fun `handleIllegalStateException should return CONFLICT with custom message`() {
    val exception = IllegalStateException("Invalid state")

    val response = handler.handleIllegalStateException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Invalid state")
  }

  @Test
  fun `handleIllegalStateException should return CONFLICT with default message when exception message is null`() {
    val exception = mock<IllegalStateException>()
    whenever(exception.message).thenReturn(null)

    val response = handler.handleIllegalStateException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Deze actie brengt de data in een ongeldige staat en is niet toegestaan")
  }

  @Test
  fun `handleSoftDeletedCompanyConflictException should return CONFLICT with company details`() {
    val companyId = CompanyId(UUID.randomUUID())
    val exception = SoftDeletedCompanyConflictException(
      companyId,
      "email",
      "test@example.com",
      "Company conflict"
    )

    val response = handler.handleSoftDeletedCompanyConflictException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Company conflict")
    assertThat(response.body?.deletedCompanyId).isEqualTo(companyId.uuid.toString())
    assertThat(response.body?.conflictField).isEqualTo("email")
    assertThat(response.body?.conflictValue).isEqualTo("test@example.com")
  }

  @Test
  fun `handleIncompatibleWasteStreamsException should return CONFLICT with custom message`() {
    val exception = IncompatibleWasteStreamsException("Incompatible waste streams")

    val response = handler.handleIncompatibleWasteStreamsException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Incompatible waste streams")
  }

  @Test
  fun `handleIncompatibleWasteStreamsException should return CONFLICT with default message when exception message is null`() {
    val exception = mock<IncompatibleWasteStreamsException>()
    whenever(exception.message).thenReturn(null)

    val response = handler.handleIncompatibleWasteStreamsException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Deze actie brengt de data in een ongeldige staat en is niet toegestaan")
  }

  @Test
  fun `handleDataIntegrityViolationException should return CONFLICT with duplicate key message for code constraint`() {
    val rootCause = RuntimeException("duplicate key value violates unique constraint \"unique_code\"")
    val exception = mock<DataIntegrityViolationException>()
    whenever(exception.message).thenReturn("could not execute statement")
    whenever(exception.rootCause).thenReturn(rootCause)

    val response = handler.handleDataIntegrityViolationException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Een item met deze code bestaat al")
  }

  @Test
  fun `handleDataIntegrityViolationException should return CONFLICT with duplicate key message for name constraint`() {
    val rootCause = RuntimeException("duplicate key value violates unique constraint \"unique_name\"")
    val exception = mock<DataIntegrityViolationException>()
    whenever(exception.message).thenReturn("could not execute statement")
    whenever(exception.rootCause).thenReturn(rootCause)

    val response = handler.handleDataIntegrityViolationException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Een item met deze naam bestaat al")
  }

  @Test
  fun `handleDataIntegrityViolationException should return CONFLICT with generic duplicate key message`() {
    val rootCause = RuntimeException("duplicate key value violates unique constraint \"some_other_constraint\"")
    val exception = mock<DataIntegrityViolationException>()
    whenever(exception.message).thenReturn("could not execute statement")
    whenever(exception.rootCause).thenReturn(rootCause)

    val response = handler.handleDataIntegrityViolationException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Dit item bestaat al in de database")
  }

  @Test
  fun `handleDataIntegrityViolationException should return CONFLICT with duplicate key message when in main message`() {
    val exception = mock<DataIntegrityViolationException>()
    whenever(exception.message).thenReturn("duplicate key error occurred")
    whenever(exception.rootCause).thenReturn(null)

    val response = handler.handleDataIntegrityViolationException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Dit item bestaat al in de database")
  }

  @Test
  fun `handleDataIntegrityViolationException should return CONFLICT with foreign key message`() {
    val rootCause = RuntimeException("foreign key constraint violation")
    val exception = mock<DataIntegrityViolationException>()
    whenever(exception.message).thenReturn("could not execute statement")
    whenever(exception.rootCause).thenReturn(rootCause)

    val response = handler.handleDataIntegrityViolationException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Deze actie kan niet worden uitgevoerd omdat er gerelateerde gegevens bestaan")
  }

  @Test
  fun `handleDataIntegrityViolationException should return CONFLICT with foreign key message when in main message`() {
    val exception = mock<DataIntegrityViolationException>()
    whenever(exception.message).thenReturn("foreign key violation occurred")
    whenever(exception.rootCause).thenReturn(null)

    val response = handler.handleDataIntegrityViolationException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Deze actie kan niet worden uitgevoerd omdat er gerelateerde gegevens bestaan")
  }

  @Test
  fun `handleDataIntegrityViolationException should return CONFLICT with generic message for other violations`() {
    val exception = mock<DataIntegrityViolationException>()
    whenever(exception.message).thenReturn("some other integrity violation")
    whenever(exception.rootCause).thenReturn(null)

    val response = handler.handleDataIntegrityViolationException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Er is een probleem met de gegevensintegriteit")
  }

  @Test
  fun `handleDataIntegrityViolationException should handle null message and null rootCause`() {
    val exception = mock<DataIntegrityViolationException>()
    whenever(exception.message).thenReturn(null)
    whenever(exception.rootCause).thenReturn(null)

    val response = handler.handleDataIntegrityViolationException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.message).isEqualTo("Er is een probleem met de gegevensintegriteit")
  }
}
