package nl.eazysoftware.eazyrecyclingservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "tenant")
data class TenantProperties(
    var purchaseFinancialEmail: String = "hello+purchase@eazysoftware.nl",
    var salesFinancialEmail: String = "hello+sales@eazysoftware.nl"
)
