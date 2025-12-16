package nl.eazysoftware.eazyrecyclingservice.controller.invoice

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllInvoices
import nl.eazysoftware.eazyrecyclingservice.application.query.GetInvoiceById
import nl.eazysoftware.eazyrecyclingservice.application.query.InvoiceDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.InvoiceView
import nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice.*
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ROLE_ADMIN
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceDocumentType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceType
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@RestController
@PreAuthorize(HAS_ROLE_ADMIN)
@RequestMapping("/invoices")
class InvoiceController(
    private val createInvoice: CreateInvoice,
    private val updateInvoice: UpdateInvoice,
    private val deleteInvoice: DeleteInvoice,
    private val getAllInvoices: GetAllInvoices,
    private val getInvoiceById: GetInvoiceById,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateInvoiceRequest): InvoiceResult {
        val command = CreateInvoiceCommand(
            invoiceType = InvoiceType.valueOf(request.invoiceType),
            documentType = InvoiceDocumentType.valueOf(request.documentType),
            customerId = UUID.fromString(request.customerId),
            invoiceDate = request.invoiceDate,
            originalInvoiceId = request.originalInvoiceId,
            lines = request.lines.map { line ->
                InvoiceLineCommand(
                    date = line.date,
                    catalogItemId = line.catalogItemId,
                    description = line.description,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    orderReference = line.orderReference,
                )
            },
        )
        return createInvoice.handle(command)
    }

    @GetMapping
    fun getAll(): List<InvoiceView> {
        return getAllInvoices.handle()
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<InvoiceDetailView> {
        val invoice = getInvoiceById.handle(id)
        return if (invoice != null) {
            ResponseEntity.ok(invoice)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateInvoiceRequest
    ): InvoiceResult {
        val command = UpdateInvoiceCommand(
            invoiceId = id,
            invoiceDate = request.invoiceDate,
            lines = request.lines.map { line ->
                InvoiceLineCommand(
                    date = line.date,
                    catalogItemId = line.catalogItemId,
                    description = line.description,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    orderReference = line.orderReference,
                )
            },
        )
        return updateInvoice.handle(command)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        deleteInvoice.handle(id)
    }

    data class CreateInvoiceRequest(
        @field:NotBlank
        val invoiceType: String,

        @field:NotBlank
        val documentType: String,

        @field:NotBlank
        val customerId: String,

        val invoiceDate: LocalDate,

        val originalInvoiceId: Long?,

        @field:Valid
        val lines: List<InvoiceLineRequest>,
    )

    data class UpdateInvoiceRequest(
        val invoiceDate: LocalDate,

        @field:Valid
        val lines: List<InvoiceLineRequest>,
    )

    data class InvoiceLineRequest(
        val date: LocalDate,

        @field:NotNull
        @field:Positive
        val catalogItemId: Long,

        val description: String?,

        @field:NotNull
        val quantity: BigDecimal,

        @field:NotNull
        val unitPrice: BigDecimal,

        val orderReference: String?,
    )
}
