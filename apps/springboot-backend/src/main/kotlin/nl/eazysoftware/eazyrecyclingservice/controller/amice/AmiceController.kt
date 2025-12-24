package nl.eazysoftware.eazyrecyclingservice.controller.amice

import kotlinx.datetime.number
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.ApproveCorrectiveDeclaration
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclaration
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarations
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Controller for testing Amice integration endpoints.
 * This is primarily for manual testing and debugging of Amice SOAP service calls.
 */
@RestController
@RequestMapping("/amice")
@PreAuthorize(HAS_ADMIN_OR_PLANNER)
class AmiceController(
  private val lmaDeclarations: LmaDeclarations,
  private val approveCorrectiveDeclaration: ApproveCorrectiveDeclaration,
) {

  @GetMapping
  fun getDeclarations(pageable: Pageable): Page<LmaDeclarationView> {
    return lmaDeclarations.findAll(pageable)
      .map { LmaDeclarationView.fromDomain(it) }
  }

  data class LmaDeclarationView(
    val id: String,
    val wasteStreamNumber: String,
    val period: String,
    val totalWeight: Int,
    val totalShipments: Int,
    val status: String,
    val wasteName: String,
    val pickupLocation: String,
    val errors: List<String>?,
    val transporters: List<String>,
  ) {
    companion object {
      fun fromDomain(lmaDeclaration: LmaDeclaration): LmaDeclarationView {
        return LmaDeclarationView(
          id = lmaDeclaration.id,
          wasteStreamNumber = lmaDeclaration.wasteStreamNumber.number,
          period = "${lmaDeclaration.period.month.number.toString().padStart(2, '0')}-${lmaDeclaration.period.year}",
          totalWeight = lmaDeclaration.totalWeight,
          totalShipments = lmaDeclaration.totalTransports,
          status = lmaDeclaration.status,
          wasteName = lmaDeclaration.wasteName,
          pickupLocation = lmaDeclaration.pickupLocation.toAddressLine(),
          errors = lmaDeclaration.errors?.toList(),
          transporters = lmaDeclaration.transporters,
        )
      }
    }
  }

  /**
   * Approves and submits a corrective declaration to LMA.
   *
   * Corrective declarations are created with status CORRECTIVE and require manual approval
   * before being submitted. This endpoint handles the approval process.
   */
  @PostMapping("/declarations/{declarationId}/approve")
  @ResponseStatus(HttpStatus.OK)
  fun approveDeclaration(
    @PathVariable declarationId: String
  ): ApproveDeclarationResponse {
    val result = approveCorrectiveDeclaration.approve(declarationId)

    return ApproveDeclarationResponse(
      success = result.success,
      message = result.message,
      declarationId = result.declarationId,
    )
  }

  data class ApproveDeclarationResponse(
    val success: Boolean,
    val message: String,
    val declarationId: String,
  )
}
