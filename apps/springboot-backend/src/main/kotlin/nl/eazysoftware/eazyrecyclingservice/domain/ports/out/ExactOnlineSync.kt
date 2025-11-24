package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company

/**
 * Outgoing port for synchronizing companies with Exact Online
 */
interface ExactOnlineSync {
    /**
     * Sync a company to Exact Online.
     * This should be a fire-and-forget operation that handles failures gracefully
     * without affecting the main company creation flow.
     */
    fun syncCompany(company: Company)
}
