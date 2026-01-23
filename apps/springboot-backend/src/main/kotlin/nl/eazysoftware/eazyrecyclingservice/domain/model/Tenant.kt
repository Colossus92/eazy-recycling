package nl.eazysoftware.eazyrecyclingservice.domain.model

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.invoice.InvoiceType

object Tenant {
  val processorPartyId = ProcessorPartyId("08797")
  val companyName = "WHD Metaalrecycling"
  val purchaseFinancialEmail = "hello+purchase@eazysoftware.nl"
  val salesFinancialEmail = "hello+sales@eazysoftware.nl"

  fun getFinancialEmailForInvoiceType(invoiceType: InvoiceType): String {
    return when (invoiceType) {
      InvoiceType.PURCHASE -> purchaseFinancialEmail
      InvoiceType.SALE -> salesFinancialEmail
    }
  }
}
