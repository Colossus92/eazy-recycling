package nl.eazysoftware.eazyrecyclingservice.controller.invoice

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllInvoices
import nl.eazysoftware.eazyrecyclingservice.application.query.GetInvoiceById
import nl.eazysoftware.eazyrecyclingservice.application.query.InvoiceDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.InvoiceView
import nl.eazysoftware.eazyrecyclingservice.application.usecase.invoice.*
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ROLE_ADMIN
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceDocumentType
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceId
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
    private val createCompletedInvoice: CreateCompletedInvoice,
    private val createCreditInvoice: CreateCreditInvoice,
    private val updateInvoice: UpdateInvoice,
    private val finalizeInvoice: FinalizeInvoice,
    private val deleteInvoice: DeleteInvoice,
    private val sendInvoice: SendInvoice,
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
                    id = line.id,
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
    fun getById(@PathVariable id: UUID): ResponseEntity<InvoiceDetailView> {
        val invoice = getInvoiceById.handle(id)
        return if (invoice != null) {
            ResponseEntity.ok(invoice)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateInvoiceRequest
    ): InvoiceResult {
        val command = UpdateInvoiceCommand(
            invoiceId = id,
            customerId = UUID.fromString(request.customerId),
            invoiceType = InvoiceType.valueOf(request.invoiceType),
            invoiceDate = request.invoiceDate,
            lines = request.lines.map { line ->
                InvoiceLineCommand(
                    id = line.id,
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

    @PostMapping("/completed")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCompleted(@Valid @RequestBody request: CreateInvoiceRequest): InvoiceResult {
        val command = CreateInvoiceCommand(
            invoiceType = InvoiceType.valueOf(request.invoiceType),
            documentType = InvoiceDocumentType.valueOf(request.documentType),
            customerId = UUID.fromString(request.customerId),
            invoiceDate = request.invoiceDate,
            originalInvoiceId = request.originalInvoiceId,
            lines = request.lines.map { line ->
                InvoiceLineCommand(
                    id = line.id,
                    date = line.date,
                    catalogItemId = line.catalogItemId,
                    description = line.description,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    orderReference = line.orderReference,
                )
            },
        )
        return createCompletedInvoice.handle(command)
    }

    @PostMapping("/{id}/finalize")
    @ResponseStatus(HttpStatus.OK)
    fun finalize(@PathVariable id: UUID): InvoiceResult {
        return finalizeInvoice.handle(FinalizeInvoiceCommand(id))
    }

    @PostMapping("/{id}/send")
    @ResponseStatus(HttpStatus.OK)
    fun send(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SendInvoiceRequest
    ): InvoiceResult {
        val command = SendInvoiceCommand(
            invoiceId = InvoiceId(id),
            to = request.to,
            bcc = request.bcc,
            subject = request.subject,
            body = request.body,
        )
        return sendInvoice.handle(command)
    }

    @PostMapping("/{id}/credit")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCredit(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateCreditInvoiceRequest
    ): InvoiceResult {
        val command = CreateCreditInvoiceCommand(
            originalInvoiceId = id,
            invoiceDate = request.invoiceDate,
        )
        return createCreditInvoice.handle(command)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) {
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

        val originalInvoiceId: UUID?,

        @field:Valid
        val lines: List<InvoiceLineRequest>,
    )

    data class UpdateInvoiceRequest(
        @field:NotBlank
        val customerId: String,

        @field:NotBlank
        val invoiceType: String,

        val invoiceDate: LocalDate,

        @field:Valid
        val lines: List<InvoiceLineRequest>,
    )

    data class InvoiceLineRequest(
        val id: UUID? = null,
        val date: LocalDate,
        val catalogItemId: UUID,

        val description: String? = null,

        val quantity: BigDecimal,

        val unitPrice: BigDecimal,

        val orderReference: String?,
    )

    data class SendInvoiceRequest(
        @field:NotBlank
        val to: String,

        val bcc: String?,

        @field:NotBlank
        val subject: String,

        @field:NotBlank
        val body: String,
    )

    data class CreateCreditInvoiceRequest(
        val invoiceDate: LocalDate,
    )
}
