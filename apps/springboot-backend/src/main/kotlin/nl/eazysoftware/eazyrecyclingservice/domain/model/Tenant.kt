package nl.eazysoftware.eazyrecyclingservice.domain.model

import nl.eazysoftware.eazyrecyclingservice.config.TenantProperties
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceType
import org.springframework.stereotype.Component

@Component
class Tenant(private val tenantProperties: TenantProperties) {
  val processorPartyId = ProcessorPartyId("08797")
  val companyName = "WHD Kabel- en Metaalrecycling B.V."

  fun getFinancialEmailForInvoiceType(invoiceType: InvoiceType): String {
    return when (invoiceType) {
      InvoiceType.PURCHASE -> tenantProperties.purchaseFinancialEmail
      InvoiceType.SALE -> tenantProperties.salesFinancialEmail
    }
  }
}
