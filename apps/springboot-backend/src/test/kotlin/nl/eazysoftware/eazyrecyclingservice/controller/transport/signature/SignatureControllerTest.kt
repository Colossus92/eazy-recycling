package nl.eazysoftware.eazyrecyclingservice.controller.transport.signature


import nl.eazysoftware.eazyrecyclingservice.domain.service.CreateSignatureRequest
import nl.eazysoftware.eazyrecyclingservice.domain.service.WaybillDocumentService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class SignatureControllerTest {

  @Mock
  private lateinit var waybillDocumentService: WaybillDocumentService

  private var signatureController: SignatureController? = null

  @BeforeEach
  fun setup() {
    signatureController = SignatureController(waybillDocumentService)
  }

  @Test
  fun `getSignatureStatuses should return signature statuses`() {
    val id = UUID.randomUUID()
    whenever(waybillDocumentService.getSignatureStatuses(id)).thenReturn(mock())

    signatureController?.getSignatureStatuses(id)

    verify(waybillDocumentService).getSignatureStatuses(id)
  }

  @Test
  fun `saveSignature should save signature`() {
    val id = UUID.randomUUID()
    val request = CreateSignatureRequest(
      signature = "signature",
      party = "carrier",
      email = "email"
    )

    signatureController?.saveSignature(id, request)

    verify(waybillDocumentService).saveSignature(id, request)
  }

}
